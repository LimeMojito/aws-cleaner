/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2015
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.limemojito.aws.cleaner.resource;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DynamoResourceCleaner extends PhysicalResourceCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoResourceCleaner.class);
    private final AmazonDynamoDB dbClient;

    @Autowired
    public DynamoResourceCleaner(AmazonDynamoDB dbClient) {
        super();
        this.dbClient = dbClient;
    }

    @Override
    protected List<String> getPhysicalResourceIds() {
        LOGGER.debug("Scanning tables");
        return dbClient.listTables().getTableNames();
    }

    @Override
    protected void performDelete(String physicalId) {
        LOGGER.debug("Deleting resource {}", physicalId);
        dbClient.deleteTable(physicalId);
    }
}
