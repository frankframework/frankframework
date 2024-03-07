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
package org.frankframework.statistics;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.core.IConfigurationAware;
import org.frankframework.core.IPipe;
import org.frankframework.core.PipeLine;
import org.frankframework.core.SenderException;
import org.frankframework.monitoring.EventThrowing;
import org.frankframework.receivers.Receiver;
import org.frankframework.util.LogUtil;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Meter.Type;
import io.micrometer.core.instrument.binder.cache.EhCache2Metrics;
import io.micrometer.core.instrument.search.Search;
import lombok.Setter;
import net.sf.ehcache.Ehcache;

public class MetricsInitializer implements StatisticsKeeperIterationHandler<MetricsInitializer.NodeConfig>, InitializingBean, DisposableBean, ApplicationContextAware {
	protected Logger log = LogUtil.getLogger(this);
	private @Setter ApplicationContext applicationContext;

	private MeterRegistry registry;
	private NodeConfig root;

	protected static class NodeConfig {
		public final String name;
		public final List<Tag> tags;
		public final int groupLevel;

		NodeConfig(String name, List<Tag> tags, int groupLevel) {
			this.name = name;
			this.tags = tags!=null ? tags : new LinkedList<>();
			this.groupLevel = groupLevel;
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		try {
			registry = applicationContext.getBean(MeterRegistry.class);
		} catch (BeanCreationException | BeanInstantiationException | NoSuchBeanDefinitionException e) {
			throw new IllegalStateException("unable to initialize MetricsInitializer, no registry set", e);
		}

		List<Tag> tags = new LinkedList<>();
		tags.add(Tag.of("type", "application"));
		root = new NodeConfig("frank", tags, 0);
	}

	public Counter createCounter(@Nonnull IConfigurationAware frankElement, @Nonnull FrankMeterType type) {
		if(type.getMeterType() == Type.COUNTER) {
			return (Counter) createMeter(frankElement, type);
		}
		return null;
	}

	public Meter createMeter(@Nonnull IConfigurationAware frankElement, @Nonnull FrankMeterType type) {
		if(frankElement instanceof EventThrowing) { //TODO implement a nice interface with getAdapter instead of misusing the EventThrowing interface
			EventThrowing elm = (EventThrowing) frankElement;
			if(elm.getAdapter() != null) {
				return createMeter(frankElement, type, elm.getAdapter());
			}
		}
		return createMeter(frankElement, type, null);
	}

	private Meter createMeter(@Nonnull IConfigurationAware frankElement, @Nonnull FrankMeterType type, @Nullable Adapter adapter) {
		Objects.requireNonNull(frankElement.getName());

		String elementType;
		if(frankElement instanceof Receiver) {
			elementType = "receiver";
		} else if(frankElement instanceof PipeLine) {
			elementType = "pipeline";
		} else if(frankElement instanceof IPipe) {
			elementType = "pipe";
		} else if(frankElement instanceof Adapter) {
			elementType = "adapter";
		} else {
			throw new IllegalStateException("meter type not configured");
		}

		ApplicationContext configuration = frankElement.getApplicationContext();
		List<Tag> tags = new ArrayList<>(4);
		if(adapter != null) {
			tags.add(Tag.of("adapter", adapter.getAdapter().getName()));
		}
		tags.add(Tag.of("configuration", configuration.getId()));
		tags.add(Tag.of("name", frankElement.getName()));
		tags.add(Tag.of("type", elementType));

		//new Meter.Id(name, tags, baseUnit, description, Type.COUNTER)
		switch (type.getMeterType()) {
		case COUNTER:
			return Counter.builder(type.getMeterName()).tags(tags).register(registry);
		case DISTRIBUTION_SUMMARY:
			throw new IllegalStateException("tbd");
//			return DistributionSummary.builder(type.getMeterName()).tags(tags).register(registry);
		default:
			throw new IllegalArgumentException("Unexpected value: " + type.getMeterType());
		}
	}

	@Override
	public void destroy() throws Exception {
		Search search = Search.in(registry).tag("configuration", applicationContext.getId());
		search.counters().parallelStream().forEach(e -> {
			registry.remove(e);
		});
		search.gauges().parallelStream().forEach(e -> {
			registry.remove(e);
		});
	}

	@Override
	public void configure() throws ConfigurationException {
		//not used
	}

	@Override
	public NodeConfig start(Date now, Date mainMark, Date detailMark) throws SenderException {
		return root;
	}

	@Override
	public void end(NodeConfig data) throws SenderException {
		//not used
	}

	@Override
	public void handleStatisticsKeeper(NodeConfig data, StatisticsKeeper sk) throws SenderException {
		if (sk==null) {
			log.warn("StatisticsKeeper is null");
			return;
		}
		if (data==null) {
			log.warn("NodeConfig data is null, sk="+sk.getName());
			sk.initMetrics(registry, sk.getName(), null);
			return;
		}
		sk.initMetrics(registry, data.name, data.tags);
	}

	@Override
	public void handleScalar(NodeConfig data, String scalarName, ScalarMetricBase<?> meter) throws SenderException {
		meter.initMetrics(registry, data.name, data.tags, scalarName);
	}

	@Override
	public void handleScalar(NodeConfig data, String scalarName, long value) throws SenderException {
		//not used
	}

	@Override
	public void handleScalar(NodeConfig data, String scalarName, Date value) throws SenderException {
		//not used
	}

	@Override
	public NodeConfig openGroup(NodeConfig parentData, String dimensionName, String type) throws SenderException {
		String nodeName = parentData.name;
		List<Tag> tags = new LinkedList<>(parentData.tags);
		int groupLevel = parentData.groupLevel;
		if (StringUtils.isNotEmpty(dimensionName)) {
			tags.add(Tag.of(type, dimensionName));
		} else {
			nodeName=nodeName+"."+type;
		}
		return new NodeConfig(nodeName, tags, groupLevel);
	}

	@Override
	public void closeGroup(NodeConfig data) throws SenderException {
		//not used
	}

	public void configureCache(Ehcache cache) {
		new EhCache2Metrics(cache, root.tags).bindTo(registry);
	}

}
