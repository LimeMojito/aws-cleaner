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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.elasticache.ElastiCacheClient;
import software.amazon.awssdk.services.elasticache.model.CacheCluster;

import java.util.List;

/**
 * Resource cleaner for AWS ElastiCache clusters.
 * This cleaner identifies and deletes ElastiCache clusters that are in "available" state.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ElasticacheResourceCleaner extends PhysicalResourceCleaner {
    private final ElastiCacheClient client;

    /**
     * {@inheritDoc}
     * Retrieves a list of all ElastiCache cluster IDs that are in "available" state.
     *
     * @return A list of ElastiCache cluster IDs
     */
    @Override
    protected List<String> getPhysicalResourceIds() {
        log.debug("Getting all physical resource ids");
        return client.describeCacheClustersPaginator()
                     .stream()
                     .flatMap(p -> p.cacheClusters().stream())
                     .filter(cacheCluster -> ("available".equals(cacheCluster.cacheClusterStatus())))
                     .map(CacheCluster::cacheClusterId)
                     .toList();
    }

    /**
     * {@inheritDoc}
     * Deletes an ElastiCache cluster identified by its ID.
     *
     * @param physicalId The ID of the ElastiCache cluster to delete
     */
    @Override
    protected void performDelete(String physicalId) {
        log.info("Deleting cache {}", physicalId);
        client.deleteCacheCluster(r -> r.cacheClusterId(physicalId));
    }
}
