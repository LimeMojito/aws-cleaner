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
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.LogGroup;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Resource cleaner for AWS CloudWatch Log Groups.
 * This cleaner identifies and deletes log groups that have no stored data (0 bytes).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LogGroupCleaner extends PhysicalResourceCleaner {
    private final CloudWatchLogsClient client;

    /**
     * {@inheritDoc}
     * Retrieves a list of all CloudWatch Log Group names.
     *
     * @return A list of Log Group names
     */
    @Override
    protected List<String> getPhysicalResourceIds() {
        return client.describeLogGroups()
                     .logGroups()
                     .stream()
                     .map(LogGroup::logGroupName)
                     .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     * Deletes a CloudWatch Log Group only if it has no stored data (0 bytes).
     * Log groups with data are preserved.
     *
     * @param physicalId The name of the Log Group to delete
     */
    protected void performDelete(String physicalId) {
        log.debug("Checking group {}", physicalId);
        if (client.describeLogGroups(r -> r.logGroupNamePrefix(physicalId))
                  .logGroups()
                  .getFirst()
                  .storedBytes() == 0) {
            log.info("Removing group {}", physicalId);
            client.deleteLogGroup(r -> r.logGroupName(physicalId));
        }
    }
}
