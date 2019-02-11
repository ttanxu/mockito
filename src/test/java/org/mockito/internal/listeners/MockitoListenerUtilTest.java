/*
 * Copyright (c) 2019 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */

package org.mockito.internal.listeners;

import java.util.LinkedHashSet;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.junit.Test;
import org.mockito.exceptions.misusing.RedundantListenerException;
import org.mockito.listeners.MockitoListener;
import org.mockitoutil.TestBase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockitoListenerUtilTest extends TestBase {
    @Test
    public void should_not_allow_redundant_listeners() {
        MockitoListener listener1 = mock(MockitoListener.class);
        final MockitoListener listener2 = mock(MockitoListener.class);

        final Set<MockitoListener> listeners = new LinkedHashSet<MockitoListener>();

        //when
        MockitoListenerUtil.addListener(listener1, listeners);

        //then
        Assertions.assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
            public void call() {
                MockitoListenerUtil.addListener(listener2, listeners);
            }
        }).isInstanceOf(RedundantListenerException.class);
    }

    @Test
    public void should_clean_up_listeners_automatically() {
        MockitoListener someListener = mock(MockitoListener.class);
        MyListener cleanListener = mock(MyListener.class);
        MyListener dirtyListener = when(mock(MyListener.class).isListenerDirty()).thenReturn(true).getMock();

        Set<MockitoListener> listeners = new LinkedHashSet<MockitoListener>();

        //when
        MockitoListenerUtil.addListener(someListener, listeners);
        MockitoListenerUtil.addListener(dirtyListener, listeners);

        //then
        Assertions.assertThat(listeners).containsExactlyInAnyOrder(someListener, dirtyListener);

        //when
        MockitoListenerUtil.addListener(cleanListener, listeners);

        //then dirty listener was removed automatically
        Assertions.assertThat(listeners).containsExactlyInAnyOrder(someListener, cleanListener);
    }

    interface MyListener extends MockitoListener, AutoCleanableListener {}
}
