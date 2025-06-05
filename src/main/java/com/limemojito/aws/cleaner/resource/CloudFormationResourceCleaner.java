/*
 * Copyright 2011-2025 Lime Mojito Pty Ltd
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.limemojito.aws.cleaner.resource;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.*;
import com.amazonaws.services.cloudformation.model.Stack;
import com.limemojito.aws.cleaner.ResourceCleaner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.amazonaws.services.cloudformation.model.StackStatus.DELETE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.DELETE_FAILED;
import static com.limemojito.aws.cleaner.resource.Throttle.performRequestWithThrottle;
import static com.limemojito.aws.cleaner.resource.WaitFor.waitFor;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.split;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

/**
 * Resource cleaner for AWS CloudFormation stacks.
 * This cleaner is executed with the highest precedence to ensure CloudFormation stacks
 * are deleted before other resources. It handles dependencies between stacks by
 * analyzing exports and imports, and deletes stacks in the correct order.
 */
@Order(HIGHEST_PRECEDENCE)
@Service
@Slf4j
public class CloudFormationResourceCleaner implements ResourceCleaner {
    private final AmazonCloudFormation client;
    private final Collection<String> permanentStacks;
    private final int maxDeleteWaitSeconds;
    private boolean commit;

    /**
     * Constructs a new CloudFormationResourceCleaner.
     *
     * @param client       The AWS CloudFormation client
     * @param whitelistCsv Comma-separated list of stack name prefixes to preserve
     * @param seconds      Maximum time in seconds to wait for stack deletion to complete
     */
    @Autowired
    public CloudFormationResourceCleaner(AmazonCloudFormation client,
                                         @Value("${cleaner.cloudformation.whitelist}") String whitelistCsv,
                                         @Value("${cleaner.cloudformation.wait.delete.seconds}") int seconds) {
        this.client = client;
        this.permanentStacks = Arrays.stream(split(whitelistCsv, ','))
                                     .map(StringUtils::trimToEmpty)
                                     .collect(toList());
        this.maxDeleteWaitSeconds = seconds;
        if (!permanentStacks.isEmpty()) {
            log.info("Ignoring stacks with prefix {}", this.permanentStacks);
        }
    }

    /**
     * {@inheritDoc}
     * Sets whether the cleaner should actually delete CloudFormation stacks or just simulate the deletions.
     */
    @Override
    public void setCommit(boolean commit) {
        this.commit = commit;
    }

    /**
     * {@inheritDoc}
     * This implementation ignores the filter as CloudFormation stacks are managed differently.
     * The decision to delete stacks is based on stack names and export dependencies.
     */
    @Override
    public void setFilter(PhysicalDeletionFilter filter) {
        log.debug("Ignoring filter {}", filter.getClass());
    }

    /**
     * {@inheritDoc}
     * Cleans AWS CloudFormation stacks by:
     * 1. First identifying and deleting stacks without exports
     * 2. Then deleting remaining stacks in dependency order (stacks with unused exports first)
     *
     * This approach respects the dependencies between stacks and ensures that stacks
     * are deleted in the correct order to avoid dependency conflicts.
     */
    @Override
    public void clean() {
        final Map<String, List<String>> stackToExport = retrieveCloudformationExportMap();
        final List<StackSummary> killList = retrieveStacksToDie();

        log.info("Detecting stacks without exports");
        final List<StackSummary> noExportStacks = killList.stream()
                                                          .filter(ss -> !stackToExport.containsKey(ss.getStackId()))
                                                          .collect(toList());
        log.debug("Found {} stacks without exports", noExportStacks.size());

        log.info("Deleting stacks with no exports");
        deleteAndWait(noExportStacks);
        killList.removeAll(noExportStacks);

        log.info("Deleting stacks in export dependency order ");
        if (commit) {
            iterateRemovingStacksWithUnusedExports(stackToExport, killList);
        } else {
            log.info("Would delete {} (in appropriate order)",
                     killList.stream().map(StackSummary::getStackName).collect(toList()));
        }
    }

    private void iterateRemovingStacksWithUnusedExports(Map<String, List<String>> stackToExport,
                                                        List<StackSummary> killList) {
        do {
            // relies on a max build time.
            removeStacksWithExportsNotInUse(stackToExport, killList);
        } while (!killList.isEmpty());
    }

    private void removeStacksWithExportsNotInUse(Map<String, List<String>> stackToExport, List<StackSummary> killList) {
        for (Iterator<StackSummary> iterator = killList.iterator(); iterator.hasNext(); ) {
            final StackSummary stack = iterator.next();
            final List<String> exports = stackToExport.get(stack.getStackId());
            boolean canKill = true;
            for (int i = 0; canKill && i < exports.size(); i++) {
                canKill = checkExportUnused(exports.get(i));
            }
            if (canKill) {
                deleteAndWait(stack);
                iterator.remove();
            }
        }
        log.debug("There are {} stacks remaining", killList.size());
    }

    private boolean checkExportUnused(String export) {
        final ListImportsResult response = performRequestWithThrottle(() -> listImports(export));
        return response.getImports().isEmpty();
    }

    private ListImportsResult listImports(String export) {
        try {
            return client.listImports(new ListImportsRequest().withExportName(export));
        } catch (AmazonCloudFormationException e) {
            if (e.getMessage().contains("is not imported")) {
                return new ListImportsResult().withImports();
            } else {
                throw e;
            }
        }
    }

