/*
 * Copyright (c) 2019 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */

package org.mockito.internal.listeners;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.mockito.internal.exceptions.Reporter;
import org.mockito.listeners.MockitoListener;

public class MockitoListenerUtil {
    public static void addListener(MockitoListener listener, Set<MockitoListener> listeners) {
        List<MockitoListener> delete = new LinkedList<MockitoListener>();
        for (MockitoListener existing : listeners) {
            if (existing.getClass().equals(listener.getClass())) {
                if (existing instanceof AutoCleanableListener && ((AutoCleanableListener) existing).isListenerDirty()) {
                    //dirty listener means that there was an exception even before the test started
                    //if we fail here with redundant mockito listener exception there will be multiple failures causing confusion
                    //so we simply remove the existing listener and move on
                    delete.add(existing);
                } else {
                    Reporter.redundantMockitoListener(listener.getClass().getSimpleName());
                }
            }
        }
        //delete dirty listeners so they don't occupy state/memory and don't receive notifications
        for (MockitoListener toDelete : delete) {
            listeners.remove(toDelete);
        }
        listeners.add(listener);
    }
}
