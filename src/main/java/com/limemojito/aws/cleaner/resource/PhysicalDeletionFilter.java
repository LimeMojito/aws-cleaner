/*
 * Copyright (c) Lime Mojito Pty Ltd 2011-2018
 *
 * Except as otherwise permitted by the Copyright Act 1967 (Cth) (as amended from time to time) and/or any other
 * applicable copyright legislation, the material may not be reproduced in any format and in any way whatsoever
 * without the prior written consent of the copyright owner.
 */

package com.limemojito.aws.cleaner.resource;

public interface PhysicalDeletionFilter {
    boolean shouldDelete(String physicalId);
}