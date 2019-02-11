/*
 * Copyright (c) 2017 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.framework;

import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockSettings;
import org.mockito.Mockito;
import org.mockito.MockitoFramework;
import org.mockito.StateMaster;
import org.mockito.exceptions.misusing.RedundantListenerException;
import org.mockito.listeners.MockCreationListener;
import org.mockito.listeners.MockitoListener;
import org.mockito.listeners.VerificationListener;
import org.mockito.mock.MockCreationSettings;
import org.mockitoutil.TestBase;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.*;
import static org.mockitoutil.ThrowableAssert.assertThat;

public class DefaultMockitoFrameworkTest extends TestBase {

    private DefaultMockitoFramework framework = new DefaultMockitoFramework();

    @After public void clearListeners() {
        new StateMaster().clearMockitoListeners();
    }

    @Test(expected = IllegalArgumentException.class)
    public void prevents_adding_null_listener() {
        framework.addListener(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void prevents_removing_null_listener() {
        framework.removeListener(null);
    }

    @Test
    public void ok_to_remove_unknown_listener() {
        //it is safe to remove listener that was not added before
        framework.removeListener(new MockitoListener() {});
    }

    @Test
    public void ok_to_remove_listener_multiple_times() {
        MockitoListener listener = new MockitoListener() {};

        //when
        framework.addListener(listener);

        //then it is ok to:
        framework.removeListener(listener);
        framework.removeListener(listener);
    }

    @Test
    public void adds_creation_listener() {
        //given creation listener is added
        MockCreationListener listener = mock(MockCreationListener.class);
        framework.addListener(listener);

        //when
        MockSettings settings = withSettings().name("my list");
        List mock = mock(List.class, settings);
        Set mock2 = mock(Set.class);

        //then
        verify(listener).onMockCreated(eq(mock), any(MockCreationSettings.class));
        verify(listener).onMockCreated(eq(mock2), any(MockCreationSettings.class));
        verifyNoMoreInteractions(listener);
    }

    @SuppressWarnings({"CheckReturnValue", "MockitoUsage"})
    @Test
    public void removes_creation_listener() {
        //given creation listener is added
        MockCreationListener listener = mock(MockCreationListener.class);
        framework.addListener(listener);

        //and hooked up correctly
        mock(List.class);
        verify(listener).onMockCreated(ArgumentMatchers.any(), any(MockCreationSettings.class));

        //when
        framework.removeListener(listener);
        mock(Set.class);

        //then
        verifyNoMoreInteractions(listener);
    }

    @Test public void prevents_duplicate_listeners_of_the_same_type() {
        //given creation listener is added
        framework.addListener(new MyListener());

        assertThat(new Runnable() {
            @Override
            public void run() {
                framework.addListener(new MyListener());
            }
        })  .throwsException(RedundantListenerException.class)
            .throwsMessage("\n" +
                    "Problems adding Mockito listener.\n" +
                    "Listener of type 'MyListener' has already been added and not removed.\n" +
                    "It indicates that previous listener was not removed according to the API.\n" +
                    "When you add a listener, don't forget to remove the listener afterwards:\n" +
                    "  Mockito.framework().removeListener(myListener);\n" +
                    "For more information, see the javadoc for RedundantListenerException class.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void prevents_adding_null_global_listener() {
        framework.addGlobalListener(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void prevents_removing_null_global_listener() {
        framework.removeGlobalListener(null);
    }

    @Test
    public void ok_to_remove_unknown_global_listener() {
        //it is safe to remove listener that was not added before
        framework.removeGlobalListener(new MockitoListener() {});
    }

    @Test
    public void ok_to_remove_global_listener_multiple_times() {
        MockitoListener listener = new MockitoListener() {};

        //when
        framework.addListener(listener);

        //then it is ok to:
        framework.removeGlobalListener(listener);
        framework.removeGlobalListener(listener);
    }

    @SuppressWarnings({"CheckReturnValue"})
    @Test
    public void adds_global_creation_listener() {
        MockitoFramework mockitoFramework = Mockito.framework();
        assumeTrue(mockitoFramework instanceof DefaultMockitoFramework);
        DefaultMockitoFramework framework = (DefaultMockitoFramework)mockitoFramework;

        //given creation listener is added
        MockCreationListener listener = mock(MockCreationListener.class);
        framework.addGlobalListener(listener);

        //and hooked up correctly
        mock(List.class);
        verify(listener).onMockCreated(ArgumentMatchers.any(), any(MockCreationSettings.class));

        //when
        framework.removeGlobalListener(listener);
        mock(Set.class);

        //then
        verifyNoMoreInteractions(listener);
    }

    @Test public void prevents_duplicate_global_listeners_of_the_same_type() {
        MockitoFramework mockitoFramework = Mockito.framework();
        assumeTrue(mockitoFramework instanceof DefaultMockitoFramework);
        final DefaultMockitoFramework framework = (DefaultMockitoFramework)mockitoFramework;

        //given creation listener is added
        framework.addGlobalListener(new MyListener());

        assertThat(new Runnable() {
            @Override
            public void run() {
                framework.addGlobalListener(new MyListener());
            }
        })  .throwsException(RedundantListenerException.class)
            .throwsMessage("\n" +
                               "Problems adding Mockito listener.\n" +
                               "Listener of type 'MyListener' has already been added and not removed.\n" +
                               "It indicates that previous listener was not removed according to the API.\n" +
                               "When you add a listener, don't forget to remove the listener afterwards:\n" +
                               "  Mockito.framework().removeListener(myListener);\n" +
                               "For more information, see the javadoc for RedundantListenerException class.");
    }

    @Test
    public void combines_verification_listeners() {
        MockitoFramework mockitoFramework = Mockito.framework();
        assumeTrue(mockitoFramework instanceof DefaultMockitoFramework);
        final DefaultMockitoFramework framework = (DefaultMockitoFramework)mockitoFramework;

        VerificationListener threadLocalListener = mock(VerificationListener.class);
        framework.addListener(threadLocalListener);

        VerificationListener globalListener = mock(VerificationListener.class);
        framework.addGlobalListener(globalListener);

        Set<VerificationListener> listeners = framework.verificationListeners();
        assertTrue(listeners.contains(threadLocalListener));
        assertTrue(listeners.contains(globalListener));

        framework.removeListener(threadLocalListener);
        framework.removeGlobalListener(globalListener);
    }

    private static class MyListener implements MockitoListener {}
}
