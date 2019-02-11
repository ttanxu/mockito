/*
 * Copyright (c) 2019 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */

package org.mockito.internal.junit;

import org.junit.Test;
import org.mockito.internal.creation.MockSettingsImpl;
import org.mockito.plugins.InlineMockMaker;
import org.mockito.plugins.MockMaker;
import org.mockitoutil.TestBase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MockTrackerTest extends TestBase {

    @Test
    public void works_with_mock_maker_without_need_for_cleanup() {
        MockMaker maker = mock(MockMaker.class);

        MockTracker tracker = new MockTracker(maker);

        Object mock1 = new Object();
        tracker.onMockCreated(mock1, new MockSettingsImpl());
        Object mock2 = new Object();
        tracker.onMockCreated(mock2, new MockSettingsImpl());

        tracker.testFinished();
    }

    @Test
    public void clear_tracked_mocks_when_finish() {
        InlineMockMaker maker = mock(InlineMockMaker.class);

        MockTracker tracker = new MockTracker(maker);

        Object mock1 = new Object();
        tracker.onMockCreated(mock1, new MockSettingsImpl());
        Object mock2 = new Object();
        tracker.onMockCreated(mock2, new MockSettingsImpl());

        tracker.testFinished();

        verify(maker).cleanUpMock(mock1);
        verify(maker).cleanUpMock(mock2);
    }

    @Test
    public void set_listener_dirty() {
        MockTracker tracker = new MockTracker(mock(MockMaker.class));

        assertFalse(tracker.isListenerDirty());
        tracker.setListenerDirty();
        assertTrue(tracker.isListenerDirty());
    }
}
