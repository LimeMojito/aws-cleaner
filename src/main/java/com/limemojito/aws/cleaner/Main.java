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

package com.limemojito.aws.cleaner;

import com.limemojito.aws.cleaner.config.CleanerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private final ResourceCleaner[] resourceCleaners;
    private final UserChecker userChecker;

    @Autowired
    public Main(UserChecker userChecker, ResourceCleaner... resourceCleaners) {
        this.userChecker = userChecker;
        this.resourceCleaners = resourceCleaners;
    }

    public static void main(String... args) {
        LOGGER.info("Initialising");

        AbstractApplicationContext context = new AnnotationConfigApplicationContext(CleanerConfig.class);
        context.registerShutdownHook();
        Main main = context.getBean(Main.class);
        main.cleanEnvironment();
    }

    public void cleanEnvironment() {
        if (!userChecker.isOK()) {
            throw new IllegalStateException("Check .aws/credentials as user is not allowed for cleaning");
        }
        LOGGER.info("Cleaning AWS resources");
        for (ResourceCleaner resourceCleaner : resourceCleaners) {
            LOGGER.info("Processing {}", resourceCleaner.getClass().getSimpleName());
            resourceCleaner.clean();
        }
        LOGGER.debug("Resource cleaning completed");
    }
}
