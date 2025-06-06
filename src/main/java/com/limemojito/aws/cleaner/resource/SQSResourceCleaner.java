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
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.List;

/**
 * Resource cleaner for AWS SQS queues.
 * This cleaner identifies and deletes SQS queues in the AWS account.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SQSResourceCleaner extends PhysicalResourceCleaner {
    private final SqsClient client;

    /**
     * {@inheritDoc}
     * Retrieves a list of all SQS queue URLs in the AWS account.
     *
     * @return A list of SQS queue URLs
     */
    @Override
    protected List<String> getPhysicalResourceIds() {
        log.debug("Getting SQS Queue URLs");
        return client.listQueuesPaginator()
                     .stream()
                     .flatMap(page -> page.queueUrls().stream())
                     .toList();
    }

    /**
     * {@inheritDoc}
     * Deletes an SQS queue identified by its URL.
     *
     * @param physicalId The URL of the SQS queue to delete
     */
    @Override
    protected void performDelete(String physicalId) {
        log.info("Deleting Queue {}", physicalId);
        client.deleteQueue(r -> r.queueUrl(physicalId));
    }
}
