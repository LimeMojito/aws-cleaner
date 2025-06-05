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

import com.amazonaws.AmazonServiceException;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

import static java.lang.String.format;

/**
 * Utility class for handling AWS API throttling.
 * This class provides methods to execute AWS API calls with automatic retry logic
 * when throttling occurs, using an exponential backoff strategy.
 */
@Slf4j
public class Throttle {
    private static final int BACKOFF_SECONDS = 2;
    private static final int MAX_ATTEMPTS = 7;

    /**
     * Executes an AWS action with throttling protection.
     * If the action is throttled, it will be retried with exponential backoff.
     *
     * @param action The AWS action to execute
     */
    static void performWithThrottle(AwsAction action) {
        performWithThrottle(action, 1);
    }

    /**
     * Executes an AWS request with throttling protection and returns the result.
     * If the request is throttled, it will be retried with exponential backoff.
     *
     * @param request The AWS request to execute
     * @param <T> The type of the result returned by the request
     * @return The result of the AWS request
     */
    static <T> T performRequestWithThrottle(AwsRequest request) {
        return performRequestWithThrottle(request, 1);
    }

    private static void performWithThrottle(AwsAction action, int attemptCount) {
        try {
            giveUp(attemptCount);
            action.performAction();
        } catch (AmazonServiceException e) {
            if (isThrottle(e)) {
                waitForAttempt(attemptCount);
                performWithThrottle(action, ++attemptCount);
            } else {
                throw e;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T performRequestWithThrottle(AwsRequest request, int attemptCount) {
        try {
            giveUp(attemptCount);
            return (T) request.performRequest();
        } catch (AmazonServiceException e) {
            if (isThrottle(e)) {
                waitForAttempt(attemptCount);
                return performRequestWithThrottle(request, ++attemptCount);
            } else {
                throw e;
            }
        }
    }

    private static void giveUp(int attemptCount) {
        if (attemptCount > MAX_ATTEMPTS) {
            throw new IllegalStateException(format("Timeout AWS operation after %d attempts", MAX_ATTEMPTS));
        }
    }

    private static boolean isThrottle(AmazonServiceException e) {
        return "Throttling".equals(e.getErrorCode());
    }

    private static void waitForAttempt(int attemptCount) {
        try {
            final int retrySeconds = attemptCount * BACKOFF_SECONDS;
            log.warn("Throttled API calls detected, backoff {} seconds", retrySeconds);
            Thread.sleep(Duration.ofSeconds(retrySeconds).toMillis());
        } catch (InterruptedException e1) {
            log.warn("Interrupted");
        }
    }

    /**
     * Functional interface for AWS requests that return a result.
     * Used with the performRequestWithThrottle method to execute AWS API calls
     * that return a value.
     */
    @FunctionalInterface
    interface AwsRequest {
        /**
         * Executes an AWS request and returns the result.
         *
         * @return The result of the AWS request
         */
        Object performRequest();
    }

    /**
     * Functional interface for AWS actions that don't return a result.
     * Used with the performWithThrottle method to execute AWS API calls
     * that don't return a value.
     */
    @FunctionalInterface
    interface AwsAction {
        /**
         * Executes an AWS action.
         */
        void performAction();
    }
}
