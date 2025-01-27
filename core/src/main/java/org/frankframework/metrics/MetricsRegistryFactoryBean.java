/*
   Copyright 2022-2024 WeAreFrank!

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
package org.frankframework.metrics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

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
import lombok.extern.log4j.Log4j2;

import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.Misc;

/**
 * Singleton bean that keeps track of a Spring Application's uptime.
 *
 */
@Log4j2
public class MetricsRegistryFactoryBean implements InitializingBean, DisposableBean, FactoryBean<MeterRegistry> {

	private static final String CONFIGURATOR_CLASS_SUFFIX = ".configurator";
	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();

	private MeterRegistry registry;
	private @Nullable JvmGcMetrics jvmGcMetrics;
	private @Nullable Log4j2Metrics log4j2Metrics;

	private void createRegistry() {
		CompositeMeterRegistry compositeRegistry = new CompositeMeterRegistry();
		Properties metricProperties = AppConstants.getInstance().getAppConstants(AbstractMetricsRegistryConfigurator.METRICS_EXPORT_PROPERTY_PREFIX);

		for(Object keyObj:metricProperties.keySet()) {
			String key = (String)keyObj;
			if (key.endsWith(".enabled")) {
				String tail = key.substring(AbstractMetricsRegistryConfigurator.METRICS_EXPORT_PROPERTY_PREFIX.length());
				String[] tailArr = tail.split("\\.");
				if (tailArr.length==2 && APP_CONSTANTS.getBoolean(key, false)) {
					AbstractMetricsRegistryConfigurator<?> config = loadMeterRegistry(metricProperties, tailArr[0]);
					if(config != null) {
						config.registerAt(compositeRegistry);
					}
				}
			}
		}

		this.registry = compositeRegistry;
	}

	private AbstractMetricsRegistryConfigurator<?> loadMeterRegistry(Properties metricProperties, String product) {
		final String configuratorClassNamePropertyKey = AbstractMetricsRegistryConfigurator.METRICS_EXPORT_PROPERTY_PREFIX+product+CONFIGURATOR_CLASS_SUFFIX;
		final String configuratorClassName = metricProperties.getProperty(configuratorClassNamePropertyKey);
		if (StringUtils.isEmpty(configuratorClassName)) {
			log.warn("did not find value for property [{}] to enable configuration of enabled meter registy product [{}]", configuratorClassNamePropertyKey, product);
			return null;
		}

		log.debug("using class [{}] to configure MeterRegistry [{}]", configuratorClassName, product);
		try {
			return ClassUtils.newInstance(configuratorClassName, AbstractMetricsRegistryConfigurator.class);
		} catch (Exception e) {
			log.warn("cannot configure MeterRegistry [{}]", product, e);
		}
		return null;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		createRegistry();
		Assert.notNull(registry, "unable to create registry");

		setupCommonTags();
		addSystemMetrics();
	}

	private void setupCommonTags() {
		registry.config().commonTags(
			"instance", APP_CONSTANTS.getString("instance.name",""),
			"ff_version", APP_CONSTANTS.getString("application.version", "unknown"),
			"hostname" , Misc.getHostname(),
			"dtap_stage" , APP_CONSTANTS.getProperty("dtap.stage")
		);
	}

	// These classes are for exposing JVM specific metrics
	private void addSystemMetrics() {
		List<Tag> tags = new ArrayList<>();
		tags.add(Tag.of("type", "system"));
		new ClassLoaderMetrics(tags).bindTo(registry);
		new JvmMemoryMetrics(tags).bindTo(registry);
		new ProcessorMetrics(tags).bindTo(registry);
		new JvmThreadMetrics(tags).bindTo(registry);

		jvmGcMetrics = new JvmGcMetrics(tags);
		jvmGcMetrics.bindTo(registry);

		log4j2Metrics = new Log4j2Metrics(tags);
		log4j2Metrics.bindTo(registry);

		String logDir = APP_CONSTANTS.get("log.dir");
		if(StringUtils.isNotEmpty(logDir)) {
			File f = new File(logDir);
			new DiskSpaceMetrics(f, tags).bindTo(registry);
		}
	}

	@Override
	public MeterRegistry getObject() {
		return registry;
	}

	@Override
	public Class<? extends MeterRegistry> getObjectType() {
		return  this.registry != null ? this.registry.getClass() : MeterRegistry.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void destroy() throws Exception {
		try {
			if(jvmGcMetrics != null) jvmGcMetrics.close();
			if(log4j2Metrics != null) log4j2Metrics.close();
		} finally {
			registry.close();
		}
	}
}
