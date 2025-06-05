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

package com.limemojito.aws.cleaner;

import com.limemojito.aws.cleaner.resource.PhysicalDeletionFilter;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Interface for AWS resource cleaners.
 * Implementations of this interface are responsible for cleaning specific types of AWS resources.
 * Each cleaner can be configured to run in dry-run mode or to actually perform deletions.
 */
public interface ResourceCleaner {
    /**
     * Sets the filter to determine which resources should be deleted.
     *
     * @param filter The filter that determines which resources should be physically deleted
     */
    @Autowired
    void setFilter(PhysicalDeletionFilter filter);

    /**
     * Executes the cleaning process for the specific AWS resource type.
     * The actual behavior depends on whether commit mode is enabled.
     */
    void clean();

    /**
     * Sets whether the cleaner should actually perform deletions or just simulate them.
     *
     * @param commit true to perform actual deletions, false for dry-run mode
     */
    void setCommit(boolean commit);
}
