/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2015
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.limemojito.aws.cleaner.integration;

import com.limemojito.aws.cleaner.Main;
import org.junit.Test;

public class LocalCleanTest {

    @Test
    public void shouldCleanLocalOk() throws Exception {
        Main.main("LOCAL");
    }
}
