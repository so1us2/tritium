/*
 * (c) Copyright 2018 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.tritium.event.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.Metric;
import com.google.common.collect.ImmutableList;
import com.palantir.tritium.event.AbstractInvocationEventHandler;
import com.palantir.tritium.event.DefaultInvocationContext;
import com.palantir.tritium.event.InvocationContext;
import com.palantir.tritium.metrics.registry.DefaultTaggedMetricRegistry;
import com.palantir.tritium.metrics.registry.MetricName;
import com.palantir.tritium.metrics.registry.SlidingWindowTaggedMetricRegistry;
import com.palantir.tritium.metrics.registry.TaggedMetricRegistry;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TaggedMetricsServiceInvocationEventHandlerTest {

    public static final class TestImplementation {

        @SuppressWarnings("unused")
        public String doFoo() {
            return "bar";
        }

    }

    @Parameterized.Parameters
    public static ImmutableList<Supplier<Object>> data() {
        return ImmutableList.of(
                DefaultTaggedMetricRegistry::new,
                () -> new SlidingWindowTaggedMetricRegistry(30, TimeUnit.SECONDS));
    }

    @Parameterized.Parameter
    public Supplier<TaggedMetricRegistry> registrySupplier = () -> null;

    @Test
    public void testTaggedServiceMetricsCaptured() throws Exception {
        TaggedMetricRegistry registry = registrySupplier.get();

        TestImplementation testInterface = new TestImplementation();

        TaggedMetricsServiceInvocationEventHandler handler =
                new TaggedMetricsServiceInvocationEventHandler(registry, "quux");

        invokeMethod(handler, testInterface, "doFoo", "bar", /* success= */true);

        Map<MetricName, Metric> metrics = registry.getMetrics();
        MetricName expectedMetricName = MetricName.builder()
                .safeName("quux")
                .putSafeTags("service-name", "TestImplementation")
                .putSafeTags("endpoint", "doFoo")
                .build();
        assertThat(metrics).containsKey(expectedMetricName);
    }

    @Test
    public void testTaggedServiceMetricsCapturedAsErrors() throws Exception {
        TaggedMetricRegistry registry = registrySupplier.get();

        TestImplementation testInterface = new TestImplementation();

        TaggedMetricsServiceInvocationEventHandler handler =
                new TaggedMetricsServiceInvocationEventHandler(registry, "quux");

        invokeMethod(handler, testInterface, "doFoo", "bar", /* success= */false);

        Map<MetricName, Metric> metrics = registry.getMetrics();
        MetricName expectedMetricName = MetricName.builder()
                .safeName("quux-failures")
                .putSafeTags("service-name", "TestImplementation")
                .putSafeTags("endpoint", "doFoo")
                .putSafeTags("cause", RuntimeException.class.getName())
                .build();
        assertThat(metrics).containsKey(expectedMetricName);
    }

    private static void invokeMethod(
            AbstractInvocationEventHandler handler, Object obj, String methodName, Object result, boolean success)
            throws Exception {

        InvocationContext context = DefaultInvocationContext.of(obj, obj.getClass().getMethod(methodName), null);
        if (success) {
            handler.onSuccess(context, result);
        } else {
            handler.onFailure(context, new RuntimeException("fail"));
        }
    }
}
