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

import java.io.IOException;
import java.io.OutputStream;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import lombok.Setter;

import org.frankframework.http.AbstractHttpServlet;
import org.frankframework.lifecycle.IbisInitializer;
import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;

@IbisInitializer
public class PrometheusMeterServlet extends AbstractHttpServlet {
	private static final boolean ACTIVE = AppConstants.getInstance().getBoolean("management.metrics.export.prometheus.enabled", false);

	private PrometheusMeterRegistry prometheusRegistry = null;
	private transient @Setter @Autowired MeterRegistry registry;
	private static final Logger LOG = LogUtil.getLogger(PrometheusMeterServlet.class);

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		if(ACTIVE) {
			Assert.notNull(registry, "metrics registry not found");

			if (registry instanceof CompositeMeterRegistry compositeMeterRegistry) {
				for(MeterRegistry meterRegistry:compositeMeterRegistry.getRegistries()) {
					if (meterRegistry instanceof PrometheusMeterRegistry prometheusMeterRegistry) {
						prometheusRegistry = prometheusMeterRegistry;
					}
				}
			}
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			if (prometheusRegistry==null) {
				resp.sendError(501, "Prometheus registry not found");
			} else {
				try (OutputStream stream = resp.getOutputStream()) {
					prometheusRegistry.scrape(stream);
				}
			}
		} catch (IOException e) {
			LOG.warn("unable to scrape PrometheusRegistry", e);
		}
	}

	@Override
	public boolean isEnabled() {
		return ACTIVE;
	}

	@Override
	public String getUrlMapping() {
		return "/metrics/prometheus";
	}
}
