/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2015
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.limemojito.aws.cleaner.resource;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.limemojito.aws.cleaner.ResourceCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DynamoResourceCleaner implements ResourceCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoResourceCleaner.class);
    private static final int BACKOFF_SECONDS = 2;
    private static final long SECONDS_TO_MILLIS = 1_000L;
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
        final String tablePrefix = String.format("%s-", environment);
        tableNames.stream().filter(s -> s.startsWith(tablePrefix)).forEach((tableName) -> performWithThrottle(() -> {
            LOGGER.debug("Deleting table {}", tableName);
            dbClient.deleteTable(tableName);
        }));
    }

    private interface AwsAction {
        void performAction();
    }

    private void performWithThrottle(AwsAction action) {
        try {
            action.performAction();
        } catch (AmazonServiceException e) {
            if ("Throttling".equals(e.getErrorCode())) {
                LOGGER.warn("Throttled API calls detected, backoff {} seconds", BACKOFF_SECONDS);
                try {
                    Thread.sleep(BACKOFF_SECONDS * SECONDS_TO_MILLIS);
                } catch (InterruptedException e1) {
                    LOGGER.warn("Interrupted");
                }
                action.performAction();
            }
        }
    }
}
