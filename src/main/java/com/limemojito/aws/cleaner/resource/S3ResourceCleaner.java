/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2015
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.limemojito.aws.cleaner.resource;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class S3ResourceCleaner extends BaseAwsResourceCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(S3ResourceCleaner.class);
    private final AmazonS3Client client;

    @Autowired
    public S3ResourceCleaner(AmazonS3Client client) {
        this.client = client;
    }

    @Override
    public String getName() {
        return "S3 Cleaner";
    }

    @Override
    public void clean(String environment) {
        LOGGER.debug("Listing s3 buckets");
        final List<Bucket> buckets = client.listBuckets();
        LOGGER.debug("Found {} buckets", buckets.size());
        final String bucketEnvironment = String.format("-%s-", environment.toLowerCase());
        buckets.stream()
               .filter(bucket -> bucket.getName().contains(bucketEnvironment))
               .forEach(bucket -> performWithThrottle(() -> deleteBucket(bucket)));
    }

    private void deleteBucket(Bucket bucket) {
        LOGGER.debug("Deleting bucket {}", bucket.getName());
        try {
            client.deleteBucket(bucket.getName());
        } catch (AmazonS3Exception e) {
            if (e.getErrorCode().equals("BucketNotEmpty")) {
                deleteAll(bucket.getName());
                deleteBucket(bucket);
            } else {
                throw e;
            }
        }
    }

    private void deleteAll(String bucketName) {
        LOGGER.debug("Deleting all content in {}", bucketName);
        final ObjectListing objectListing = client.listObjects(bucketName);
        LOGGER.debug("Creating delete objects request for {} objects", objectListing.getObjectSummaries().size());
        DeleteObjectsRequest request = createDeleteFilesRequest(objectListing);
        client.deleteObjects(request);
        LOGGER.debug("Delete objects request complete");
    }

    private DeleteObjectsRequest createDeleteFilesRequest(ObjectListing expectedFileList) {
        final DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(expectedFileList.getBucketName());
        final List<S3ObjectSummary> objectSummaries = expectedFileList.getObjectSummaries();
        List<DeleteObjectsRequest.KeyVersion> keys = new ArrayList<>(objectSummaries.size());
        keys.addAll(objectSummaries.stream()
                                   .map(objectSummary -> new DeleteObjectsRequest.KeyVersion(objectSummary.getKey()))
                                   .collect(Collectors.toList()));
        deleteObjectsRequest.setKeys(keys);
        return deleteObjectsRequest;
    }
}
