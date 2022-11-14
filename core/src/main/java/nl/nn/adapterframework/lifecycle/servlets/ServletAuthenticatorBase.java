package nl.nn.adapterframework.lifecycle.servlets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import lombok.Getter;
import nl.nn.adapterframework.lifecycle.ServletManager;
import nl.nn.adapterframework.util.LogUtil;

public abstract class ServletAuthenticatorBase implements IAuthenticator, ApplicationContextAware {
	private static final String HTTP_SECURITY_BEAN_NAME = "org.springframework.security.config.annotation.web.configuration.HttpSecurityConfiguration.httpSecurity";

	protected Logger log = LogUtil.getLogger(this);

	private @Getter ApplicationContext applicationContext;
	private @Getter Set<String> endpoints = new HashSet<>();
	private @Getter List<String> securityRoles = new ArrayList<>();

	@Override
	public final void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public final void registerServlet(ServletConfiguration config) {
		setEndpoints(config.getUrlMapping());
		setSecurityRoles(config.getSecurityRoles());
	}

	private void setSecurityRoles(List<String> securityRoles) {
		if(securityRoles == null || securityRoles.isEmpty()) {
			securityRoles = ServletManager.DEFAULT_IBIS_ROLES; //TODO make it so when you specify no roles it disables authorization
		}
		this.securityRoles.addAll(securityRoles);
	}

	private void setEndpoints(List<String> urlMappings) {
		for(String url : urlMappings) {
			String matcherUrl = url;
			if(url.endsWith("*")) {
				matcherUrl = url+"*";
			}

			if(endpoints.contains(matcherUrl)) {
				throw new IllegalStateException("endpoint already configured");
			}

			log.info("registering url [{}] with lookup pattern [{}]", url, matcherUrl);
			endpoints.add(matcherUrl);
		}
	}

	@Override
	public void build() {
		if(applicationContext == null) {
			throw new IllegalStateException("Authenticator is not wired through local BeanFactory");
		}
		if(endpoints.isEmpty()) { //No servlets registered so no need to build/enable this Authenticator
			log.info("no url matchers found, ignoring Authenticator [{}]", this::getClass);
			return;
		}

		ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext)applicationContext).getBeanFactory();
		String name = "HttpSecurityChain-"+this.getClass().getSimpleName();

		//Register the SecurityFilter in the (WebXml)BeanFactory so the WebSecurityConfiguration can configure them
		beanFactory.registerSingleton(name, createSecurityFilterChain());
	}

	private SecurityFilterChain createSecurityFilterChain() {
		HttpSecurity httpSecurityConfigurer = applicationContext.getBean(HTTP_SECURITY_BEAN_NAME, HttpSecurity.class);
		return configureHttpSecurity(httpSecurityConfigurer);
	}

	private SecurityFilterChain configureHttpSecurity(HttpSecurity http) {
		try {
			http.headers().frameOptions().sameOrigin();
			http.csrf().disable();
			http.requestMatcher(getRequestMatcher());
			http.formLogin().disable();

			return configure(http);
		} catch (Exception e) {
			throw new IllegalStateException("unable to configure Spring Security", e);
		}
	}

	/** Before building it configures the Chain. */
	protected abstract SecurityFilterChain configure(HttpSecurity http) throws Exception;

	private RequestMatcher getRequestMatcher() {
		List<RequestMatcher> requestMatchers = new ArrayList<>();
		for(String url : endpoints) {
			requestMatchers.add(new AntPathRequestMatcher(url, null, false));
		}
		return (requestMatchers.size() == 1) ? requestMatchers.get(0) : new OrRequestMatcher(requestMatchers);
	}
}
