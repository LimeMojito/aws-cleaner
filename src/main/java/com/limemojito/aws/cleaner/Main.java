/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2015
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.limemojito.aws.cleaner;

import com.limemojito.aws.cleaner.config.CleanerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.stereotype.Service;

import static java.lang.String.format;

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
        if (args.length < 1) {
            throw new IllegalStateException("Arguments to main need to include the environment");
        }
        LOGGER.info("Initialising");
        final String environment = args[0];

        AbstractApplicationContext context = new AnnotationConfigApplicationContext(CleanerConfig.class);
        context.registerShutdownHook();
        Main main = context.getBean(Main.class);
        main.cleanEnvironment(environment);
    }

    public void cleanEnvironment(String environment) {
        verifyEnvironment(environment);
        if (!userChecker.isOK()) {
            throw new IllegalStateException("Check .aws/credentials as user is not allowed for cleaning");
        }
        LOGGER.info("Cleaning AWS resources in {}", environment);
        for (ResourceCleaner resourceCleaner : resourceCleaners) {
            LOGGER.info("Processing {}", resourceCleaner.getName());
            resourceCleaner.clean(environment);
        }
        LOGGER.debug("Resource cleaning completed");
    }

    private void verifyEnvironment(String environment) {
        if (!(ResourceCleaner.LOCAL_ENVIRONMENT.equals(environment)
                || ResourceCleaner.DEV_ENVIRONMENT.equals(environment))) {
            throw new IllegalStateException(format("Environment %s is not supported", environment));
        }
    }
}
