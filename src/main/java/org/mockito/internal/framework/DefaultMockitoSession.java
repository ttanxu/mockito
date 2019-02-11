/*
 * Copyright (c) 2018 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.framework;

import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoFramework;
import org.mockito.MockitoSession;
import org.mockito.exceptions.misusing.RedundantListenerException;
import org.mockito.internal.configuration.plugins.Plugins;
import org.mockito.internal.exceptions.Reporter;
import org.mockito.internal.junit.MockTracker;
import org.mockito.internal.junit.TestFinishedEvent;
import org.mockito.internal.junit.UniversalTestListener;
import org.mockito.plugins.MockitoLogger;
import org.mockito.quality.Strictness;

import java.util.List;

public class DefaultMockitoSession implements MockitoSession {

    private final String name;
    private final UniversalTestListener listener;
    private final MockTracker mockTracker;

    public DefaultMockitoSession(List<Object> testClassInstances, String name, Strictness strictness, MockitoLogger logger,
                                 boolean trackAndCleanUpMocks) {
        this.name = name;
        listener = new UniversalTestListener(strictness, logger);
        MockitoFramework framework = Mockito.framework();
        try {
            //So that the listener can capture mock creation events
            framework.addListener(listener);
        } catch (RedundantListenerException e) {
            Reporter.unfinishedMockingSession();
        }
        try {
            for (Object testClassInstance : testClassInstances) {
                MockitoAnnotations.initMocks(testClassInstance);
            }
        } catch (RuntimeException e) {
            //clean up in case 'initMocks' fails
            listener.setListenerDirty();
            throw e;
        }

        if (trackAndCleanUpMocks && (framework instanceof DefaultMockitoFramework)) {
            mockTracker = new MockTracker(Plugins.getMockMaker());
            try {
                ((DefaultMockitoFramework) framework).addGlobalListener(mockTracker);
            } catch (RedundantListenerException e) {
                listener.setListenerDirty();
                Reporter.multipleTrackingMockSession();
            }
        } else {
            mockTracker = null;
        }
    }

    @Override
    public void setStrictness(Strictness strictness) {
        listener.setStrictness(strictness);
    }

    @Override
    public void finishMocking() {
        finishMocking(null);
    }

    @Override
    public void finishMocking(final Throwable failure) {
        //Cleaning up the state, we no longer need the listener hooked up
        //The listener implements MockCreationListener and at this point
        //we no longer need to listen on mock creation events. We are wrapping up the session
        MockitoFramework framework = Mockito.framework();
        framework.removeListener(listener);
        if (mockTracker != null) {
            ((DefaultMockitoFramework) framework).removeGlobalListener(mockTracker);
        }

        //Emit test finished event so that validation such as strict stubbing can take place
        listener.testFinished(new TestFinishedEvent() {
            @Override
            public Throwable getFailure() {
                return failure;
            }
            @Override
            public String getTestName() {
                return name;
            }
        });

        //Validate only when there is no test failure to avoid reporting multiple problems
        if (failure == null) {
            //Finally, validate user's misuse of Mockito framework.
            Mockito.validateMockitoUsage();
        }

        if (mockTracker != null) {
            mockTracker.testFinished();
        }
    }
}
