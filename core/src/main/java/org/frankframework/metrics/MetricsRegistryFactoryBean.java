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
package org.frankframework.metrics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
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
import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;
import org.frankframework.util.Misc;

/**
 * Singleton bean that keeps track of a Spring Application's uptime.
 *
 */
public class MetricsRegistryFactoryBean implements InitializingBean, DisposableBean, FactoryBean<MeterRegistry> {

	private static final String CONFIGURATOR_CLASS_SUFFIX = ".configurator";
	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();

	private final Logger log = LogUtil.getLogger(this);
	private final MeterRegistry registry;
	private @Nullable JvmGcMetrics jvmGcMetrics;
	private @Nullable Log4j2Metrics log4j2Metrics;

	public MetricsRegistryFactoryBean() {
		CompositeMeterRegistry compositeRegistry = new CompositeMeterRegistry();

		for(Object keyObj:APP_CONSTANTS.keySet()) {
			String key = (String)keyObj;
			if (key.startsWith(MetricsRegistryConfiguratorBase.METRICS_EXPORT_PROPERTY_PREFIX) && key.endsWith(".enabled")) {
				String tail = key.substring(MetricsRegistryConfiguratorBase.METRICS_EXPORT_PROPERTY_PREFIX.length());
				String[] tailArr = tail.split("\\.");
				if (tailArr.length==2 && APP_CONSTANTS.getBoolean(key, false)) {
					String product=tailArr[0];
					String configuratorClassNamePropertyKey = MetricsRegistryConfiguratorBase.METRICS_EXPORT_PROPERTY_PREFIX+product+CONFIGURATOR_CLASS_SUFFIX;
					String configuratorClassName = APP_CONSTANTS.get(configuratorClassNamePropertyKey);
					if (StringUtils.isEmpty(configuratorClassName)) {
						log.warn("did not find value for property ["+configuratorClassNamePropertyKey+"] to enable configuration of enabled meter registy product ["+product+"]");
						continue;
					}
					log.debug("using class ["+configuratorClassName+"] to configure enabled meter registy product ["+product+"]");
					try {
						Class<MetricsRegistryConfiguratorBase> configuratorClass = (Class<MetricsRegistryConfiguratorBase>) Class.forName(configuratorClassName);
						MetricsRegistryConfiguratorBase configurator = configuratorClass.newInstance();
						configurator.registerAt(compositeRegistry);
					} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
						log.warn("Cannot configure meter registy product ["+product+"]", e);
					}
				}
			}
		}

		this.registry = compositeRegistry;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
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
		return  (this.registry != null ? this.registry.getClass() : MeterRegistry.class);
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
