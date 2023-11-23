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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.HttpConstraintElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
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
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.support.HttpRequestHandlerServlet;
import org.springframework.web.filter.RequestContextFilter;

import lombok.Setter;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.security.JwtSecurityFilter;
import nl.nn.adapterframework.util.SpringUtils;
import nl.nn.adapterframework.util.StreamUtil;

@Order(Ordered.LOWEST_PRECEDENCE-1)
public class HttpInboundGateway implements WebSecurityConfigurer<WebSecurity>, ServletContextAware, IntegrationPattern, InitializingBean, ApplicationContextAware, BeanFactoryAware {
	private static final String HTTP_SECURITY_BEAN_NAME = "org.springframework.security.config.annotation.web.configuration.HttpSecurityConfiguration.httpSecurity";

	private static final String SERVLET_NAME = "HttpInboundGatewayServlet";

	private final Logger log = LogManager.getLogger(HttpInboundGateway.class);

	private HttpRequestHandlingMessagingGateway gateway;
	private @Setter ApplicationContext applicationContext;
	private @Setter BeanFactory beanFactory;
	private @Setter ServletContext servletContext;

	@Value("${management.gateway.http.inbound.path:/iaf/management}")
	private String httpPath;

	@Override
	public void afterPropertiesSet() {
		if(applicationContext == null) {
			throw new IllegalStateException("no ApplicationContext set");
		}

		if(gateway == null) {
			createGateway();
			createGatewayEndpoint();
		}
	}

	private void createGateway() {
		gateway = SpringUtils.createBean(applicationContext, HttpRequestHandlingMessagingGateway.class);
		gateway.setRequestChannel(getRequestChannel(applicationContext));
		gateway.setErrorChannel(getErrorChannel(applicationContext));
		gateway.setMessageConverters(getMessageConverters());
		gateway.setErrorOnTimeout(false);
		gateway.setRequestTimeout(0L);
		gateway.setReplyTimeout(0L);

		DefaultHttpHeaderMapper headerMapper = SpringUtils.createBean(applicationContext, DefaultHttpHeaderMapper.class);
		headerMapper.setInboundHeaderNames(getRequestHeaders());
		headerMapper.setOutboundHeaderNames(BusMessageUtils.HEADER_PREFIX_PATTERN);
		gateway.setHeaderMapper(headerMapper);

		ConfigurableBeanFactory cbf = (ConfigurableBeanFactory) this.beanFactory;
		cbf.registerSingleton(SERVLET_NAME, gateway);
	}

	public void createGatewayEndpoint() {
		HttpRequestHandlerServlet servlet = new HttpRequestHandlerServlet(); //name of this servlet must match the inbound gateway name

		log.info("created management service endpoint [{}]", httpPath);

		ServletRegistration.Dynamic serv = servletContext.addServlet(SERVLET_NAME, servlet);
		serv.setLoadOnStartup(-1);
		serv.addMapping(httpPath);

		HttpConstraintElement httpConstraintElement = new HttpConstraintElement(TransportGuarantee.NONE);
		serv.setServletSecurity(new ServletSecurityElement(httpConstraintElement));
	}

	private String[] getRequestHeaders() {
		List<String> headers = new ArrayList<>();
		headers.add(BusAction.ACTION_HEADER_NAME);
		headers.add(BusTopic.TOPIC_HEADER_NAME);
		headers.add(BusMessageUtils.HEADER_PREFIX_PATTERN);
		return headers.toArray(new String[0]);
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
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.inbound_gateway;
	}

	@Override
	public void init(WebSecurity builder) {
		// Nothing to init
	}

	@Override
	public void configure(WebSecurity builder) {
		builder.addSecurityFilterChainBuilder(this::createSecurityFilterChain);
	}

	private SecurityFilterChain createSecurityFilterChain() {
		HttpSecurity httpSecurityConfigurer = applicationContext.getBean(HTTP_SECURITY_BEAN_NAME, HttpSecurity.class);
		return configureHttpSecurity(httpSecurityConfigurer);
	}

	private SecurityFilterChain configureHttpSecurity(HttpSecurity http) {
		try {
			//Apply defaults to disable bloated filters, see DefaultSecurityFilterChain.getFilters for the actual list.
			http.headers().frameOptions().sameOrigin(); //Allow same origin iframe request
			http.csrf().disable();
			http.securityMatcher(new AntPathRequestMatcher(httpPath));
			http.formLogin().disable(); //Disable the form login filter
			http.anonymous().disable(); //Disable the default anonymous filter
			http.logout().disable(); //Disable the logout endpoint on every filter
			http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
			http.authorizeHttpRequests().anyRequest().authenticated();

			Filter requestDispatcher = SpringUtils.createBean(applicationContext, RequestContextFilter.class);
			http.addFilterAfter(requestDispatcher, AuthorizationFilter.class);

			JwtSecurityFilter securityFilter = SpringUtils.createBean(applicationContext, JwtSecurityFilter.class);
			http.addFilterBefore(securityFilter, BasicAuthenticationFilter.class);

			return http.build();
		} catch (Exception e) {
			throw new IllegalStateException("unable to configure Spring Security", e);
		}
	}
}
