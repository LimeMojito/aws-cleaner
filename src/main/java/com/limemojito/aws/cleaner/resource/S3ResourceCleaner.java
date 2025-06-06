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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Resource cleaner for AWS S3 buckets.
 * This cleaner identifies and deletes S3 buckets in the current AWS region.
 * It handles the deletion of all objects and versions within a bucket before deleting the bucket itself.
 */
@Service
@Slf4j
public class S3ResourceCleaner extends PhysicalResourceCleaner {
    private final S3Client client;
    private final int bucketMax;
    private final Region cleaningRegion;

    /**
     * Constructs a new S3ResourceCleaner.
     *
     * @param client The AWS S3 client
     */
    @Autowired
    public S3ResourceCleaner(S3Client client,
                             @Value("${cleaner.bucket.max}") int maxBuckets,
                             Region cleaningRegion) {
        this.client = client;
        this.bucketMax = maxBuckets;
        this.cleaningRegion = cleaningRegion;
    }

    /**
     * {@inheritDoc}
     * Retrieves a list of all S3 bucket names in the current AWS region.
     * Filters out buckets that are not in the same region as the client.
     *
     * @return A list of S3 bucket names in the current region
     */
    @Override
    protected List<String> getPhysicalResourceIds() {
        log.info("Checking Buckets");
        final List<String> collect = client.listBucketsPaginator(r -> r.maxBuckets(bucketMax))
                                           .stream()
                                           .flatMap(page -> page.buckets().stream())
                                           .map(Bucket::name)
                                           .filter(this::checkRegion)
                                           .collect(Collectors.toList());
        log.info("Found {} Buckets to remove {}", collect.size(), collect);
        return collect;
    }

    /**
     * {@inheritDoc}
     * Deletes an S3 bucket identified by its name.
     * This method handles the complex process of emptying the bucket (deleting all objects
     * and versions) before attempting to delete the bucket itself.
     *
     * @param physicalId The name of the S3 bucket to delete
     */
    @Override
    protected void performDelete(String physicalId) {
        deleteBucket(physicalId);
    }

    private boolean checkRegion(String name) {
        try {
            Object bucketRegion = Throttle.performRequestWithThrottle(() -> client.headBucket(r -> r.bucket(name))
                                                                                  .bucketRegion());
            log.debug("Bucket {} is in region {}, cleaning {}", name, bucketRegion, cleaningRegion);
            return cleaningRegion.toString().equals(bucketRegion.toString());
        } catch (S3Exception e) {
            log.debug("Can not head bucket {} from region {}", name, cleaningRegion);
            return false;
        }
    }

    private void deleteBucket(String bucketName) {
        try {
            log.info("Deleting bucket {}", bucketName);
            client.deleteBucket(r -> r.bucket(bucketName));
        } catch (S3Exception e) {
            switch (e.awsErrorDetails().errorCode()) {
                case "AccessDenied" -> {
                    log.warn("Can not delete bucket {} as access denied", bucketName);
                    deleteAll(bucketName);
                }
                case "BucketNotEmpty" -> {
                    deleteAll(bucketName);
                    deleteBucket(bucketName);
                }
                default -> {
                    log.warn("Received error {} {}", e.awsErrorDetails().errorCode(), e.getMessage());
                    throw e;
                }
            }
        }
    }

    private void deleteAll(String bucketName) {
        log.info("Deleting all content in {}", bucketName);
        deleteAllObjects(bucketName);
        // handle "deleted" versions
        deleteAllVersions(bucketName);
    }

    private void deleteAllVersions(String bucketName) {
        log.info("Deleting Object Versions in bucket {}", bucketName);
        client.listObjectVersionsPaginator(r -> r.bucket(bucketName))
              .stream()
              .forEach(page -> {
                  for (ObjectVersion vs : page.versions()) {
                      log.debug("Deleting Version {}:{}", vs.key(), vs.versionId());
                      client.deleteObject(r -> r.bucket(bucketName)
                                                .key(vs.key())
                                                .versionId(vs.versionId())
                                                .build());
                  }
              });
    }

    private void deleteAllObjects(String bucketName) {
        log.info("Deleting Objects in bucket {}", bucketName);
        client.listObjectsV2Paginator(r -> r.bucket(bucketName))
              .stream()
              .forEach(obj -> {
                  final List<S3Object> objectSummaries = obj.contents();
                  log.debug("Creating delete objects request for {} objects", objectSummaries.size());
                  final List<ObjectIdentifier> deleteList = objectSummaries.stream()
                                                                           .map(o -> ObjectIdentifier.builder()
                                                                                                     .key(o.key())
                                                                                                     .build())
                                                                           .toList();
                  client.deleteObjects(DeleteObjectsRequest.builder()
                                                           .bucket(bucketName)
                                                           .delete(r -> r.objects(deleteList))
                                                           .build());
                  log.debug("Delete objects request complete");
              });
    }
}
