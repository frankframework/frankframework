/*
  Copyright 2024 WeAreFrank!

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

package org.frankframework.http;

import io.micrometer.common.lang.NonNull;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.pool.ConnPoolControl;
import org.frankframework.core.IConfigurationAware;
import org.frankframework.statistics.FrankMeterType;
import org.frankframework.statistics.MetricsInitializer;

/**
 * Based on PoolingHttpClientConnectionManagerMetricsBinder in `micrometer-core` repository on
 * <a href="https://github.com/micrometer-metrics/micrometer/blob/main/micrometer-core/src/main/java/io/micrometer/core/instrument/binder/httpcomponents/PoolingHttpClientConnectionManagerMetricsBinder.java">github</a>
 * <br/>
 * The implementation is a bit different to integrate correctly with the framework by using the `MetricsInitializer` and a `IConfigurationAware` element.
 * Please note that this code is specific for Apache Http Components version 4.
 */
public class MicrometerConnectionManagerMetricsBinder implements MeterBinder {

	private final ConnPoolControl<HttpRoute> connPoolControl;
	private final MetricsInitializer configurationMetrics;
	private final IConfigurationAware frankElement;

	public MicrometerConnectionManagerMetricsBinder(ConnPoolControl<HttpRoute> connPoolControl, MetricsInitializer configurationMetrics,
													IConfigurationAware frankElement) {
		this.connPoolControl = connPoolControl;
		this.configurationMetrics = configurationMetrics;
		this.frankElement = frankElement;
	}

	@Override
	public void bindTo(@NonNull MeterRegistry registry) {
		registerTotalMetrics();
	}

	private void registerTotalMetrics() {
		configurationMetrics.createGauge(frankElement, FrankMeterType.SENDER_HTTP_CLIENT_MAX, () -> connPoolControl.getTotalStats().getMax());
		configurationMetrics.createGauge(frankElement, FrankMeterType.SENDER_HTTP_CLIENT_AVAILABLE, () -> connPoolControl.getTotalStats().getAvailable());
		configurationMetrics.createGauge(frankElement, FrankMeterType.SENDER_HTTP_CLIENT_LEASED, () -> connPoolControl.getTotalStats().getLeased());
		configurationMetrics.createGauge(frankElement, FrankMeterType.SENDER_HTTP_CLIENT_PENDING, () -> connPoolControl.getTotalStats().getPending());
	}
}
