/*
 * Copyright (c) 2019 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */

package org.mockito.internal.junit;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import org.mockito.internal.listeners.AutoCleanableListener;
import org.mockito.listeners.MockCreationListener;
import org.mockito.mock.MockCreationSettings;
import org.mockito.plugins.InlineMockMaker;
import org.mockito.plugins.MockMaker;

public class MockTracker implements MockCreationListener, AutoCleanableListener {

    private final List<WeakReference<Object>> mocks = new ArrayList<WeakReference<Object>>();
    private final InlineMockMaker mockMaker;
    private boolean listenerDirty;

    public MockTracker(MockMaker mockMaker) {
        if (mockMaker instanceof InlineMockMaker) {
            this.mockMaker = (InlineMockMaker) mockMaker;
        } else {
            this.mockMaker = null;
        }
    }

    @Override
    public void onMockCreated(Object mock, MockCreationSettings settings) {
        if (mockMaker == null) {
            return;
        }

        synchronized (mocks) {
            mocks.add(new WeakReference<Object>(mock));
        }
    }

    public void testFinished() {
        if (mockMaker == null) {
            return;
        }

        WeakReference[] localMocks;
        synchronized (mocks) {
            localMocks = mocks.toArray(new WeakReference[0]);
        }

        for (WeakReference weakMock : localMocks) {
            Object mock = weakMock.get();
            if (mock != null) {
                mockMaker.cleanUpMock(mock);
            }
        }
    }

    @Override
    public boolean isListenerDirty() {
        return listenerDirty;
    }

    public void setListenerDirty() {
        listenerDirty = true;
    }
}
