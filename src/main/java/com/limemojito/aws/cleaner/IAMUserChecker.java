/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2016
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.limemojito.aws.cleaner;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.GetUserResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class IAMUserChecker implements UserChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(IAMUserChecker.class);
    private final AmazonIdentityManagement iam;

    public IAMUserChecker(AmazonIdentityManagement iam) {
        this.iam = iam;
    }

    @Override
    public boolean isOK() {
        final GetUserResult user = iam.getUser();
        final String userName = user.getUser().getUserName();
        LOGGER.info("Performing clean as {}", userName);
        return StringUtils.startsWith(userName, "np");
    }

}
