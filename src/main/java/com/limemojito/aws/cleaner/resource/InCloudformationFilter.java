/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
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

