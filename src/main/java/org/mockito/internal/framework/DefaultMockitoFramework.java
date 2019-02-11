/*
 * Copyright (c) 2016 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.framework;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.mockito.MockitoFramework;
import org.mockito.internal.configuration.plugins.Plugins;
import org.mockito.internal.invocation.DefaultInvocationFactory;
import org.mockito.internal.listeners.MockitoListenerUtil;
import org.mockito.internal.util.Checks;
import org.mockito.invocation.InvocationFactory;
import org.mockito.listeners.MockCreationListener;
import org.mockito.listeners.MockitoListener;
import org.mockito.listeners.VerificationListener;
import org.mockito.mock.MockCreationSettings;
import org.mockito.plugins.MockitoPlugins;

import static org.mockito.internal.progress.ThreadSafeMockingProgress.mockingProgress;

public class DefaultMockitoFramework implements MockitoFramework {

    private final Set<MockitoListener> listeners = new LinkedHashSet<MockitoListener>();

    public MockitoFramework addListener(MockitoListener listener) {
        Checks.checkNotNull(listener, "listener");
        mockingProgress().addListener(listener);
        return this;
    }

    public MockitoFramework removeListener(MockitoListener listener) {
        Checks.checkNotNull(listener, "listener");
        mockingProgress().removeListener(listener);
        return this;
    }

    /**
     * Adds a listener to Mockito.
     *
     * <p>It's different from {@link #addListener(MockitoListener)} in the way that this listener will receive
     * notifications from all threads.
     *
     * <p>It also throws {@link org.mockito.exceptions.misusing.RedundantListenerException} if caller tries to add a
     * listener of the same type as an added listener.
     *
     * <p>The listener will be called in the thread where the notification is sent. It's the responsibility of listeners
     * to ensure thread-safety.
     *
     * @param listener to add to Mockito
     * @return this instance of mockito framework (fluent builder pattern)
     */
    public MockitoFramework addGlobalListener(MockitoListener listener) {
        Checks.checkNotNull(listener, "listener");
        synchronized (listeners) {
            MockitoListenerUtil.addListener(listener, listeners);
        }
        return this;
    }

    public MockitoFramework removeGlobalListener(MockitoListener listener) {
        Checks.checkNotNull(listener, "listener");
        synchronized (listeners) {
            listeners.remove(listener);
        }
        return this;
    }

    public Set<VerificationListener> verificationListeners() {
        Set<VerificationListener> verificationListeners = mockingProgress().verificationListeners();
        synchronized (listeners) {
            for (MockitoListener listener : listeners) {
                if (listener instanceof VerificationListener) {
                    verificationListeners.add((VerificationListener) listener);
                }
            }
        }
        return verificationListeners;
    }

    public void mockingStarted(Object mock, MockCreationSettings settings) {
        mockingProgress().mockingStarted(mock, settings);
        List<MockCreationListener> creationListeners = new ArrayList<MockCreationListener>();
        synchronized (listeners) {
            for (MockitoListener listener : listeners) {
                if (listener instanceof MockCreationListener) {
                    creationListeners.add((MockCreationListener) listener);
                }
            }
        }
        for (MockCreationListener listener : creationListeners) {
            listener.onMockCreated(mock, settings);
        }
    }

    @Override
    public MockitoPlugins getPlugins() {
        return Plugins.getPlugins();
    }

    @Override
    public InvocationFactory getInvocationFactory() {
        return new DefaultInvocationFactory();
    }
}
