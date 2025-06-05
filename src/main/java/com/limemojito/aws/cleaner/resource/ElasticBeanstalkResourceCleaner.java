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

import com.amazonaws.SdkClientException;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Resource cleaner for AWS Elastic Beanstalk environments.
 * This cleaner identifies and terminates Elastic Beanstalk environments that are in "Ready" state.
 */
@Service
@Slf4j
public class ElasticBeanstalkResourceCleaner extends PhysicalResourceCleaner {
    private final AWSElasticBeanstalk client;

    /**
     * Constructs a new ElasticBeanstalkResourceCleaner.
     *
     * @param client The AWS Elastic Beanstalk client
     */
    @Autowired
    public ElasticBeanstalkResourceCleaner(AWSElasticBeanstalk client) {
        super();
        this.client = client;
    }

    /**
     * {@inheritDoc}
     * Retrieves a list of all Elastic Beanstalk environment names that are in "Ready" state.
     * If communication with the Elastic Beanstalk service fails, returns an empty list.
     *
     * @return A list of Elastic Beanstalk environment names
     */
    @Override
    protected List<String> getPhysicalResourceIds() {
        try {
            final DescribeEnvironmentsResult result = client.describeEnvironments();
            final List<EnvironmentDescription> environments = result.getEnvironments();
            log.debug("{} environments found", environments.size());
            return environments.stream()
                               .filter(environmentDescription -> environmentDescription.getStatus()
                                                                                       .equalsIgnoreCase("Ready"))
                               .map(EnvironmentDescription::getEnvironmentName)
                               .collect(Collectors.toList());
        } catch (SdkClientException e) {
            log.warn("Could not communicate with elastic beanstalk: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * {@inheritDoc}
     * Terminates an Elastic Beanstalk environment identified by its name.
     *
     * @param physicalId The name of the Elastic Beanstalk environment to terminate
     */
    @Override
    protected void performDelete(String physicalId) {
        log.info("Terminating environment {}", physicalId);
        client.terminateEnvironment(new TerminateEnvironmentRequest().withEnvironmentName(physicalId));
    }
}
