/*
 * Copyright 2011-2023 Lime Mojito Pty Ltd
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

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.model.DeleteLogGroupRequest;
import com.amazonaws.services.logs.model.DescribeLogGroupsRequest;
import com.amazonaws.services.logs.model.LogGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogGroupCleaner extends PhysicalResourceCleaner {
    private final AWSLogs client;

    @Override
    protected List<String> getPhysicalResourceIds() {
        return client.describeLogGroups()
                .getLogGroups()
                .stream()
                .map(LogGroup::getLogGroupName)
                .collect(Collectors.toList());
    }

    protected void performDelete(String physicalId) {
        log.debug("Checking group {}", physicalId);
        if (client.describeLogGroups(new DescribeLogGroupsRequest().withLogGroupNamePrefix(physicalId))
                .getLogGroups()
                .get(0)
                .getStoredBytes() == 0) {
            log.info("Removing group {}", physicalId);
            client.deleteLogGroup(new DeleteLogGroupRequest(physicalId));
        }
    }
}
