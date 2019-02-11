/*
 * Copyright (c) 2018 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.session;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoSession;
import org.mockito.StateMaster;
import org.mockito.exceptions.misusing.MultipleTrackingMockSessionException;
import org.mockito.exceptions.misusing.UnfinishedMockingSessionException;
import org.mockito.quality.Strictness;
import org.mockito.session.MockitoSessionLogger;
import org.mockitoutil.ThrowableAssert;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.WARN;

public class DefaultMockitoSessionBuilderTest {

    @After public void after() {
        new StateMaster().clearMockitoListeners();
    }

    @Test public void creates_sessions() {
        //no configuration is legal
        new DefaultMockitoSessionBuilder().startMocking().finishMocking();

        //passing null to configuration is legal, default value will be used
        new DefaultMockitoSessionBuilder().initMocks((Object) null).startMocking().finishMocking();
        new DefaultMockitoSessionBuilder().initMocks((Object[]) null).startMocking().finishMocking();
        new DefaultMockitoSessionBuilder().initMocks(null, null).strictness(null).startMocking().finishMocking();
        new DefaultMockitoSessionBuilder().strictness(null).startMocking().finishMocking();

        //happy path
        new DefaultMockitoSessionBuilder().initMocks(this).startMocking().finishMocking();
        new DefaultMockitoSessionBuilder().initMocks(new Object()).startMocking().finishMocking();
        new DefaultMockitoSessionBuilder().strictness(Strictness.LENIENT).startMocking().finishMocking();
        new DefaultMockitoSessionBuilder().trackAndCleanUpMocks().startMocking().finishMocking();
    }

    @Test public void creates_sessions_for_multiple_test_class_instances_for_repeated_calls() {
        TestClass testClass = new TestClass();
        TestClass.NestedTestClass nestedTestClass = testClass.new NestedTestClass();

        new DefaultMockitoSessionBuilder().initMocks(testClass).initMocks(nestedTestClass).startMocking().finishMocking();

        assertNotNull(testClass.set);
        assertNotNull(nestedTestClass.list);
    }

    @Test public void creates_sessions_for_multiple_test_class_instances_for_varargs_call() {
        TestClass testClass = new TestClass();
        TestClass.NestedTestClass nestedTestClass = testClass.new NestedTestClass();

        new DefaultMockitoSessionBuilder().initMocks(testClass, nestedTestClass).startMocking().finishMocking();

        assertNotNull(testClass.set);
        assertNotNull(nestedTestClass.list);
    }

    @Test public void uses_logger_and_strictness() {
        TestClass testClass = new TestClass();

        final List<String> hints = new ArrayList<String>();
        MockitoSession session = new DefaultMockitoSessionBuilder()
            .initMocks(testClass)
            .strictness(WARN)
            .logger(new MockitoSessionLogger() {
                @Override
                public void log(String hint) {
                    hints.add(hint);
                }
            })
            .startMocking();

        when(testClass.set.add(1)).thenReturn(true);

        session.finishMocking();

        assertFalse(hints.isEmpty());
    }

    @Test public void requires_finish_mocking() {
        new DefaultMockitoSessionBuilder().startMocking();

        ThrowableAssert.assertThat(new Runnable() {
            public void run() {
                new DefaultMockitoSessionBuilder().startMocking();
            }
        }).throwsException(UnfinishedMockingSessionException.class);
    }

    @Test public void auto_cleans_dirty_listeners() {
        new DefaultMockitoSessionBuilder().startMocking();

        ThrowableAssert.assertThat(new Runnable() {
            public void run() {
                new DefaultMockitoSessionBuilder().startMocking();
            }
        }).throwsException(UnfinishedMockingSessionException.class);
    }

    @Test public void cant_open_multiple_tracking_mocks_sessions() throws Exception {
        MockitoSession session = new DefaultMockitoSessionBuilder().trackAndCleanUpMocks().startMocking();

        try {
            FutureTask<Void> task = new FutureTask<Void>(new Callable<Void>() {
                @Override
                public Void call() {
                    ThrowableAssert.assertThat(new Runnable() {
                        public void run() {
                            new DefaultMockitoSessionBuilder().trackAndCleanUpMocks().startMocking();
                        }
                    }).throwsException(MultipleTrackingMockSessionException.class);
                    return null;
                }
            });
            Thread t = new Thread(task);
            t.start();
            task.get();
        } finally {
            session.finishMocking();
        }
    }

    @Test public void can_open_another_session_not_tracking_mocks() throws Exception {
        MockitoSession session = new DefaultMockitoSessionBuilder().trackAndCleanUpMocks().startMocking();

        try {
            FutureTask<Void> task = new FutureTask<Void>(new Callable<Void>() {
                @Override
                public Void call() {
                    new DefaultMockitoSessionBuilder().startMocking().finishMocking();
                    return null;
                }
            });
            Thread t = new Thread(task);
            t.start();
            task.get();
        } finally {
            session.finishMocking();
        }
    }

    @Test public void cleans_listeners_after_disallow_multiple_tracking_sessions() throws Exception {
        MockitoSession session = new DefaultMockitoSessionBuilder().trackAndCleanUpMocks().startMocking();

        try {
            FutureTask<Void> task = new FutureTask<Void>(new Callable<Void>() {
                @Override
                public Void call() {
                    ThrowableAssert.assertThat(new Runnable() {
                        public void run() {
                            new DefaultMockitoSessionBuilder().trackAndCleanUpMocks().startMocking();
                        }
                    }).throwsException(MultipleTrackingMockSessionException.class);
                    new DefaultMockitoSessionBuilder().startMocking().finishMocking();
                    return null;
                }
            });
            Thread t = new Thread(task);
            t.start();
            task.get();
        } finally {
            session.finishMocking();
        }
    }

    class TestClass {

        @Mock public Set<Object> set;

        class NestedTestClass {
            @Mock public List<Object> list;
        }
    }
}
