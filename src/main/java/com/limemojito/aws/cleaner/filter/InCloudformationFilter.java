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

import com.limemojito.aws.cleaner.resource.Throttle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.StackResource;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Filter implementation that determines if a resource should be deleted based on whether
 * it is part of a CloudFormation stack.
 * Resources that are managed by CloudFormation stacks are preserved, while standalone
 * resources are candidates for deletion.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InCloudformationFilter implements DeletionFilter {
    private final CloudFormationClient cloudFormation;

    /**
     * Determines if a resource should be deleted by checking if it belongs to a CloudFormation stack.
     * Resources that are part of a CloudFormation stack are preserved.
     *
     * @param physicalId The physical ID of the AWS resource to evaluate
     * @return true if the resource should be deleted (not in a CloudFormation stack),
     * false if it should be preserved (part of a CloudFormation stack)
     */
    @Override
    public boolean shouldDelete(String physicalId) {
        final AtomicBoolean resourceInCloudformationStack = new AtomicBoolean();
        Throttle.performWithThrottle(() -> {
            final boolean inCf = isInCloudformation(physicalId);
            resourceInCloudformationStack.set(inCf);
            log.debug("is {} in cloudformation? {}", physicalId, inCf);
        });
        return !resourceInCloudformationStack.get();
    }

    private boolean isInCloudformation(String physicalId) {
        try {
            final DescribeStackResourcesResponse describeStackResourcesResponse = cloudFormation.describeStackResources(
                    r -> r.physicalResourceId(physicalId));
            final Set<String> stacks = describeStackResourcesResponse.stackResources()
                                                                     .stream()
                                                                     .map(StackResource::stackName)
                                                                     .collect(Collectors.toSet());
            log.debug("{} is in stack(s) {}", physicalId, stacks);
            return describeStackResourcesResponse.hasStackResources();
        } catch (CloudFormationException e) {
            if (e.getMessage().contains("Stack") && e.getMessage().contains("does not exist")) {
                return false;
            } else {
                throw e;
            }
        }
    }

}
