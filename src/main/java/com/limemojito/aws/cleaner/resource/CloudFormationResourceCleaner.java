/*
 * Copyright 2018 Lime Mojito Pty Ltd
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
import com.limemojito.aws.cleaner.ResourceCleaner;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.amazonaws.services.cloudformation.model.StackStatus.*;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.split;

@Service
public class CloudFormationResourceCleaner implements ResourceCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudFormationResourceCleaner.class);
    private static final String DELETE_COMPLETE = "DELETE_COMPLETE";
    private static final long STATUS_DELAY = 5_000L;
    private final AmazonCloudFormation client;
    private final Collection<String> permanentStacks;
    private AmazonCloudFormationException deleteError;
    private PhysicalDeletionFilter filter;
    private boolean commit;

    @Autowired
    public CloudFormationResourceCleaner(AmazonCloudFormation client,
                                         @Value("${cleaner.cloudformation.whitelist}") String whitelistCsv) {
        this.client = client;
        this.permanentStacks = Arrays.stream(split(whitelistCsv, ','))
                                     .map(StringUtils::trimToEmpty)
                                     .collect(toList());
        if (!permanentStacks.isEmpty()) {
            LOGGER.info("Ignoring stacks with prefix {}", this.permanentStacks);
        }
    }

    @Override
    public void setCommit(boolean commit) {
        this.commit = commit;
    }

    @Override
    public void setFilter(PhysicalDeletionFilter filter) {
        LOGGER.debug("Ignoring filter {}", filter.getClass());
    }

    @Override
    public void clean() {
        LOGGER.debug("Requesting stacks");
        final ListStacksRequest request = new ListStacksRequest().withStackStatusFilters(CREATE_COMPLETE,
                                                                                         CREATE_FAILED,
                                                                                         CREATE_IN_PROGRESS,
                                                                                         ROLLBACK_COMPLETE,
                                                                                         ROLLBACK_FAILED,
                                                                                         UPDATE_COMPLETE,
                                                                                         UPDATE_COMPLETE_CLEANUP_IN_PROGRESS,
                                                                                         UPDATE_IN_PROGRESS,
                                                                                         UPDATE_ROLLBACK_COMPLETE,
                                                                                         UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS,
                                                                                         UPDATE_ROLLBACK_FAILED,
                                                                                         UPDATE_ROLLBACK_IN_PROGRESS,
                                                                                         DELETE_FAILED);
        final List<StackSummary> stacks = client.listStacks(request).getStackSummaries();
        LOGGER.debug("{} stacks found", stacks.size());
        this.deleteError = null;
        stacks.stream()
              .filter(this::isKillStack)
              .forEach(this::deleteAndContinue);
        if (deleteError != null) {
            throw deleteError;
        }
    }

    private boolean isKillStack(StackSummary summary) {
        final String stackStatus = summary.getStackStatus();
        final boolean statusOkToRemove = canBeRemoved(stackStatus);
        if (statusOkToRemove) {
            final String stackName = summary.getStackName();
            final boolean killStack = (!isPermStackName(stackName));
            if (!killStack) {
                LOGGER.info("Preserving stack named " + stackName);
            }
            return killStack;
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

    private void deleteAndContinue(StackSummary stackSummary) {
        if (commit) {
            try {
                Throttle.performWithThrottle(() -> deleteStack(stackSummary));
            } catch (AmazonCloudFormationException e) {
                LOGGER.warn("Could not delete stack {}. {}", stackSummary.getStackName(), e.getErrorMessage());
                deleteError = e;
            }
        } else {
            LOGGER.info("Would delete stack {}", stackSummary.getStackName());
        }
    }

    private boolean canBeRemoved(String stackStatus) {
        switch (stackStatus.toUpperCase()) {
            case "CREATE_COMPLETE":
            case "CREATE_IN_PROGRESS":
            case "CREATE_FAILED":

            case "REVIEW_IN_PROGRESS":

            case "ROLLBACK_FAILED":

            case "UPDATE_COMPLETE":
            case "UPDATE_COMPLETE_CLEANUP_IN_PROGRESS":
            case "UPDATE_IN_PROGRESS":
            case "UPDATE_ROLLBACK_COMPLETE":
            case "UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS":
            case "UPDATE_ROLLBACK_FAILED":
            case "UPDATE_ROLLBACK_IN_PROGRESS":
                return true;

            default:
                return false;
        }
    }

    private void deleteStack(StackSummary stack) {
        final String stackName = stack.getStackName();
        LOGGER.info("Deleting stack {} with current status {}", stackName, stack.getStackStatus());
        try {
            client.deleteStack(new DeleteStackRequest().withStackName(stackName));
            waitForDeletion(stackName);
        } catch (AmazonCloudFormationException e) {
            if (!e.getMessage().contains("not exist")) {
                throw e;
            }
        }
        LOGGER.debug("Deleted stack");
    }

    private void waitForDeletion(String stackName) {
        String stackStatus;
        do {
            waitForStackAction();
            stackStatus = fetchDeleteStatus(stackName);
        } while (stackStatus.equals("DELETE_IN_PROGRESS"));
        if (!DELETE_COMPLETE.equals(stackStatus)) {
            throw new RuntimeException(format("Could not delete stack %s status is %s", stackName, stackStatus));
        }
    }

    private String fetchDeleteStatus(String stackName) {
        String stackStatus;
        final DescribeStacksResult describeStacksResult = client.describeStacks(new DescribeStacksRequest().withStackName(stackName));
        if (!describeStacksResult.getStacks().isEmpty()) {
            stackStatus = describeStacksResult.getStacks().get(0).getStackStatus();
        } else {
            stackStatus = DELETE_COMPLETE;
        }
        return stackStatus;
    }

    private void waitForStackAction() {
        try {
            Thread.sleep(STATUS_DELAY);
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted");
        }
    }
}
