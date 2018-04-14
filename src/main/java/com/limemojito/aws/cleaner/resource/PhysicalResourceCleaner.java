/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.limemojito.aws.cleaner.resource;

import com.limemojito.aws.cleaner.ResourceCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public abstract class PhysicalResourceCleaner implements ResourceCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(PhysicalResourceCleaner.class);
    private PhysicalDeletionFilter filter;

    public PhysicalResourceCleaner() {
        filter = physicalId -> true;
    }

    @Autowired
    public void setFilter(PhysicalDeletionFilter filter) {
        this.filter = filter;
    }

    @Override
    public void clean() {
        final List<String> physicalResourceIdList = getPhysicalResourceIds();
        if (!physicalResourceIdList.isEmpty()) {
            LOGGER.debug("Deleting {} resources", physicalResourceIdList.size());
            physicalResourceIdList.stream()
                                  .filter(p -> filter.shouldDelete(p))
                                  .forEach((physicalId) -> Throttle.performWithThrottle(() -> performDelete(physicalId)));
        }
    }

    protected abstract List<String> getPhysicalResourceIds();

    protected abstract void performDelete(String physicalId);
}
