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
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;

/**
 * Resource cleaner for AWS DynamoDB tables.
 * This cleaner identifies and deletes DynamoDB tables in the AWS account.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DynamoResourceCleaner extends PhysicalResourceCleaner {
    private final DynamoDbClient dbClient;

    /**
     * {@inheritDoc}
     * Retrieves a list of all DynamoDB table names in the AWS account.
     *
     * @return A list of DynamoDB table names
     */
    @Override
    protected List<String> getPhysicalResourceIds() {
        log.debug("Scanning tables");
        return dbClient.listTablesPaginator().stream().flatMap(p -> p.tableNames().stream()).toList();
    }

    /**
     * {@inheritDoc}
     * Deletes a DynamoDB table identified by its name.
     *
     * @param physicalId The name of the DynamoDB table to delete
     */
    @Override
    protected void performDelete(String physicalId) {
        log.info("Deleting resource {}", physicalId);
        dbClient.deleteTable(r -> r.tableName(physicalId));
    }
}
