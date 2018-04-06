/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2016
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.limemojito.aws.cleaner.resource;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SNSResourceCleanerTest extends AwsResourceCleanerUnitTestCase {

    private static final String TOPIC = "some:arn:-DEV-";
    private static final String SUBSUBSCRIPTION_ARN = "sub";
    private static final String ENDPOINT_ARN = "endpointArn";
    @Mock
    private AmazonSNSClient snsClient;

    @Test
    public void shouldCleanDevOk() throws Exception {
        when(snsClient.listPlatformApplications()).thenReturn(platforms());
        when(snsClient.listEndpointsByPlatformApplication(any(ListEndpointsByPlatformApplicationRequest.class))).thenReturn(endpoints());
        performTopicSubscriptionDelete(TOPIC);

        verify(snsClient, times(1)).listPlatformApplications();
        verify(snsClient, times(1)).listEndpointsByPlatformApplication(any(ListEndpointsByPlatformApplicationRequest.class));
        verify(snsClient, times(1)).deleteEndpoint(any(DeleteEndpointRequest.class));
    }

    private ListEndpointsByPlatformApplicationResult endpoints() {
        return new ListEndpointsByPlatformApplicationResult().withEndpoints(new Endpoint().withEndpointArn(ENDPOINT_ARN));
    }

    private ListPlatformApplicationsResult platforms() {
        return new ListPlatformApplicationsResult().withPlatformApplications(devPlatform());
    }

    private PlatformApplication devPlatform() {
        return new PlatformApplication().withPlatformApplicationArn("something_DEVELOPMENT");
    }

    private void performTopicSubscriptionDelete(String topicArn) {
        when(snsClient.listTopics()).thenReturn(topicList());
        when(snsClient.listSubscriptionsByTopic(topicArn)).thenReturn(subscriptions(topicArn));

        SNSResourceCleaner resourceCleaner = new SNSResourceCleaner(snsClient);

        resourceCleaner.clean();

        verify(snsClient, times(2)).listTopics();
        verify(snsClient).listSubscriptionsByTopic(topicArn);
        verify(snsClient).unsubscribe(SUBSUBSCRIPTION_ARN);
    }

    private ListSubscriptionsByTopicResult subscriptions(String topicArn) {
        return new ListSubscriptionsByTopicResult().withSubscriptions(createSubscription(topicArn));
    }

    private Subscription createSubscription(String topicArn) {
        return new Subscription().withTopicArn(topicArn).withSubscriptionArn(SUBSUBSCRIPTION_ARN);
    }

    private ListTopicsResult topicList() {
        return new ListTopicsResult().withTopics(createTopic(TOPIC));
    }

    private Topic createTopic(String arn) {
        return new Topic().withTopicArn(arn);
    }
}
