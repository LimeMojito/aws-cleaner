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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.HeadBucketRequest;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class S3ResourceCleaner extends PhysicalResourceCleaner {
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
                     .filter(this::checkRegion)
                     .collect(Collectors.toList());
    }

    @Override
    protected void performDelete(String physicalId) {
        deleteBucket(physicalId);
    }

    private boolean checkRegion(String name) {
        final String cleaningRegion = client.getRegionName();
        try {
            Object bucketRegion = Throttle.performRequestWithThrottle(() -> client.headBucket(new HeadBucketRequest(name))
                                                                                  .getBucketRegion());
            log.info("Bucket {} is in region {}, cleaning {}", name, bucketRegion, cleaningRegion);
            return cleaningRegion.equals(bucketRegion);
        } catch (AmazonS3Exception e) {
            log.debug("Can not head bucket {} from region {}", name, cleaningRegion);
            return false;
        }
    }

    private void deleteBucket(String bucketName) {
        log.info("Deleting bucket {}", bucketName);
        try {
            client.deleteBucket(bucketName);
        } catch (AmazonS3Exception e) {
            switch (e.getErrorCode()) {
                case "AccessDenied":
                    log.warn("Can not delete bucket {} as access denied", bucketName);
                    deleteAll(bucketName);
                    break;
                case "BucketNotEmpty":
                    deleteAll(bucketName);
                    deleteBucket(bucketName);
                    break;
                default:
                    log.warn("Received error {} {} {}", e.getErrorCode(), e.getMessage(), e.getAdditionalDetails());
                    throw e;
            }
        }
    }

    private void deleteAll(String bucketName) {
        log.info("Deleting all content in {}", bucketName);
        deleteAllObjects(bucketName);
        deleteAllVersions(bucketName);
    }

    private void deleteAllVersions(String bucketName) {
        VersionListing versionList = client.listVersions(new ListVersionsRequest().withBucketName(bucketName));
        log.info("Deleting {} Versions", versionList.getVersionSummaries().size());
        while (true) {
            for (S3VersionSummary vs : versionList.getVersionSummaries()) {
                log.debug("Deleting Version {}:{}", vs.getKey(), vs.getVersionId());
                client.deleteVersion(bucketName, vs.getKey(), vs.getVersionId());
            }
            if (versionList.isTruncated()) {
                versionList = client.listNextBatchOfVersions(versionList);
            } else {
                break;
            }
        }
    }

    private void deleteAllObjects(String bucketName) {
        ObjectListing objectListing = client.listObjects(bucketName);
        log.info("Deleting {} Objects", objectListing.getObjectSummaries().size());
        while (true) {
            if (!objectListing.getObjectSummaries().isEmpty()) {
                final List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
                log.debug("Creating delete objects request for {} objects", objectSummaries.size());
                DeleteObjectsRequest request = createDeleteFilesRequest(objectListing);
                client.deleteObjects(request);
                log.debug("Delete objects request complete");
            }
            if (objectListing.isTruncated()) {
                objectListing = client.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
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
