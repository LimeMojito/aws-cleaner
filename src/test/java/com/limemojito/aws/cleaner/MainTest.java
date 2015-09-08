package com.limemojito.aws.cleaner;/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2015
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MainTest {

    @Mock
    private ResourceCleaner cleaner;

    @Test
    public void shouldCallCleanEnvironment() throws Exception {
        when(cleaner.getName()).thenReturn("Test");

        Main main = new Main(cleaner);
        main.cleanEnvironment("LOCAL");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionIfNoEnvironmentSet() throws Exception {
        Main.main();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionOnIllegalEnvironment() throws Exception {
        Main.main("PROD");
    }
}
