/*
 * Copyright 2018 Lime Mojito Pty Ltd
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

import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.model.CacheCluster;
import com.amazonaws.services.elasticache.model.DeleteCacheClusterRequest;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
public class ElasticacheResourceCleaner extends PhysicalResourceCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticacheResourceCleaner.class);
    private final AmazonElastiCache client;

    @Autowired
    public ElasticacheResourceCleaner(AmazonElastiCache client) {
        super();
        this.client = client;
    }

    @Override
    protected List<String> getPhysicalResourceIds() {
        DescribeCacheClustersResult results = client.describeCacheClusters();
        final List<CacheCluster> cacheClusters = results.getCacheClusters();
        LOGGER.debug("Found {} clusters", cacheClusters.size());
        return cacheClusters.stream()
                            .filter(cacheCluster -> ("available".equals(cacheCluster.getCacheClusterStatus())))
                            .map(CacheCluster::getCacheClusterId)
                            .collect(toList());
    }

    @Override
    protected void performDelete(String physicalId) {
        LOGGER.info("Deleting cache {}", physicalId);
        client.deleteCacheCluster(new DeleteCacheClusterRequest(physicalId));
    }
}
