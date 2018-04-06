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

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SNSResourceCleaner extends CompositeResourceCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(SNSResourceCleaner.class);

    @Autowired
    public SNSResourceCleaner(AmazonSNS client) {
        super(new SnsSubscriptionCleaner(client), new SnsPlatformCleaner(client), new SnsTopicCleaner(client));
    }

    private static class SnsTopicCleaner extends PhysicalResourceCleaner {
        private final AmazonSNS client;

        SnsTopicCleaner(AmazonSNS client) {
            super();
            this.client = client;
        }

        @Override
        protected List<String> getPhysicalResourceIds() {
            return client.listTopics().getTopics().stream().map(Topic::getTopicArn).collect(Collectors.toList());
        }

        @Override
        protected void performDelete(String physicalId) {
            client.deleteTopic(physicalId);
        }
    }

    private static class SnsSubscriptionCleaner extends PhysicalResourceCleaner {
        private final AmazonSNS client;

        SnsSubscriptionCleaner(AmazonSNS client) {
            super();
            this.client = client;
        }

        @Override
        protected List<String> getPhysicalResourceIds() {
            return client.listTopics().getTopics().stream().map(Topic::getTopicArn).collect(Collectors.toList());
        }

        @Override
        protected void performDelete(String physicalId) {
            LOGGER.debug("Listing subscriptions for {}", physicalId);
            for (Subscription subscription : client.listSubscriptionsByTopic(physicalId).getSubscriptions()) {
                String subscriptionArn = subscription.getSubscriptionArn();
                LOGGER.debug("Unsubscribe {}", subscriptionArn);
                Throttle.performWithThrottle(() -> client.unsubscribe(subscriptionArn));
            }
        }
    }

    private static class SnsPlatformCleaner extends PhysicalResourceCleaner {
        private final AmazonSNS client;

        SnsPlatformCleaner(AmazonSNS client) {
            super();
            this.client = client;
        }

        @Override
        protected List<String> getPhysicalResourceIds() {
            return client.listPlatformApplications()
                         .getPlatformApplications()
                         .stream()
                         .map(PlatformApplication::getPlatformApplicationArn)
                         .collect(Collectors.toList());
        }

        @Override
        protected void performDelete(String physicalId) {
            LOGGER.debug("Removing endpoints from {}", physicalId);
            final ListEndpointsByPlatformApplicationRequest applicationRequest = new ListEndpointsByPlatformApplicationRequest()
                    .withPlatformApplicationArn(physicalId);
            ListEndpointsByPlatformApplicationResult endpoints = client.listEndpointsByPlatformApplication(applicationRequest);
            for (Endpoint endpoint : endpoints.getEndpoints()) {
                String endpointArn = endpoint.getEndpointArn();
                LOGGER.debug("Removing {}", endpointArn);
                Throttle.performWithThrottle(() -> client.deleteEndpoint(new DeleteEndpointRequest().withEndpointArn(endpointArn)));
            }
        }
    }

}
