/*
 * Copyright 2020 Lime Mojito Pty Ltd
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

import com.amazonaws.regions.Regions;
import com.limemojito.aws.cleaner.config.CleanerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private final List<ResourceCleaner> resourceCleaners;

    @Autowired
    public Main(List<ResourceCleaner> resourceCleaners, Regions region) {
        LOGGER.info("Performing clean in region {} using {} cleaners", region, resourceCleaners.size());
        this.resourceCleaners = resourceCleaners;
    }

    public static void main(String... args) {
        if (args.length == 0) {
            LOGGER.info("\n\nUsage: java -D.... -jar cleaner.jar --commit"
                                + "\n\t-Dcleaner.region=<region> to override AWS region."
                                + "\n\t-Dcleaner.cloudformation.whitelist=<comma,separated,stack,name,prefixes> to keep named stacks."
                                + "\n\t-Dcleaner.role.arn=<roleArn> role to assume to access AWS."
                                + "\n\t-Dcleaner.mfa.arn=<mfaArn> device to use with Multi Factor Authentication (prompts for code)."
                                + "\n"
                                + "\n\t --commit to commit changes."
                                + "\n\n");
        }
        boolean commit = Arrays.asList(args).contains("--commit");
        if (!commit) {
            LOGGER.warn("performing dry run.");
        }
        AbstractApplicationContext context = new AnnotationConfigApplicationContext(CleanerConfig.class);
        context.registerShutdownHook();
        Main main = context.getBean(Main.class);
        main.setCommit(commit);
        main.cleanEnvironment();
    }

    public void setCommit(boolean commit) {
        if (commit) {
            LOGGER.warn("Committing Changes");
        }
        resourceCleaners.forEach(o -> o.setCommit(commit));
    }

    public void cleanEnvironment() {
        LOGGER.info("Cleaning AWS resources");
        for (ResourceCleaner resourceCleaner : resourceCleaners) {
            LOGGER.info("Processing {}", resourceCleaner.getClass().getSimpleName());
            resourceCleaner.clean();
        }
        LOGGER.debug("Resource cleaning completed");
    }
}
