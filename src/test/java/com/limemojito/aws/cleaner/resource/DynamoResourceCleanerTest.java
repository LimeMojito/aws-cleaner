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
import com.limemojito.aws.cleaner.ResourceCleaner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DynamoResourceCleanerTest extends AwsResourceCleanerUnitTestCase {

    @Mock
    private AmazonDynamoDBClient client;
    private ResourceCleaner cleaner;

    @Before
    public void setUp() throws Exception {
        cleaner = new DynamoResourceCleaner(client);
    }

    @Test
    public void shouldCleanViaClient() throws Exception {
        when(client.listTables()).thenReturn(expectedListTables());

        assertThat(cleaner.getName(), is("DynamoDB Cleaner"));

        cleaner.clean();

        verify(client).listTables();
        verify(client).deleteTable("LOCAL-TABLE");
        verify(client).deleteTable("DEV-TABLE");
        verify(client).deleteTable("PROD-TABLE");
    }

    @Test
    public void shouldDeleteOnThrottle() throws Exception {
        when(client.listTables()).thenReturn(expectedListTables());
        when(client.deleteTable("LOCAL-TABLE")).thenThrow(createThrottleException()).thenReturn(null);

        cleaner.clean();

        verify(client).listTables();
        verify(client, times(2)).deleteTable("LOCAL-TABLE");
        verify(client).deleteTable("DEV-TABLE");
        verify(client).deleteTable("PROD-TABLE");
    }

    private ListTablesResult expectedListTables() {
        final ListTablesResult listTablesResult = new ListTablesResult();
        listTablesResult.setTableNames(asList("LOCAL-TABLE", "DEV-TABLE", "PROD-TABLE"));
        return listTablesResult;
    }
}
