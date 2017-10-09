/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2016
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.limemojito.aws.cleaner.resource;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.lang.String.format;

@Service
public class CloudFormationResourceCleaner extends BaseAwsResourceCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudFormationResourceCleaner.class);
    private static final String DELETE_COMPLETE = "DELETE_COMPLETE";
    private static final long STATUS_DELAY = 5_000L;
    private final AmazonCloudFormationClient client;
    private AmazonCloudFormationException deleteError;

    @Autowired
    public CloudFormationResourceCleaner(AmazonCloudFormationClient client) {
        this.client = client;
    }

    @Override
    public String getName() {
        return "Cloud Formation Cleaner";
    }

    @Override
    public void clean(String environment) {
        LOGGER.debug("Requesting stacks");
        final ListStacksResult result = client.listStacks();
        final List<StackSummary> stacks = result.getStackSummaries();
        LOGGER.debug("{} stacks found", stacks.size());
        this.deleteError = null;
        stacks.stream()
              .filter(summary -> {
                  final String stackPrefix = ALL_ENVIRONMENTS.equals(environment) ? "" : environment;
                  final String stackStatus = summary.getStackStatus();
                  final String stackName = summary.getStackName();
                  return (stackName.startsWith(stackPrefix) && canBeRemoved(stackStatus));
              }).forEach(this::deleteAndContinue);
        if (deleteError != null) {
            throw deleteError;
        }
    }

    private void deleteAndContinue(StackSummary stackSummary) {
        try {
            performWithThrottle(() -> deleteStack(stackSummary));
        } catch (AmazonCloudFormationException e) {
            LOGGER.warn("Could not delete stack {}. {}", stackSummary.getStackName(), e.getErrorMessage());
            deleteError = e;
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
