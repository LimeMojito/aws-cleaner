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
        super();
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
