package com.limemojito.aws.cleaner;/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2015
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

import org.junit.Test;

public class MainTest {

    @Test
    public void shouldCallCleanEnvironment() throws Exception {
        Main main = new Main();
        main.cleanEnvironment("LOCAL");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionIfNoEnvironmentSet() throws Exception {
        Main.main(new String[0]);
    }
}
