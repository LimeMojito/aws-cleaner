/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2015
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.limemojito.aws.cleaner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MainTest {

    @Mock
    private ResourceCleaner cleaner;

    @Mock
    private UserChecker userChecker;

    @Test
    public void shouldCallCleanEnvironment() throws Exception {
        when(userChecker.isOK()).thenReturn(true);

        Main main = new Main(userChecker, cleaner);
        main.cleanEnvironment();
    }
}
