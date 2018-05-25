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

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.limemojito.aws.cleaner.ResourceCleaner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.*;

public class S3ResourceCleanerTest extends AwsResourceCleanerUnitTestCase {

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private AmazonS3Client client;
    private ResourceCleaner cleaner;

    @Before
    public void setUp() throws Exception {
        cleaner = new S3ResourceCleaner(client);
        cleaner.setCommit(true);
    }

    @Test
    public void shouldCleanLocalS3Ok() throws Exception {
        when(client.listBuckets()).thenReturn(createBucketList());

        cleaner.clean();

        verify(client, times(1)).deleteBucket("test-local-bucket");
        verify(client).deleteBucket("test-dev-bucket");
        verify(client).deleteBucket("test-prod-bucket");
    }

    @Test
    public void shouldDeleteOnThrottle() throws Exception {
        when(client.listBuckets()).thenReturn(createBucketList());
        doThrow(createThrottleException()).doNothing().when(client).deleteBucket("test-local-bucket");

        cleaner.clean();

        verify(client, times(2)).deleteBucket("test-local-bucket");
        verify(client).deleteBucket("test-dev-bucket");
        verify(client).deleteBucket("test-prod-bucket");
    }

    @Test
    public void shouldDeleteNonEmptyBucket() throws Exception {
        final ObjectListing expectedFileList = createFileList();
        when(client.listBuckets()).thenReturn(createBucketList());
        when(client.listObjects("test-local-bucket")).thenReturn(expectedFileList);
        doThrow(createsS3NotEmptyException()).doNothing().when(client).deleteBucket("test-local-bucket");

        cleaner.clean();

        verify(client).deleteObjects(any(DeleteObjectsRequest.class));
        verify(client, times(2)).deleteBucket("test-local-bucket");
        verify(client).deleteBucket("test-dev-bucket");
        verify(client).deleteBucket("test-prod-bucket");
    }

    private ObjectListing createFileList() {
        final ObjectListing objectListing = new ObjectListing();
        objectListing.getObjectSummaries().add(createFile("test-one.jpg"));
        objectListing.getObjectSummaries().add(createFile("folder/test-two.txt"));
        objectListing.getObjectSummaries().add(createFile("test-three.dat"));
        return objectListing;
    }

    private S3ObjectSummary createFile(String fileName) {
        final S3ObjectSummary s3ObjectSummary = new S3ObjectSummary();
        s3ObjectSummary.setKey(fileName);
        return s3ObjectSummary;
    }

    private List<Bucket> createBucketList() {
        List<Bucket> buckets = new LinkedList<>();
        buckets.add(new Bucket("test-local-bucket"));
        buckets.add(new Bucket("test-dev-bucket"));
        buckets.add(new Bucket("test-prod-bucket"));
        return buckets;
    }
}
