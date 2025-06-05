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

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.limemojito.aws.cleaner.ResourceCleaner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

public class DynamoResourceCleanerTest extends AwsResourceCleanerUnitTestCase {

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private AmazonDynamoDBClient client;

    private ResourceCleaner cleaner;

    @Before
    public void setUp() {
        cleaner = new DynamoResourceCleaner(client);
        cleaner.setCommit(true);
    }

    @Test
    public void shouldCleanViaClient() {
        when(client.listTables()).thenReturn(expectedListTables());

        cleaner.clean();

        verify(client).listTables();
        verify(client).deleteTable("LOCAL-TABLE");
        verify(client).deleteTable("DEV-TABLE");
        verify(client).deleteTable("PROD-TABLE");
    }

    @Test
    public void shouldDeleteOnThrottle() {
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
