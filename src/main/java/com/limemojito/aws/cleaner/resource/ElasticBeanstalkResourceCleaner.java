/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2015
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.limemojito.aws.cleaner.resource;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ElasticBeanstalkResourceCleaner extends PhysicalResourceCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticBeanstalkResourceCleaner.class);
    private final AWSElasticBeanstalk client;

    @Autowired
    public ElasticBeanstalkResourceCleaner(AWSElasticBeanstalk client) {
        this.client = client;
    }

    @Override
    protected List<String> getPhysicalResourceIds() {
        final DescribeEnvironmentsResult result = client.describeEnvironments();
        final List<EnvironmentDescription> environments = result.getEnvironments();
        LOGGER.debug("{} environments found", environments.size());
        return environments.stream()
                           .filter(environmentDescription -> environmentDescription.getStatus().equalsIgnoreCase("Ready"))
                           .map(EnvironmentDescription::getEnvironmentName)
                           .collect(Collectors.toList());
    }

    @Override
    protected void performDelete(String physicalId) {
        LOGGER.info("Terminating environment {}", physicalId);
        client.terminateEnvironment(new TerminateEnvironmentRequest().withEnvironmentName(physicalId));
    }
}
