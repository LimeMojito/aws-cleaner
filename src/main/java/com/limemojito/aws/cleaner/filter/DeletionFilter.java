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

/**
 * Interface for filtering AWS resources to determine which ones should be deleted.
 * Implementations of this interface provide the logic to decide whether a specific
 * AWS resource should be deleted based on its physical ID.
 */
@FunctionalInterface
public interface DeletionFilter {
    /**
     * Determines whether a resource with the given physical ID should be deleted.
     *
     * @param physicalId The physical ID of the AWS resource to evaluate
     * @return true if the resource should be deleted, false otherwise
     */
    boolean shouldDelete(String physicalId);
}
