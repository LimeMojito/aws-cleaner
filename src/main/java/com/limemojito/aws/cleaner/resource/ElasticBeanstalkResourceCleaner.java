/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2015
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.limemojito.aws.cleaner.resource;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ElasticBeanstalkResourceCleaner extends BaseAwsResourceCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticBeanstalkResourceCleaner.class);
    private final AWSElasticBeanstalkClient client;

    @Autowired
    public ElasticBeanstalkResourceCleaner(AWSElasticBeanstalkClient client) {
        this.client = client;
    }

    @Override
    public String getName() {
        return "Elastic Beanstalk Cleaner";
    }

    @Override
    public void clean(String environment) {
        LOGGER.debug("Requesting environments");
        final DescribeEnvironmentsResult result = client.describeEnvironments();
        final List<EnvironmentDescription> environments = result.getEnvironments();
        LOGGER.debug("{} environments found", environments.size());
        environments
                .stream()
                .filter(environmentDescription -> {
                    final String envPrefix = ALL_ENVIRONMENTS.equals(environment) ? "": environment;
                    return (environmentDescription.getEnvironmentName()
                                                  .startsWith(envPrefix) && environmentDescription.getStatus()
                                                                                                     .equalsIgnoreCase("Ready"));
                })
                .forEach(environmentDescription -> performWithThrottle(() -> terminateEnvironment(environmentDescription)));
    }

    private void terminateEnvironment(EnvironmentDescription environmentDescription) {
        final String environmentName = environmentDescription.getEnvironmentName();
        final TerminateEnvironmentRequest terminateEnvironmentRequest = new TerminateEnvironmentRequest()
                .withEnvironmentName(environmentName);
        LOGGER.info("Terminating environment {}", environmentName);
        client.terminateEnvironment(terminateEnvironmentRequest);
    }
}
