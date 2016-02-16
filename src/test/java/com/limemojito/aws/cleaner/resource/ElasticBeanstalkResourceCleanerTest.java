/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2015
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.limemojito.aws.cleaner.resource;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentStatus;
import com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentRequest;
import com.limemojito.aws.cleaner.ResourceCleaner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collection;
import java.util.LinkedList;

import static com.amazonaws.services.elasticbeanstalk.model.EnvironmentStatus.Ready;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ElasticBeanstalkResourceCleanerTest extends AwsResourceCleanerUnitTestCase {

    @Mock
    private AWSElasticBeanstalkClient client;

    private ResourceCleaner cleaner;

    @Before
    public void setup() {
        cleaner = new ElasticBeanstalkResourceCleaner(client);
    }

    @Test
    public void shouldCleanLocalOnly() throws Exception {
        when(client.describeEnvironments()).thenReturn(createExampleEnvironments());

        assertThat(cleaner.getName(), is("Elastic Beanstalk Cleaner"));

        cleaner.clean("LOCAL");

        verify(client).describeEnvironments();
        verify(client, times(1)).terminateEnvironment(createTerminateRequest("LOCAL"));
    }

    @Test
    public void shouldDeleteOnThrottle() throws Exception {
        TerminateEnvironmentRequest expectedRequest = createTerminateRequest("LOCAL");
        when(client.describeEnvironments()).thenReturn(createExampleEnvironments());
        when(client.terminateEnvironment(expectedRequest)).thenThrow(createThrottleException()).thenReturn(null);

        cleaner.clean("LOCAL");

        verify(client).describeEnvironments();
        verify(client, times(2)).terminateEnvironment(expectedRequest);
        verify(client, times(0)).terminateEnvironment(createTerminateRequest("DEV"));
        verify(client, times(0)).terminateEnvironment(createTerminateRequest("PROD"));
    }

    private TerminateEnvironmentRequest createTerminateRequest(String environmentName) {
        return new TerminateEnvironmentRequest().withEnvironmentName(environmentName);
    }

    private DescribeEnvironmentsResult createExampleEnvironments() {
        final Collection<EnvironmentDescription> environments = new LinkedList<>();
        environments.add(new EnvironmentDescription().withEnvironmentName("LOCAL").withStatus(Ready));
        environments.add(new EnvironmentDescription().withEnvironmentName("DEV").withStatus(Ready));
        environments.add(new EnvironmentDescription().withEnvironmentName("PROD").withStatus(Ready));
        return new DescribeEnvironmentsResult().withEnvironments(environments);
    }
}
