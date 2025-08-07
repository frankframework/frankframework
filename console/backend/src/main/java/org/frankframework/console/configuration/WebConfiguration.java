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
package org.frankframework.console.configuration;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.frankframework.management.bus.LocalGateway;
import org.frankframework.management.bus.OutboundGatewayFactory;
import org.frankframework.management.gateway.InputStreamHttpMessageConverter;

/**
 * This class is found by the `FrankFrameworkApiContext.xml` file, is loaded after the xml has been loaded/wired.
 */
@Configuration
@EnableWebMvc
public class WebConfiguration implements WebMvcConfigurer, EnvironmentAware {

	private String gatewayClassName;

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		Optional<HttpMessageConverter<?>> converterFound = converters.stream().filter(AbstractJackson2HttpMessageConverter.class::isInstance).findFirst();
		if (converterFound.isPresent()) {
			AbstractJackson2HttpMessageConverter converter = (AbstractJackson2HttpMessageConverter) converterFound.get();
			converter.setPrettyPrint(true);
		}

		converters.add(new InputStreamHttpMessageConverter());
		converters.add(new FormHttpMessageConverter());
	}

	@Override
	public void addFormatters(FormatterRegistry registry) {
		ApplicationConversionService.configure(registry);
	}

	@Bean
	StandardServletMultipartResolver multipartResolver() {
		return new StandardServletMultipartResolver();
	}

	@Bean
	public OutboundGatewayFactory outboundGateway() {
		OutboundGatewayFactory factory = new OutboundGatewayFactory();
		factory.setGatewayClassname(gatewayClassName);
		return factory;
	}

	@Override
	public void setEnvironment(Environment environment) {
		gatewayClassName = environment.getProperty("management.gateway.outbound.class", String.class, LocalGateway.class.getCanonicalName());
	}
}
