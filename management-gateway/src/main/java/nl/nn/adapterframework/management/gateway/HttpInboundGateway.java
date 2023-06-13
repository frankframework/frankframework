/*
   Copyright 2023 WeAreFrank!

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
package nl.nn.adapterframework.management.gateway;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.integration.IntegrationPattern;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.http.inbound.HttpRequestHandlingMessagingGateway;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.web.filter.RequestContextFilter;

import lombok.Setter;
import nl.nn.adapterframework.lifecycle.DynamicRegistration;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.util.SpringUtils;
import nl.nn.adapterframework.util.StreamUtil;

public class HttpInboundGateway extends HttpServlet implements DynamicRegistration.Servlet, IntegrationPattern, InitializingBean, ApplicationContextAware {

	private static final long serialVersionUID = 1L;

	private final transient Logger log = LogManager.getLogger(HttpInboundGateway.class);

	private transient HttpRequestHandlingMessagingGateway gateway;
	private transient @Setter ApplicationContext applicationContext;

	@Value("${management.gateway.http.inbound.path:/iaf/management}")
	private transient String httpPath;

	@Override
	public void afterPropertiesSet() {
		if(applicationContext == null) {
			throw new IllegalStateException("no ApplicationContext set");
		}

		if(gateway == null) {
			addRequestContextFilter();
			createGateway();
		}
	}

	private void createGateway() {
		gateway = SpringUtils.createBean(applicationContext, HttpRequestHandlingMessagingGateway.class);
		gateway.setRequestChannel(getRequestChannel(applicationContext));
		gateway.setErrorChannel(getErrorChannel(applicationContext));
		gateway.setMessageConverters(getMessageConverters());
		gateway.setErrorOnTimeout(true);

		DefaultHttpHeaderMapper headerMapper = SpringUtils.createBean(applicationContext, DefaultHttpHeaderMapper.class);
		headerMapper.setInboundHeaderNames(getRequestHeaders());
		headerMapper.setOutboundHeaderNames(BusMessageUtils.HEADER_PREFIX_PATTERN);
		gateway.setHeaderMapper(headerMapper);

		gateway.start();
	}

	private String[] getRequestHeaders() {
		List<String> headers = new ArrayList<>();
		headers.add(BusAction.ACTION_HEADER_NAME);
		headers.add(BusTopic.TOPIC_HEADER_NAME);
		headers.add(BusMessageUtils.HEADER_PREFIX_PATTERN);
		return headers.toArray(new String[0]);
	}

	private void addRequestContextFilter() {
		ServletContext context = applicationContext.getBean("servletContext", ServletContext.class);
		FilterRegistration.Dynamic filter = context.addFilter("RequestContextFilter", RequestContextFilter.class);
		EnumSet<DispatcherType> dispatcherTypes = EnumSet.of(DispatcherType.REQUEST);
		filter.addMappingForServletNames(dispatcherTypes, false, getName());
	}

	private MessageChannel getRequestChannel(ApplicationContext applicationContext) {
		return applicationContext.getBean("frank-management-bus", MessageChannel.class);
	}

	private SubscribableChannel getErrorChannel(ApplicationContext applicationContext) {
		PublishSubscribeChannel channel = SpringUtils.createBean(applicationContext, PublishSubscribeChannel.class);
		channel.setBeanName("ErrorMessageConvertingChannel");
		ErrorMessageConverter errorConverter = SpringUtils.createBean(applicationContext, ErrorMessageConverter.class);
		if(channel.subscribe(errorConverter)) {
			log.info("created ErrorMessageConverter [{}]", errorConverter);
		} else {
			log.info("unable to create ErrorMessageConverter, all errors wil be ingored");
			gateway.setErrorOnTimeout(false);
		}
		return channel;
	}

	/**
	 * Reply converters to turn byte[] / InputStreams and Strings to something that the HTTP Inbound and Outbound gateways can understand.
	 */
	private List<HttpMessageConverter<?>> getMessageConverters() {
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
		StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter(StreamUtil.DEFAULT_CHARSET);
		stringHttpMessageConverter.setWriteAcceptCharset(false);
		messageConverters.add(stringHttpMessageConverter);
		messageConverters.add(new InputStreamHttpMessageConverter());
		messageConverters.add(new ByteArrayHttpMessageConverter());
		return messageConverters;
	}

	@Override
	public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		if(gateway == null) {
			throw new ServletException("no gateway configured");
		}

		gateway.handleRequest(req, res);
	}

	@Override
	public void destroy() {
		gateway.stop();
		super.destroy();
	}

	@Override
	public String getUrlMapping() {
		return httpPath;
	}

	@Override
	public String[] getAccessGrantingRoles() {
		return DynamicRegistration.ALL_IBIS_USER_ROLES;
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.inbound_gateway;
	}
}
