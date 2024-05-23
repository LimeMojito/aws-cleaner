/*
 * Copyright 2011-2024 Lime Mojito Pty Ltd
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

import com.limemojito.aws.cleaner.ResourceCleaner;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public abstract class PhysicalResourceCleaner implements ResourceCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(PhysicalResourceCleaner.class);
    @Getter
    private PhysicalDeletionFilter filter;
    @Getter
    private boolean commit;

    public PhysicalResourceCleaner() {
        filter = physicalId -> true;
    }

    @Override
    @Autowired
    public void setFilter(PhysicalDeletionFilter filter) {
        this.filter = filter;
    }

    @Override
    public void setCommit(boolean commit) {
        this.commit = commit;
    }

    @Override
    public void clean() {
        final List<String> physicalResourceIdList = getPhysicalResourceIds();
        if (!physicalResourceIdList.isEmpty()) {
            physicalResourceIdList.stream()
                                  .filter(p -> filter.shouldDelete(p))
                                  .forEach((physicalId) -> {
                                      if (!commit) {
                                          LOGGER.info("Would delete {}", physicalId);
                                      } else {
                                          Throttle.performWithThrottle(() -> performDelete(physicalId));
                                      }
                                  });
        }
    }

    protected abstract List<String> getPhysicalResourceIds();

    protected abstract void performDelete(String physicalId);
}
