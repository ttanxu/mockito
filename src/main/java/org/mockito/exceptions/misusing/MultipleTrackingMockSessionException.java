/*
 * Copyright (c) 2019 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */

package org.mockito.exceptions.misusing;

import org.mockito.MockitoSession;
import org.mockito.exceptions.base.MockitoException;

/**
 * Reports the misuse where user tries to open more than one {@link MockitoSession} that tracks and cleans up mocks.
 * User needs to finish previous session that tracks and cleans up mocks before trying to open a new one. Note this
 * doesn't prevent user from opening sessions that doesn't track or clean up mocks.
 *
 * @since 2.24.4
 */
public class MultipleTrackingMockSessionException extends MockitoException {
    public MultipleTrackingMockSessionException(String message) {
        super(message);
    }
}
