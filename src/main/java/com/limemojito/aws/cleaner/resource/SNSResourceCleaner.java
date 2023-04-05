/*
 * Copyright 2011-2023 Lime Mojito Pty Ltd
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
import com.amazonaws.services.sns.model.ListSubscriptionsRequest;
import com.amazonaws.services.sns.model.ListSubscriptionsResult;
import com.amazonaws.services.sns.model.Subscription;
import com.amazonaws.services.sns.model.Topic;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.limemojito.aws.cleaner.resource.Throttle.performWithThrottle;

@Service
@RequiredArgsConstructor
@Slf4j
public class SNSResourceCleaner extends PhysicalResourceCleaner {
    private static final int AWS_ACCOUNT_GROUP = 2;
    private static final int QUEUE_NAME_GROUP = 3;
    private final Pattern queueArnMatcher = Pattern.compile("^arn:aws:sqs:(.+?):(.+?):(.+)");
    private final AmazonSNS client;
    private final AmazonSQS sqs;


    @Override
    public void clean() {
        super.clean();
        // remove any dangling SQS subscriptions
        ListSubscriptionsRequest listSubscriptionsRequest = new ListSubscriptionsRequest();
        do {
            ListSubscriptionsResult listSubscriptionsResult = client.listSubscriptions(listSubscriptionsRequest);
            listSubscriptionsRequest.setNextToken(listSubscriptionsResult.getNextToken());
            List<Subscription> subscriptions = listSubscriptionsResult.getSubscriptions();
            subscriptions.forEach(this::removeSqsSubscription);
        } while (listSubscriptionsRequest.getNextToken() != null);
    }

    private void removeSqsSubscription(Subscription subscription) {
        log.debug("Checking {}", subscription.getSubscriptionArn());
        if (getFilter().shouldDelete(subscription.getSubscriptionArn())
                && "SQS".equalsIgnoreCase(subscription.getProtocol())) {
            String queueArn = subscription.getEndpoint();
            final Matcher matcher = queueArnMatcher.matcher(queueArn);
            if (matcher.matches()) {
                try {
                    performWithThrottle(() ->
                            sqs.getQueueUrl(new GetQueueUrlRequest(matcher.group(QUEUE_NAME_GROUP))
                                    .withQueueOwnerAWSAccountId(matcher.group(AWS_ACCOUNT_GROUP))));
                } catch (QueueDoesNotExistException e) {
                    removeQueueSubscription(subscription);
                }
            }
        }
    }

    private void removeQueueSubscription(Subscription subscription) {
        if (isCommit()) {
            log.info("Removing dangling subscription {} to {}",
                    subscription.getSubscriptionArn(),
                    subscription.getEndpoint());
            unsubscribe(subscription.getSubscriptionArn());
        } else {
            log.info("Would delete dangling subscription {} to {}",
                    subscription.getSubscriptionArn(),
                    subscription.getEndpoint());
        }
    }

    @Override
    protected List<String> getPhysicalResourceIds() {
        return client.listTopics().getTopics().stream()
                .map(Topic::getTopicArn)
                .collect(Collectors.toList());
    }

    protected void performDelete(String physicalId) {
        log.info("Deleting Topic {} and all subscriptions", physicalId);
        client.listSubscriptionsByTopic(physicalId)
                .getSubscriptions()
                .stream()
                .map(Subscription::getSubscriptionArn)
                .forEach(this::unsubscribe);
        client.deleteTopic(physicalId);
    }

    private void unsubscribe(String subArn) {
        performWithThrottle(() -> {
            log.info("Unsubscribe {}", subArn);
            client.unsubscribe(subArn);
        });
    }
}
