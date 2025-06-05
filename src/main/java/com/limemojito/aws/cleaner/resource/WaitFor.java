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

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for waiting for conditions to become true.
 * This class provides methods to wait for a specified condition to become true,
 * with configurable timeout and polling intervals.
 */
@Slf4j
public class WaitFor {

    private static final int DEFAULT_POLLING_DELAY = 5000;

    /**
     * Waits for a specified condition to become true, with a custom polling delay.
     *
     * @param maxWaitSeconds Maximum time to wait in seconds
     * @param pollingDelayMs Delay between polling attempts in milliseconds
     * @param t The condition to check
     * @return true if the condition became true within the timeout period, false otherwise
     */
    public static boolean waitFor(int maxWaitSeconds, long pollingDelayMs, SituationToBecomeTrue t) {
        boolean timeout = waitForSituationOrTimeout(maxWaitSeconds, pollingDelayMs, t);
        if (!timeout) {
            log.warn("Situation did not occur in {} seconds", maxWaitSeconds);
        }
        return timeout;
    }

    /**
     * Waits for a specified condition to become true, using the default polling delay.
     *
     * @param maxWaitSeconds Maximum time to wait in seconds
     * @param t The condition to check
     * @return true if the condition became true within the timeout period, false otherwise
     */
    public static boolean waitFor(int maxWaitSeconds, SituationToBecomeTrue t) {
        return waitFor(maxWaitSeconds, DEFAULT_POLLING_DELAY, t);
    }

    /**
     * Functional interface representing a condition that can be checked repeatedly.
     * Implementations define a condition that the WaitFor utility will check until it becomes true
     * or times out.
     */
    @FunctionalInterface
    public interface SituationToBecomeTrue {
        /**
         * Evaluates whether the desired condition is true.
         *
         * @return true if the condition is satisfied, false otherwise
         * @throws Exception if an error occurs while evaluating the condition
         */
        boolean situation() throws Exception;
    }

    private static boolean waitForSituationOrTimeout(int maxWaitSeconds, long pollingDelayMs, SituationToBecomeTrue t) {
        final long endTime = System.currentTimeMillis() + (1_000L * maxWaitSeconds);
        boolean situation = checkSituation(t);
        while (!situation && endTime > System.currentTimeMillis()) {
            situation = pollWait(pollingDelayMs, t);
        }
        return situation;
    }

    private static boolean pollWait(long pollingDelayMs, SituationToBecomeTrue t) {
        try {
            Thread.sleep(pollingDelayMs);
            return checkSituation(t);
        } catch (InterruptedException e) {
            log.warn("Polling rate interrupted");
            return false;
        }
    }

    private static boolean checkSituation(SituationToBecomeTrue t) {
        try {
            return t.situation();
        } catch (Exception e) {
            log.warn("Situation threw an exception: {}", e.getClass().getSimpleName());
            log.debug("Exception trace: ", e);
            return false;
        }
    }
}