    private List<StackSummary> retrieveStacksToDie() {
        log.info("Requesting stacks");
        final ListStacksRequest request = new ListStacksRequest();
        final List<StackSummary> stacks = performRequestWithThrottle(() -> client.listStacks(request)
                                                                                 .getStackSummaries());
        log.debug("{} stacks found", stacks.size());
        final List<StackSummary> killList = stacks.stream()
                                                  .filter(this::isKillStack)
                                                  .collect(toList());
        log.info("Detected {} stacks to destroy", killList.size());
        return killList;
    }

    private Map<String, List<String>> retrieveCloudformationExportMap() {
        log.info("Retrieving stacks with exports");
        final Map<String, List<String>> stackToExport = new HashMap<>();
        String nextToken = null;
        do {
            log.trace("Retrieving CloudFormation exports");
            final ListExportsRequest listExportsRequest = new ListExportsRequest().withNextToken(nextToken);
            final ListExportsResult listExportsResult = performRequestWithThrottle(() -> client.listExports(
                    listExportsRequest));
            final List<Export> cfmExports = listExportsResult.getExports();
            log.debug("Retrieved {} exports", cfmExports.size());
            for (Export cfmExport : cfmExports) {
                final List<String> exports = stackToExport.computeIfAbsent(cfmExport.getExportingStackId(),
                                                                           (key) -> new ArrayList<>());
                exports.add(cfmExport.getName());
                stackToExport.put(cfmExport.getExportingStackId(), exports);
            }
            nextToken = listExportsResult.getNextToken();
        } while (nextToken != null);
        log.debug("Retrieved {} stacks with exports", stackToExport.size());
        return stackToExport;
    }

    private boolean isKillStack(StackSummary summary) {
        final StackStatus stackStatus = StackStatus.valueOf(summary.getStackStatus());
        final boolean statusOkToRemove = canBeRemoved(stackStatus);
        if (statusOkToRemove) {
            final String stackName = summary.getStackName();
            final boolean killStack = (!isPermStackName(stackName));
            if (!killStack) {
                log.info("Preserving stack named {}", stackName);
            }
            return killStack;
        } else {
            if (stackStatus != DELETE_COMPLETE) {
                throw new AmazonCloudFormationException(format("%s can not be deleted due to status %s",
                                                               summary.getStackName(),
                                                               stackStatus));
            }
        }
        return false;
    }

    private boolean isPermStackName(String stackName) {
        for (String permStackPrefix : permanentStacks) {
            if (stackName.startsWith(permStackPrefix)) {
                return true;
            }
        }
        return false;
    }

    private void deleteAndWait(List<StackSummary> stacks) {
        // send all deletes then wait for all complete.
        stacks.forEach(this::deleteAndContinue);
        if (commit) {
            stacks.forEach(this::waitForDeleteComplete);
        }
    }

    private void deleteAndWait(StackSummary stack) {
        deleteAndContinue(stack);
        if (commit) {
            waitForDeleteComplete(stack);
        }
    }

    private void waitForDeleteComplete(StackSummary stack) {
        waitFor(maxDeleteWaitSeconds, () -> isStackDeleteCompleted(stack.getStackName()));
    }

    private void deleteAndContinue(StackSummary stack) {
        if (commit) {
            try {
                Throttle.performWithThrottle(() -> deleteStack(stack));
            } catch (AmazonCloudFormationException e) {
                log.warn("Could not delete stack {}. {}", stack.getStackName(), e.getErrorMessage());
            }
        } else {
            log.info("Would delete stack {}", stack.getStackName());
        }
    }

    private boolean canBeRemoved(StackStatus status) {
        return switch (status) {
            case CREATE_COMPLETE, CREATE_IN_PROGRESS, CREATE_FAILED, DELETE_FAILED, REVIEW_IN_PROGRESS, ROLLBACK_FAILED,
                 UPDATE_COMPLETE, UPDATE_COMPLETE_CLEANUP_IN_PROGRESS, UPDATE_IN_PROGRESS, UPDATE_ROLLBACK_COMPLETE,
                 UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS, UPDATE_ROLLBACK_FAILED, UPDATE_ROLLBACK_IN_PROGRESS ->
                    true;
            default -> false;
        };
    }

    private void deleteStack(StackSummary stack) {
        final String stackName = stack.getStackName();
        log.info("Deleting stack {} with current status {}", stackName, stack.getStackStatus());
        performDelete(stackName);
    }

    private boolean isStackDeleteCompleted(String stackName) {
        log.debug("Checking stack {} for delete completed", stackName);
        final Optional<StackStatus> status = requestStackStatus(stackName);
        if (status.isPresent() && status.get() == DELETE_FAILED) {
            log.warn("Delete failure detected on {} attempting retry", stackName);
            performDelete(stackName);
        }
        final boolean deleted = status.isEmpty() || DELETE_COMPLETE == status.get();
        if (deleted) {
            log.info("Stack {} is deleted", stackName);
        }
        return deleted;
    }

    private Optional<StackStatus> requestStackStatus(String stackName) {
        try {
            final DescribeStacksRequest request = new DescribeStacksRequest().withStackName(stackName);
            final DescribeStacksResult describeStacksResult = performRequestWithThrottle(() -> client.describeStacks(
                    request));
            final List<Stack> stacks = describeStacksResult.getStacks();
            return stacks.isEmpty()
                   ? Optional.empty()
                   : Optional.of(StackStatus.fromValue(stacks.getFirst().getStackStatus()));
        } catch (AmazonCloudFormationException e) {
            log.debug("stack not existing?", e);
            return Optional.empty();
        }
    }

    private void performDelete(String stackName) {
        try {
            client.deleteStack(new DeleteStackRequest().withStackName(stackName));
        } catch (AmazonCloudFormationException e) {
            if (!e.getMessage().contains("not exist")) {
                throw e;
            }
        }
        log.debug("Deleted stack");
    }
}
