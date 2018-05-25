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

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentRequest;
import com.limemojito.aws.cleaner.ResourceCleaner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collection;
import java.util.LinkedList;

import static com.amazonaws.services.elasticbeanstalk.model.EnvironmentStatus.Ready;
import static org.mockito.Mockito.*;

public class ElasticBeanstalkResourceCleanerTest extends AwsResourceCleanerUnitTestCase {

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

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

        cleaner.clean();

        verify(client).describeEnvironments();
        verify(client, times(1)).terminateEnvironment(createTerminateRequest("LOCAL"));
    }

    @Test
    public void shouldDeleteOnThrottle() throws Exception {
        TerminateEnvironmentRequest expectedRequest = createTerminateRequest("LOCAL");
        when(client.describeEnvironments()).thenReturn(createExampleEnvironments());
        when(client.terminateEnvironment(expectedRequest)).thenThrow(createThrottleException()).thenReturn(null);

        cleaner.clean();

        verify(client).describeEnvironments();
        verify(client, times(2)).terminateEnvironment(expectedRequest);
        verify(client).terminateEnvironment(createTerminateRequest("DEV"));
        verify(client).terminateEnvironment(createTerminateRequest("PROD"));
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
