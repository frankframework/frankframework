/*
  Copyright 2024-2025 WeAreFrank!

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

import java.util.function.Function;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.protocol.HttpContext;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.http.Outcome;

import org.frankframework.statistics.FrankMeterType;
import org.frankframework.statistics.HasStatistics;
import org.frankframework.statistics.MetricsInitializer;

/**
 * Based on MicrometerHttpClientInterceptor in `micrometer-metrics` repository on
 * <a href="https://github.com/micrometer-metrics/micrometer/blob/main/micrometer-core/src/main/java/io/micrometer/core/instrument/binder/httpcomponents/MicrometerHttpClientInterceptor.java">github</a>.
 * <br/>
 * The implementation is a bit different to integrate correctly with the framework by using the `MetricsInitializer` and a `IConfigurationAware` element.
 * Please note that this code is specific for Apache Http Components version 4.
 */
public class MicrometerHttpClientInterceptor {

	private final HttpRequestInterceptor requestInterceptor;

	private final HttpResponseInterceptor responseInterceptor;

	// Keep the resource sample as a ThreadLocal
	private final ThreadLocal<Timer.ResourceSample> threadLocal = new ThreadLocal<>();

	/**
	 * Create a {@code MicrometerHttpClientInterceptor} instance.
	 *
	 * @param configurationMetrics
	 * @param parentFrankElement
	 * @param uriMapper            URI mapper to create {@code uri} tag
	 * @param exportTagsForRoute   whether to export tags for route
	 */
	public MicrometerHttpClientInterceptor(MetricsInitializer configurationMetrics, HasStatistics parentFrankElement,
										Function<HttpRequest, String> uriMapper, boolean exportTagsForRoute) {

		this.requestInterceptor = (request, context) ->
				threadLocal.set(configurationMetrics.createTimerResource(parentFrankElement, FrankMeterType.SENDER_HTTP,
						"method", request.getRequestLine().getMethod(),
						"uri", uriMapper.apply(request)
				));

		this.responseInterceptor = (response, context) -> {
			Timer.ResourceSample resourceSample = threadLocal.get();

			if (resourceSample != null) {
				resourceSample.tag("status", Integer.toString(response.getStatusLine().getStatusCode()))
						.tag("outcome", Outcome.forStatus(response.getStatusLine().getStatusCode()).name())
						.tags(exportTagsForRoute ? generateTagsForRoute(context) : Tags.empty())
						.close();

				threadLocal.remove();
			}
		};
	}

	private Tags generateTagsForRoute(HttpContext context) {
		String targetScheme = "UNKNOWN";
		String targetHost = "UNKNOWN";
		String targetPort = "UNKNOWN";
		Object routeAttribute = context.getAttribute("http.route");

		if (routeAttribute instanceof HttpRoute httpRoute) {
			HttpHost host = httpRoute.getTargetHost();
			targetScheme = host.getSchemeName();
			targetHost = host.getHostName();
			targetPort = String.valueOf(host.getPort());
		}

		return Tags.of(
				"target.schema", targetScheme,
				"target.host", targetHost,
				"target.port", targetPort
		);
	}

	public HttpRequestInterceptor getRequestInterceptor() {
		return requestInterceptor;
	}

	public HttpResponseInterceptor getResponseInterceptor() {
		return responseInterceptor;
	}
}
