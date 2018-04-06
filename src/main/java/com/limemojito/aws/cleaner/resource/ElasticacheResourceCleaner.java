/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2016
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
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

@Service
public class ElasticacheResourceCleaner extends BaseAwsResourceCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticacheResourceCleaner.class);
    private final AmazonElastiCache client;

    @Autowired
    public ElasticacheResourceCleaner(AmazonElastiCache client) {
        this.client = client;
    }

    @Override
    public String getName() {
        return "Elasticache Cleaner";
    }

    @Override
    public void clean() {
        LOGGER.info("Cleaning elasticache");
        DescribeCacheClustersResult results = client.describeCacheClusters();
        final List<CacheCluster> cacheClusters = results.getCacheClusters();
        LOGGER.debug("Found {} clusters", cacheClusters.size());
        cacheClusters.stream()
                     .filter(cacheCluster -> ("available".equals(cacheCluster.getCacheClusterStatus())))
                     .forEach(cacheCluster1 -> performWithThrottle(() -> {
                         final String cacheClusterId = cacheCluster1.getCacheClusterId();
                         LOGGER.info("Removing {} cache cluster", cacheClusterId);
                         client.deleteCacheCluster(new DeleteCacheClusterRequest(cacheClusterId));
                     }));

    }
}
