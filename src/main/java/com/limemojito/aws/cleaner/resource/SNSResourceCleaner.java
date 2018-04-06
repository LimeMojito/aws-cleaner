/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2016
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.limemojito.aws.cleaner.resource;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SNSResourceCleaner extends BaseAwsResourceCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(SNSResourceCleaner.class);
    private final AmazonSNS client;

    @Autowired
    public SNSResourceCleaner(AmazonSNS client) {
        this.client = client;
    }

    @Override
    public String getName() {
        return "SNS Cleaner";
    }

    @Override
    public void clean() {
        LOGGER.debug("Listing topics");
        for (Topic topic : client.listTopics().getTopics()) {
            final String topicArn = topic.getTopicArn();
            unsubscribeAll(topicArn);
        }
        LOGGER.debug("Listing platforms");
        final ListPlatformApplicationsResult listPlatformApplicationsResult = client.listPlatformApplications();
        if (listPlatformApplicationsResult != null) {
            for (PlatformApplication platform : listPlatformApplicationsResult.getPlatformApplications()) {
                final String applicationArn = platform.getPlatformApplicationArn();
                removeApplicationEndpoints(applicationArn);
            }
        }
    }

    private void removeApplicationEndpoints(String applicationArn) {
        LOGGER.debug("Removing endpoints from {}", applicationArn);
        final ListEndpointsByPlatformApplicationRequest applicationRequest = new ListEndpointsByPlatformApplicationRequest()
                .withPlatformApplicationArn(applicationArn);
        ListEndpointsByPlatformApplicationResult endpoints = client.listEndpointsByPlatformApplication(applicationRequest);
        for (Endpoint endpoint : endpoints.getEndpoints()) {
            String endpointArn = endpoint.getEndpointArn();
            removeEndpoint(endpointArn);
        }
    }

    private void removeEndpoint(String endpointArn) {
        LOGGER.debug("Removing {}", endpointArn);
        performWithThrottle(() -> client.deleteEndpoint(new DeleteEndpointRequest().withEndpointArn(endpointArn)));
    }

    private void unsubscribeAll(String topicArn) {
        LOGGER.debug("Listing subscriptions for {}", topicArn);
        for (Subscription subscription : client.listSubscriptionsByTopic(topicArn).getSubscriptions()) {
            String subscriptionArn = subscription.getSubscriptionArn();
            LOGGER.debug("Unsubscribing {}", subscriptionArn);
            performWithThrottle(() -> client.unsubscribe(subscriptionArn));
        }
    }
}
