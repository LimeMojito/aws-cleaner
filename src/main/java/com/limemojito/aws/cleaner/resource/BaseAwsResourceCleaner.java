/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2015
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.limemojito.aws.cleaner.resource;

import com.amazonaws.AmazonServiceException;
import com.limemojito.aws.cleaner.ResourceCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseAwsResourceCleaner implements ResourceCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseAwsResourceCleaner.class);
    private static final int BACKOFF_SECONDS = 2;
    private static final long SECONDS_TO_MILLIS = 1_000L;

    protected void performWithThrottle(AwsAction action) {
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

    protected interface AwsAction {
        void performAction();
    }
}
