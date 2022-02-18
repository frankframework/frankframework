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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exporter.common.TextFormat;

@Path("/")
public class Metrics extends Base {
	@Context HttpServletRequest servletRequest;

	private PrometheusMeterRegistry registry;

	protected void initRegistry() {
		if (registry==null) {
			MeterRegistry metersRegistry = getIbisContext().getMeterRegistry();
			if (metersRegistry instanceof PrometheusMeterRegistry) {
				registry = (PrometheusMeterRegistry)metersRegistry;
			} else if (metersRegistry instanceof CompositeMeterRegistry) {
				CompositeMeterRegistry compositeMeterRegistry = (CompositeMeterRegistry)metersRegistry;
				for(MeterRegistry meterRegistry:compositeMeterRegistry.getRegistries()) {
					if (meterRegistry instanceof PrometheusMeterRegistry) {
						registry = (PrometheusMeterRegistry)meterRegistry;
						break;
					}
				}
			}
		}
	}

	@GET
	@Path("/metrics")
	@Produces(TextFormat.CONTENT_TYPE_004) // see https://github.com/prometheus/prometheus/issues/6499
	public Response scrapeForPrometheus() throws ApiException {
		if (registry==null) {
			initRegistry();
			if (registry==null) {
				return Response.status(Response.Status.NOT_IMPLEMENTED).build();
			}
		}
		return Response.status(Response.Status.OK).entity(registry).build(); // uses PrometheusMessageBodyWriter
	}

}
