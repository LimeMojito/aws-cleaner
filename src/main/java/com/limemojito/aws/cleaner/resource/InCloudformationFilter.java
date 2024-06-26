/*
 * Copyright 2011-2024 Lime Mojito Pty Ltd
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

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.limemojito.aws.cleaner.resource.Throttle.performWithThrottle;

@Service
public class InCloudformationFilter implements PhysicalDeletionFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(InCloudformationFilter.class);
    private final AmazonCloudFormation cloudFormation;

    @Autowired
    public InCloudformationFilter(AmazonCloudFormation cloudFormation) {
        this.cloudFormation = cloudFormation;
    }

    @Override
    public boolean shouldDelete(String physicalId) {
        final AtomicBoolean resourceInCloudformationStack = new AtomicBoolean();
        performWithThrottle(() -> {
            final boolean inCf = isInCloudformation(physicalId);
            resourceInCloudformationStack.set(inCf);
            LOGGER.debug("is {} in cloudformation? {}", physicalId, inCf);
        });
        return !resourceInCloudformationStack.get();
    }

    private boolean isInCloudformation(String physicalId) {
        try {
            cloudFormation.describeStackResources(new DescribeStackResourcesRequest().withPhysicalResourceId(physicalId));
            return true;
        } catch (AmazonCloudFormationException e) {
            if (e.getMessage().contains("Stack") && e.getMessage().contains("does not exist")) {
                return false;
            } else {
                throw e;
            }
        }
    }

}

