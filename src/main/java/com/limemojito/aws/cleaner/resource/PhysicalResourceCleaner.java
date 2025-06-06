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

import com.limemojito.aws.cleaner.ResourceCleaner;
import com.limemojito.aws.cleaner.filter.PhysicalDeletionFilter;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Abstract base class for resource cleaners that handle physical AWS resources.
 * This class provides common functionality for identifying and deleting physical AWS resources.
 * Subclasses need to implement methods to get resource IDs and perform the actual deletion.
 */
public abstract class PhysicalResourceCleaner implements ResourceCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(PhysicalResourceCleaner.class);
    @Getter
    private PhysicalDeletionFilter filter;
    @Getter
    private boolean commit;

    /**
     * {@inheritDoc}
     * Sets the filter used to determine which resources should be deleted.
     */
    @Override
    @Autowired
    public void setFilter(PhysicalDeletionFilter filter) {
        this.filter = filter;
    }

    /**
     * {@inheritDoc}
     * Sets whether the cleaner should actually perform deletions or just simulate them.
     */
    @Override
    public void setCommit(boolean commit) {
        this.commit = commit;
    }

    /**
     * {@inheritDoc}
     * Implements the cleaning process by retrieving all physical resource IDs,
     * filtering them based on the configured deletion filter, and then either
     * logging what would be deleted (in dry-run mode) or actually performing
     * the deletion with throttling.
     */
    @Override
    public void clean() {
        final List<String> physicalResourceIdList = getPhysicalResourceIds();
        if (!physicalResourceIdList.isEmpty()) {
            physicalResourceIdList.stream()
                                  .filter(p -> filter.shouldDelete(p))
                                  .forEach((physicalId) -> {
                                      if (!commit) {
                                          LOGGER.info("Would delete {}", physicalId);
                                      } else {
                                          Throttle.performWithThrottle(() -> performDelete(physicalId));
                                      }
                                  });
        }
    }

    /**
     * Retrieves the list of physical resource IDs that are candidates for deletion.
     *
     * @return A list of physical resource IDs
     */
    protected abstract List<String> getPhysicalResourceIds();

    /**
     * Performs the actual deletion of a resource identified by its physical ID.
     * This method is called only when commit mode is enabled.
     *
     * @param physicalId The physical ID of the resource to delete
     */
    protected abstract void performDelete(String physicalId);
}
