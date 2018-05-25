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

import com.amazonaws.AmazonServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Throttle {
    private static final Logger LOGGER = LoggerFactory.getLogger(Throttle.class);
    private static final int BACKOFF_SECONDS = 2;
    private static final long SECONDS_TO_MILLIS = 1_000L;

    static void performWithThrottle(AwsAction action) {
        try {
            action.performAction();
        } catch (AmazonServiceException e) {
            if ("Throttling".equals(e.getErrorCode())) {
                LOGGER.warn("Throttled API calls detected, backoff {} seconds", BACKOFF_SECONDS);
                try {
                    Thread.sleep(BACKOFF_SECONDS * SECONDS_TO_MILLIS);
                } catch (InterruptedException e1) {
                    LOGGER.warn("Interrupted");
                }
                action.performAction();
            } else {
                throw e;
            }
        }
    }

    interface AwsAction {
        void performAction();
    }
}
