/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2015
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.limemojito.aws.cleaner.resource;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.lang.String.format;

@Service
public class DynamoResourceCleaner extends BaseAwsResourceCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoResourceCleaner.class);
    private final AmazonDynamoDBClient dbClient;

    @Autowired
    public DynamoResourceCleaner(AmazonDynamoDBClient dbClient) {
        this.dbClient = dbClient;
    }

    @Override
    public String getName() {
        return "DynamoDB Cleaner";
    }

    @Override
    public void clean(String environment) {
        LOGGER.debug("Scanning tables for {} prefix", environment);
        final ListTablesResult listTablesResult = dbClient.listTables();
        final List<String> tableNames = listTablesResult.getTableNames();
        LOGGER.debug("Scanning {} tables", tableNames.size());
        final String tablePrefix = ALL_ENVIRONMENTS.equals(environment) ? "" : format("%s-", environment);
        tableNames.stream().filter(s -> s.startsWith(tablePrefix)).forEach((tableName) -> performWithThrottle(() -> {
            LOGGER.debug("Deleting table {}", tableName);
            dbClient.deleteTable(tableName);
        }));
    }

}
