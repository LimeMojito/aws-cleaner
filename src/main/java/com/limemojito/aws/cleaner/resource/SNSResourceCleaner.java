/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2016
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.limemojito.aws.cleaner.resource;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.Subscription;
import com.amazonaws.services.sns.model.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SNSResourceCleaner extends CompositeResourceCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(SNSResourceCleaner.class);
    private static List<String> topics;

    @Autowired
    public SNSResourceCleaner(AmazonSNS client) {
        super(new SnsSubscriptionCleaner(client), new SnsTopicCleaner(client));
        topics = client.listTopics().getTopics().stream().map(Topic::getTopicArn).collect(Collectors.toList());
    }

    private static class SnsTopicCleaner extends PhysicalResourceCleaner {
        private final AmazonSNS client;

        SnsTopicCleaner(AmazonSNS client) {
            super();
            this.client = client;
        }

        @Override
        protected List<String> getPhysicalResourceIds() {
            return topics;
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
            LOGGER.info("Fetching Subscriptions.");
            return topics.stream()
                         .map(topic -> client.listSubscriptionsByTopic(topic)
                                             .getSubscriptions()
                                             .stream()
                                             .map(Subscription::getSubscriptionArn)
                                             .collect(Collectors.toList()))
                         .flatMap(Collection::stream)
                         .collect(Collectors.toList());
        }

        @Override
        protected void performDelete(String physicalId) {
            client.unsubscribe(physicalId);
        }
    }
}
