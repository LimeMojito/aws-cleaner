/*
 * Copyright 2011-2024 Lime Mojito Pty Ltd
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

import com.amazonaws.services.sqs.AmazonSQS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SQSResourceCleaner extends PhysicalResourceCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(SQSResourceCleaner.class);
    private final AmazonSQS client;

    @Autowired
    public SQSResourceCleaner(AmazonSQS client) {
        this.client = client;
    }

    @Override
    protected List<String> getPhysicalResourceIds() {
        return new ArrayList<>(client.listQueues().getQueueUrls());
    }

    protected void performDelete(String physicalId) {
        LOGGER.info("Deleting Queue {}", physicalId);
        client.deleteQueue(physicalId);
    }
}
