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

import static java.util.stream.Collectors.toList;

@Service
public class ElasticacheResourceCleaner extends PhysicalResourceCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticacheResourceCleaner.class);
    private final AmazonElastiCache client;

    @Autowired
    public ElasticacheResourceCleaner(AmazonElastiCache client) {
        this.client = client;
    }

    @Override
    protected List<String> getPhysicalResourceIds() {
        LOGGER.info("Cleaning elasticache");
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
        client.deleteCacheCluster(new DeleteCacheClusterRequest(physicalId));
    }
}
