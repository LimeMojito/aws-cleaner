/*
 * Copyright 2011-2025 Lime Mojito Pty Ltd
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.Subscription;
import software.amazon.awssdk.services.sns.model.Topic;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.limemojito.aws.cleaner.resource.Throttle.performWithThrottle;

/**
 * Resource cleaner for AWS SNS topics and subscriptions.
 * This cleaner identifies and deletes SNS topics and also handles dangling SQS subscriptions
 * (subscriptions to queues that no longer exist).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SNSResourceCleaner extends PhysicalResourceCleaner {
    private static final int AWS_ACCOUNT_GROUP = 2;
    private static final int QUEUE_NAME_GROUP = 3;
    private final Pattern queueArnMatcher = Pattern.compile("^arn:aws:sqs:(.+?):(.+?):(.+)");
    private final SnsClient sns;
    private final SqsClient sqs;

    /**
     * {@inheritDoc}
     * Extends the base implementation to also clean up dangling SQS subscriptions
     * (subscriptions to queues that no longer exist).
     */
    @Override
    public void clean() {
        super.clean();
        log.debug("Cleaning SNS Subscriptions");
        sns.listSubscriptionsPaginator()
           .stream()
           .flatMap(page -> page.subscriptions().stream())
           .forEach(this::removeSqsSubscription);
    }

    /**
     * {@inheritDoc}
     * Retrieves a list of all SNS topic ARNs in the AWS account.
     *
     * @return A list of SNS topic ARNs
     */
    @Override
    protected List<String> getPhysicalResourceIds() {
        log.debug("Getting SNS Topics");
        return sns.listTopicsPaginator()
                  .stream()
                  .flatMap(page -> page.topics().stream())
                  .map(Topic::topicArn)
                  .toList();
    }

    /**
     * {@inheritDoc}
     * Deletes an SNS topic and all its subscriptions.
     * First unsubscribes all subscriptions to the topic, then deletes the topic itself.
     *
     * @param physicalId The ARN of the SNS topic to delete
     */
    @Override
    protected void performDelete(String physicalId) {
        log.info("Deleting Topic {} and all subscriptions", physicalId);
        sns.listSubscriptionsByTopicPaginator(r -> r.topicArn(physicalId))
           .stream()
           .flatMap(page -> page.subscriptions().stream())
           .map(Subscription::subscriptionArn)
           .forEach(this::unsubscribe);
        sns.deleteTopic(r -> r.topicArn(physicalId));
    }

    private void removeSqsSubscription(Subscription subscription) {
        log.debug("Checking {}", subscription.subscriptionArn());
        if (getFilter().shouldDelete(subscription.subscriptionArn())
                && "SQS".equalsIgnoreCase(subscription.protocol())) {
            String queueArn = subscription.endpoint();
            final Matcher matcher = queueArnMatcher.matcher(queueArn);
            if (matcher.matches()) {
                try {
                    final String qName = matcher.group(QUEUE_NAME_GROUP);
                    final String owner = matcher.group(AWS_ACCOUNT_GROUP);
                    performWithThrottle(() -> sqs.getQueueUrl(r -> r.queueName(qName)
                                                                    .queueOwnerAWSAccountId(owner)));
                } catch (QueueDoesNotExistException e) {
                    removeQueueSubscription(subscription);
                }
            }
        }
    }

    private void removeQueueSubscription(Subscription subscription) {
        if (isCommit()) {
            log.info("Removing dangling subscription {} to {}",
                     subscription.subscriptionArn(),
                     subscription.endpoint());
            unsubscribe(subscription.subscriptionArn());
        } else {
            log.info("Would delete dangling subscription {} to {}",
                     subscription.subscriptionArn(),
                     subscription.endpoint());
        }
    }

    private void unsubscribe(String subArn) {
        performWithThrottle(() -> {
            log.info("Unsubscribe {}", subArn);
            sns.unsubscribe(r -> r.subscriptionArn(subArn));
        });
    }
}
