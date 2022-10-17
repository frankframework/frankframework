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
package nl.nn.adapterframework.webcontrol.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.InitializingBean;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import nl.nn.adapterframework.metrics.MetricsRegistry;

@Path("/")
public class Metrics extends Base implements InitializingBean {

	private PrometheusMeterRegistry prometheusRegistry = null;

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		MetricsRegistry metrics = getApplicationContext().getBean("metricsRegistry", MetricsRegistry.class);
		MeterRegistry metersRegistry = metrics.getRegistry();

		if (metersRegistry instanceof CompositeMeterRegistry) {
			CompositeMeterRegistry compositeMeterRegistry = (CompositeMeterRegistry)metersRegistry;
			for(MeterRegistry meterRegistry:compositeMeterRegistry.getRegistries()) {
				if (meterRegistry instanceof PrometheusMeterRegistry) {
					prometheusRegistry = (PrometheusMeterRegistry)meterRegistry;
				}
			}
		}
	}

	@GET
	@Path("/metrics/prometheus")
	@Produces(TextFormat.CONTENT_TYPE_004) // see https://github.com/prometheus/prometheus/issues/6499
	public Response scrapeForPrometheus() throws ApiException {
		if (prometheusRegistry==null) {
			return Response.status(Response.Status.NOT_IMPLEMENTED).build();
		}
		return Response.status(Response.Status.OK).entity(prometheusRegistry).build(); // uses PrometheusMessageBodyWriter
	}
}
