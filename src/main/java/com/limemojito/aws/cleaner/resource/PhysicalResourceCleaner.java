/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.limemojito.aws.cleaner.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class PhysicalResourceCleaner extends BaseAwsResourceCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(PhysicalResourceCleaner.class);

    @Override
    public void clean() {
        final List<String> physicalResourceIdList = getPhysicalResourceIds();
        LOGGER.debug("Deleting {} resources", physicalResourceIdList.size());
        physicalResourceIdList.forEach((physicalId) -> performWithThrottle(() -> performDelete(physicalId)));
    }

    protected abstract List<String> getPhysicalResourceIds();

    protected abstract void performDelete(String physicalId);
}
