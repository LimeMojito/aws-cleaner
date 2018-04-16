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

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SNSResourceCleaner extends PhysicalResourceCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(SNSResourceCleaner.class);
    private final AmazonSNS client;

    @Autowired
    public SNSResourceCleaner(AmazonSNS client) {
        this.client = client;
    }

    @Override
    protected List<String> getPhysicalResourceIds() {
        return client.listTopics().getTopics().stream()
                     .map(Topic::getTopicArn)
                     .collect(Collectors.toList());
    }

    protected void performDelete(String physicalId) {
        LOGGER.info("Deleting Topic {} and all subscriptions", physicalId);
        client.listSubscriptionsByTopic(physicalId)
              .getSubscriptions()
              .stream()
              .map(Subscription::getSubscriptionArn)
              .forEach(this::unsubscribe);
        client.deleteTopic(physicalId);
    }

    private void unsubscribe(String subArn) {
        Throttle.performWithThrottle(() -> {
            LOGGER.info("Unsubscribe {}", subArn);
            client.unsubscribe(subArn);
        });
    }
}
