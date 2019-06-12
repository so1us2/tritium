/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.tritium.metrics.registry;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.codahale.metrics.Gauge;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

// @RunWith(Parameterized.class)
public class AbstractTaggedMetricRegistryTest {
    private final TaggedMetricRegistry registry = new DefaultTaggedMetricRegistry();

    @Mock
    private TaggedMetricRegistryListener listener;
    @Mock
    private TaggedMetricRegistryListener listener2;
    @Mock
    private MetricName name;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void adding_new_metric_triggers_listeners() {
        registry.addListener(listener);
        Gauge<Integer> gauge = () -> 1;
        registry.gauge(name, gauge);
        verify(listener).onGaugeAdded(name, gauge);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void removing_metric_triggers_listeners() {
        registry.addListener(listener);
        Gauge<Integer> gauge = () -> 1;
        registry.gauge(name, gauge);
        reset(listener);

        registry.remove(name);
        verify(listener).onGaugeRemoved(name);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void removed_listener_does_not_get_notified() {
        registry.addListener(listener);
        registry.removeListener(listener);
        Gauge<Integer> gauge = () -> 1;
        registry.gauge(name, gauge);
        verifyNoMoreInteractions(listener);
    }

}
