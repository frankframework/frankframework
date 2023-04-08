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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.http.inbound.HttpRequestHandlingMessagingGateway;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.web.filter.RequestContextFilter;

import lombok.Setter;
import nl.nn.adapterframework.lifecycle.DynamicRegistration;
import nl.nn.adapterframework.lifecycle.IbisInitializer;
import nl.nn.adapterframework.lifecycle.ServletManager;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.SpringUtils;
import nl.nn.adapterframework.util.StreamUtil;

@IbisInitializer
public class HttpInboundGateway extends HttpServlet implements DynamicRegistration.Servlet, InitializingBean, ApplicationContextAware {

	private static final long serialVersionUID = 1L;
	private final transient Logger log = LogManager.getLogger(HttpInboundGateway.class);

	private final String httpPath = AppConstants.getInstance().getString("management.http.path", "/iaf/management");
	private transient HttpRequestHandlingMessagingGateway gateway;
	@Setter private transient ApplicationContext applicationContext;

	@Override
	public void afterPropertiesSet() {
		if(gateway == null) {
			addRequestContextFilter();

			gateway = SpringUtils.createBean(applicationContext, HttpRequestHandlingMessagingGateway.class);
			MessageChannel requestChannel = applicationContext.getBean("frank-management-bus", MessageChannel.class);
			gateway.setRequestChannel(requestChannel);

			gateway.setErrorChannel(createErrorChannel());

			DefaultHttpHeaderMapper headerMapper = SpringUtils.createBean(applicationContext, DefaultHttpHeaderMapper.class);
			headerMapper.setInboundHeaderNames("topic", "action");
			headerMapper.setOutboundHeaderNames("meta-*");
			gateway.setHeaderMapper(headerMapper);
			gateway.setErrorOnTimeout(true);


			List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
			StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter(StreamUtil.DEFAULT_CHARSET);
			stringHttpMessageConverter.setWriteAcceptCharset(false);
			messageConverters.add(stringHttpMessageConverter);
			messageConverters.add(new InputStreamHttpMessageConverter());
			messageConverters.add(new ByteArrayHttpMessageConverter());
			gateway.setMessageConverters(messageConverters);

			gateway.start();
		}
	}

	private void addRequestContextFilter() {
		ServletContext context = applicationContext.getBean("servletContext", ServletContext.class);
		FilterRegistration.Dynamic filter = context.addFilter("RequestContextFilter", RequestContextFilter.class);
		EnumSet<DispatcherType> dispatcherTypes = EnumSet.of(DispatcherType.REQUEST);
		filter.addMappingForServletNames(dispatcherTypes, false, getName());
	}

	private SubscribableChannel createErrorChannel() {
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

	@Autowired
	public void setServletManager(ServletManager servletManager) {
		servletManager.register(this);
	}
}
