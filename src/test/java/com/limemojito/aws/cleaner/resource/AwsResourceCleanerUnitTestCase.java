/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2015
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.limemojito.aws.cleaner.resource;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.AmazonS3Exception;

public abstract class AwsResourceCleanerUnitTestCase {
    protected AmazonServiceException createThrottleException() {
        final AmazonServiceException testThrottle = new AmazonServiceException("TestThrottle");
        testThrottle.setErrorCode("Throttling");
        return testThrottle;
    }

    protected AmazonS3Exception createsS3NotEmptyException() {
        final AmazonS3Exception notEmpty = new AmazonS3Exception("not empty");
        notEmpty.setStatusCode(409);
        notEmpty.setErrorCode("BucketNotEmpty");
        return notEmpty;
    }
}
