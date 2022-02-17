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

import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import nl.nn.adapterframework.configuration.Configuration;

@Path("/")
public class Metrics extends Base {
	@Context HttpServletRequest servletRequest;

	private PrometheusMeterRegistry registry;
	
	public Metrics() {
		this(Configuration.getPrometheusMeterRegistry());
	}
	
	public Metrics(PrometheusMeterRegistry registry) {
		this.registry = registry;
		// These classes are for exposing JVM specific metrics
		new ClassLoaderMetrics().bindTo(registry);
		new JvmMemoryMetrics().bindTo(registry);
		new JvmGcMetrics().bindTo(registry);
		new ProcessorMetrics().bindTo(registry);
		new JvmThreadMetrics().bindTo(registry);
	}
	
	@GET
	@Path("/metrics")
	@Produces(TextFormat.CONTENT_TYPE_004) // see https://github.com/prometheus/prometheus/issues/6499
	public Response getLogDirectory() throws ApiException {
		return Response.status(Response.Status.OK).entity(registry.scrape()).build(); // it would be better to write directly to response.getWriter()
	}

}
