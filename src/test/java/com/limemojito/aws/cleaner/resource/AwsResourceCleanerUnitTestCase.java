/*
 * Copyright 2011-2023 Lime Mojito Pty Ltd
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
