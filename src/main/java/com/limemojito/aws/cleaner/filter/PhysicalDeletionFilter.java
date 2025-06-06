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

package com.limemojito.aws.cleaner.filter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.split;

/**
 * Interface for filtering AWS resources to determine which ones should be deleted.
 * Implementations of this interface provide the logic to decide whether a specific
 * AWS resource should be deleted based on its physical ID.
 */
@Service
@Slf4j
public class PhysicalDeletionFilter {
    private final InCloudformationFilter cloudformation;
    private final List<String> notContainsNames;

    public PhysicalDeletionFilter(InCloudformationFilter cloudformation,
                                  @Value("${cleaner.skip.names:}") String notContainsNames) {
        this.cloudformation = cloudformation;
        this.notContainsNames = stripCommaSeparated(notContainsNames);
    }

    /**
     * Determines whether a resource with the given physical ID should be deleted.
     *
     * @param physicalId The physical ID of the AWS resource to evaluate
     * @return true if the resource should be deleted, false otherwise
     */
    public boolean shouldDelete(String physicalId) {
        if (cloudformation.shouldDelete(physicalId)) {
            final boolean delete = notContainsNames.stream().noneMatch(physicalId::contains);
            if (!delete) {
                log.info("{} is in cleaner.skip.names {}", physicalId, notContainsNames);
            }
            return delete;
        }
        return false;
    }

    /**
     * Converts comma separated string to trimmed parts.
     *
     * @param commaSeparated Comma separated string
     * @return List of individual string values.
     */
    public static List<String> stripCommaSeparated(String commaSeparated) {
        return Arrays.stream(split(commaSeparated, ','))
                     .map(StringUtils::trimToEmpty)
                     .collect(toList());
    }
}
