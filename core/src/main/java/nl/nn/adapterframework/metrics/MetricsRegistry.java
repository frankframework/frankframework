/*
   Copyright 2022 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.metrics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.logging.Log4j2Metrics;
import io.micrometer.core.instrument.binder.system.DiskSpaceMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.Getter;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.Misc;

/**
 * Singleton bean that keeps track of a Spring Application's uptime.
 *
 */
public class MetricsRegistry {

	private @Getter MeterRegistry registry;

	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();

	public MetricsRegistry() {
		CompositeMeterRegistry compositeRegistry = new CompositeMeterRegistry();
		compositeRegistry.add(new PrometheusMeterRegistry(PrometheusConfig.DEFAULT));

		new StatsDRegistryConfigurator().registerAt(compositeRegistry);
		new CloudWatchRegistryConfigurator().registerAt(compositeRegistry);
		new KairosDbRegistryConfigurator().registerAt(compositeRegistry);

		this.registry = compositeRegistry;
		configureRegistry();
	}

	private void configureRegistry() {
		registry.config().commonTags(
			"instance", APP_CONSTANTS.getString("instance.name",""),
			"ff_version", APP_CONSTANTS.getString("application.version", "unknown"),
			"hostname" , Misc.getHostname(),
			"dtap_stage" , APP_CONSTANTS.getProperty("dtap.stage")
		);

		List<Tag> tags = new ArrayList<>();
		tags.add(Tag.of("type", "system"));
		// These classes are for exposing JVM specific metrics
		new ClassLoaderMetrics(tags).bindTo(registry);
		new JvmMemoryMetrics(tags).bindTo(registry);
		new JvmGcMetrics(tags).bindTo(registry);
		new ProcessorMetrics(tags).bindTo(registry);
		new JvmThreadMetrics(tags).bindTo(registry);
		String logDir = APP_CONSTANTS.get("log.dir");
		if(StringUtils.isNotEmpty(logDir)) {
			File f = new File(logDir);
			new DiskSpaceMetrics(f, tags).bindTo(registry);
		}
		new Log4j2Metrics(tags).bindTo(registry);
	}
}
