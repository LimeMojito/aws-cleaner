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
