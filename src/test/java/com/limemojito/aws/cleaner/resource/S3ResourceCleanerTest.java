/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2015
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.limemojito.aws.cleaner.resource;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.limemojito.aws.cleaner.ResourceCleaner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class S3ResourceCleanerTest extends AwsResourceCleanerUnitTestCase {

    @Mock
    private AmazonS3Client client;
    private ResourceCleaner cleaner;

    @Before
    public void setUp() throws Exception {
        cleaner = new S3ResourceCleaner(client);
    }

    @Test
    public void shouldCleanLocalS3Ok() throws Exception {
        when(client.listBuckets()).thenReturn(createBucketList());

        assertThat(cleaner.getName(), is("S3 Cleaner"));

        cleaner.clean("LOCAL");

        verify(client, times(1)).deleteBucket("test-local-bucket");
        verify(client, times(0)).deleteBucket("test-dev-bucket");
        verify(client, times(0)).deleteBucket("test-prod-bucket");
    }

    @Test
    public void shouldDeleteOnThrottle() throws Exception {
        when(client.listBuckets()).thenReturn(createBucketList());
        doThrow(createThrottleException()).doNothing().when(client).deleteBucket("test-local-bucket");

        cleaner.clean("LOCAL");

        verify(client, times(2)).deleteBucket("test-local-bucket");
        verify(client, times(0)).deleteBucket("test-dev-bucket");
        verify(client, times(0)).deleteBucket("test-prod-bucket");
    }

    private List<Bucket> createBucketList() {
        List<Bucket> buckets = new LinkedList<>();
        buckets.add(new Bucket("test-local-bucket"));
        buckets.add(new Bucket("test-dev-bucket"));
        buckets.add(new Bucket("test-prod-bucket"));
        return buckets;
    }
}
