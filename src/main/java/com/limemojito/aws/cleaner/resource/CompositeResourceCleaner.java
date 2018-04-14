/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.limemojito.aws.cleaner.resource;

import com.limemojito.aws.cleaner.ResourceCleaner;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;

public abstract class CompositeResourceCleaner implements ResourceCleaner {
    private final List<ResourceCleaner> components;

    public CompositeResourceCleaner(ResourceCleaner... cleaners) {
        this.components = Arrays.asList(cleaners);
    }

    @Autowired
    public void setFilter(PhysicalDeletionFilter filter) {
        this.components.forEach(o->o.setFilter(filter));
    }

    @Override
    public void clean() {
        this.components.forEach(ResourceCleaner::clean);
    }
}
