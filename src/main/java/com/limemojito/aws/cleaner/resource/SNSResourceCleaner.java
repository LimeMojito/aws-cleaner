/*
 * Copyright 2020 Lime Mojito Pty Ltd
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

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.Subscription;
import com.amazonaws.services.sns.model.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.limemojito.aws.cleaner.resource.Throttle.performWithThrottle;

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
        performWithThrottle(() -> {
            LOGGER.info("Unsubscribe {}", subArn);
            client.unsubscribe(subArn);
        });
    }
}
