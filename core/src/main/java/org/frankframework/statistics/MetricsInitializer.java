/*
   Copyright 2022-2025 WeAreFrank!

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
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.DistributionSummary.Builder;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter.Type;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.Search;
import lombok.Setter;

import org.frankframework.configuration.Configuration;
import org.frankframework.core.Adapter;
import org.frankframework.core.FrankElement;
import org.frankframework.core.HasName;
import org.frankframework.core.IPipe;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeLine;
import org.frankframework.http.HttpSession;
import org.frankframework.receivers.Receiver;
import org.frankframework.scheduler.AbstractJobDef;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.LogUtil;

public class MetricsInitializer implements InitializingBean, DisposableBean, ApplicationContextAware {
	public static final String PARENT_CHILD_NAME_FORMAT = "%s -> %s";
	protected Logger log = LogUtil.getLogger(this);
	private @Setter ApplicationContext applicationContext;

	private boolean publishPercentiles;
	private boolean publishHistograms;
	private int percentilePrecision;
	private List<String> percentiles;
	private List<String> timeSLO; // ServiceLevelObjectives
	private List<String> sizeSLO;

	private @Setter MeterRegistry meterRegistry;

	@Override
	public void afterPropertiesSet() {
		if(meterRegistry == null) {
			throw new IllegalStateException("unable to initialize MetricsInitializer, no registry set");
		}

		AppConstants appConstants = AppConstants.getInstance();
		publishPercentiles = appConstants.getBoolean("Statistics.percentiles.publish", false);
		percentilePrecision = appConstants.getInt("Statistics.percentiles.precision", 1);
		publishHistograms = appConstants.getBoolean("Statistics.histograms.publish", false);
		percentiles = appConstants.getListProperty("Statistics.percentiles"); // 50,90,95,98
		timeSLO = appConstants.getListProperty("Statistics.boundaries"); // 100,1000,2000,10000
		sizeSLO = appConstants.getListProperty("Statistics.size.boundaries"); // 100000,1000000
	}

	public Counter createCounter(@Nonnull FrankElement frankElement, @Nonnull FrankMeterType type) {
		return createCounter(type, getTags(frankElement, frankElement.getName(), null));
	}

	private String findName(HasName namedObject) {
		return StringUtils.isNotEmpty(namedObject.getName()) ? namedObject.getName() : ClassUtils.nameOf(namedObject);
	}

	public Timer.ResourceSample createTimerResource(@Nonnull FrankElement frankElement, @Nonnull FrankMeterType type, String... tags) {
		return Timer.resource(meterRegistry, type.getMeterName())
				.tags(tags)
				.tags(getTags(frankElement, findName(frankElement), null));
	}

	/** This DistributionSummary is suffixed under a pipe */
	public DistributionSummary createSubDistributionSummary(@Nonnull FrankElement parentFrankElement, @Nonnull HasName subFrankElement, @Nonnull FrankMeterType type) {
		return createSubDistributionSummary(parentFrankElement, findName(subFrankElement), type);
	}

	public DistributionSummary createSubDistributionSummary(@Nonnull FrankElement parentFrankElement, @Nonnull String subFrankElement, @Nonnull FrankMeterType type) {
		List<Tag> tags = getTags(parentFrankElement, String.format(PARENT_CHILD_NAME_FORMAT, findName(parentFrankElement), subFrankElement), null);
		return createDistributionSummary(type, tags);
	}

	public DistributionSummary createDistributionSummary(@Nonnull FrankElement frankElement, @Nonnull FrankMeterType type) {
		List<Tag> tags = getTags(frankElement, findName(frankElement), null);
		return createDistributionSummary(type, tags);
	}

	public DistributionSummary createThreadBasedDistributionSummary(Receiver<?> receiver, FrankMeterType type, int threadNumber) {
		List<Tag> tags = getTags(receiver, receiver.getName(), Collections.singletonList(Tag.of("thread", ""+threadNumber)));
		return createDistributionSummary(type, tags);
	}

	public Gauge createGauge(@Nonnull FrankElement frankElement, @Nonnull FrankMeterType type, Supplier<Number> numberSupplier) {
		return createGauge(type, getTags(frankElement, frankElement.getName(), null), numberSupplier);
	}

	private Counter createCounter(@Nonnull FrankMeterType type, List<Tag> tags) {
		if(type.getMeterType() != Type.COUNTER) {
			throw new IllegalStateException("MeterType ["+type+"] must be of type [Counter]");
		}
		return Counter.builder(type.getMeterName()).tags(tags).baseUnit(type.getBaseUnit()).register(meterRegistry);
	}

	private Gauge createGauge(@Nonnull FrankMeterType type, List<Tag> tags, Supplier<Number> numberSupplier) {
		if(type.getMeterType() != Type.GAUGE) {
			throw new IllegalStateException("MeterType ["+type+"] must be of type [Gauge]");
		}
		return Gauge.builder(type.getMeterName(), numberSupplier).tags(tags).baseUnit(type.getBaseUnit()).register(meterRegistry);
	}

	private DistributionSummary createDistributionSummary(@Nonnull FrankMeterType type, List<Tag> tags) {
		if(type.getMeterType() != Type.DISTRIBUTION_SUMMARY) {
			throw new IllegalStateException("MeterType ["+type+"] must be of type [DistributionSummary]");
		}

		Builder builder = DistributionSummary.builder(type.getMeterName()).tags(tags).baseUnit(type.getBaseUnit());
		if(publishPercentiles || publishHistograms) {
			builder.percentilePrecision(percentilePrecision);
			builder.publishPercentiles(getPercentiles());
			if(FrankMeterType.TIME_UNIT.equals(type.getBaseUnit())) {
				double[] slo = timeSLO.stream().mapToDouble(Double::parseDouble).toArray();
				builder.serviceLevelObjectives(slo);
				builder.maximumExpectedValue(slo[slo.length-1]);
			} else if(FrankMeterType.SIZE_UNIT.equals(type.getBaseUnit())) {
				double[] slo = sizeSLO.stream().mapToDouble(Double::parseDouble).toArray();
				builder.serviceLevelObjectives(slo);
				builder.maximumExpectedValue(slo[slo.length-1]);
			}

			if(publishHistograms) {
				builder.publishPercentileHistogram();
			}
		}
		return builder.register(meterRegistry);
	}

	private List<Tag> getTags(@Nonnull FrankElement frankElement, @Nonnull String name, @Nullable List<Tag> extraTags) {
		List<Tag> tags = new ArrayList<>(5);
		Adapter adapter = getAdapter(frankElement);
		if(adapter != null) {
			tags.add(Tag.of("adapter", adapter.getName()));
		}
		Configuration configuration = getConfiguration(frankElement);
		if(configuration != null) {
			tags.add(Tag.of("configuration", configuration.getId()));
		}
		tags.add(Tag.of("name", name));
		tags.add(Tag.of("type", getElementType(frankElement)));
		if(extraTags != null) {
			tags.addAll(extraTags);
		}

		return tags;
	}

	@Nullable
	private Configuration getConfiguration(@Nonnull FrankElement frankElement) {
		ApplicationContext ac = frankElement.getApplicationContext();
		if (ac instanceof Configuration config) {
			return config;
		} else if (ac instanceof Adapter adapter) {
			return (Configuration) adapter.getParent();
		}
		return null; //TODO throw new IllegalStateException("No ConfigurationContext found");
	}

	private Adapter getAdapter(@Nonnull FrankElement frankElement) {
		if (frankElement instanceof Adapter adapter) {
			return adapter;
		}

		if (frankElement.getApplicationContext() instanceof Adapter adapter) {
			return adapter;
		}

		return null;
	}

	private String getElementType(@Nonnull FrankElement frankElement) {
		if (frankElement instanceof Receiver) {
			return "receiver";
		} else if (frankElement instanceof PipeLine) {
			return "pipeline";
		} else if (frankElement instanceof IPipe) {
			return "pipe";
		} else if (frankElement instanceof Adapter) {
			return "adapter";
		} else if (frankElement instanceof ISender) {
			return "sender";
		} else if (frankElement instanceof AbstractJobDef) {
			return "schedule";
		} else if (frankElement instanceof HttpSession) {
			// See `org.frankframework.http.HttpSessionBase.buildHttpClient` where this might use the HttpSession as frankElement
			return "httpSession";
		} else {
			throw new IllegalStateException("meter type not configured");
		}
	}

	private double[] getPercentiles() {
		if (percentiles.size() > 4) {
			log.warn("using more than 4 percentiles is heavily discouraged");
		}
		// Validate must be whole number between 50 and 100.
		return percentiles.stream()
				.mapToDouble(Double::parseDouble)
				.map(e -> e / 100)
				.toArray();
	}

	@Override
	public void destroy() throws Exception {
		Search search = Search.in(meterRegistry).tag("configuration", applicationContext.getId());
		search.counters().forEach(meterRegistry::remove);
		search.gauges().forEach(meterRegistry::remove);
		search.summaries().forEach(meterRegistry::remove);
	}

}
