/*
 * Copyright 2020 Lime Mojito Pty Ltd
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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class S3ResourceCleaner extends PhysicalResourceCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(S3ResourceCleaner.class);
    private final AmazonS3 client;

    @Autowired
    public S3ResourceCleaner(AmazonS3 client) {
        super();
        this.client = client;
    }

    @Override
    protected List<String> getPhysicalResourceIds() {
        return client.listBuckets()
                     .stream()
                     .map(Bucket::getName)
                     .collect(Collectors.toList());
    }

    @Override
    protected void performDelete(String physicalId) {
        deleteBucket(physicalId);
    }

    private void deleteBucket(String bucketName) {
        LOGGER.info("Deleting bucket {}", bucketName);
        try {
            client.deleteBucket(bucketName);
        } catch (AmazonS3Exception e) {
            switch (e.getErrorCode()) {
                case "AccessDenied":
                    LOGGER.warn("Can not delete bucket {} as access denied", bucketName);
                    deleteAll(bucketName);
                    break;
                case "BucketNotEmpty":
                    deleteAll(bucketName);
                    deleteBucket(bucketName);
                    break;
                default:
                    throw e;
            }
        }
    }

    private void deleteAll(String bucketName) {
        final ObjectListing objectListing = client.listObjects(bucketName);
        final List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
        if (objectSummaries.size() > 0) {
            LOGGER.info("Deleting all content in {}", bucketName);
            LOGGER.debug("Creating delete objects request for {} objects", objectSummaries.size());
            DeleteObjectsRequest request = createDeleteFilesRequest(objectListing);
            client.deleteObjects(request);
            LOGGER.debug("Delete objects request complete");
        }
    }

    private DeleteObjectsRequest createDeleteFilesRequest(ObjectListing expectedFileList) {
        final DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(expectedFileList.getBucketName());
        final List<S3ObjectSummary> objectSummaries = expectedFileList.getObjectSummaries();
        final List<DeleteObjectsRequest.KeyVersion> keys = new ArrayList<>(objectSummaries.size());
        keys.addAll(objectSummaries.stream()
                                   .map(objectSummary -> new DeleteObjectsRequest.KeyVersion(objectSummary.getKey()))
                                   .collect(Collectors.toList()));
        deleteObjectsRequest.setKeys(keys);
        return deleteObjectsRequest;
    }
}
