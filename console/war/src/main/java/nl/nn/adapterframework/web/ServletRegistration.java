package nl.nn.adapterframework.web;

import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import nl.nn.adapterframework.lifecycle.DynamicRegistration.ServletWithParameters;
import nl.nn.adapterframework.lifecycle.servlets.ServletConfiguration;
import nl.nn.adapterframework.util.SpringUtils;

@Log4j2
public class ServletRegistration extends ServletRegistrationBean<ServletWithParameters> implements ApplicationContextAware, InitializingBean {
	private @Setter ApplicationContext applicationContext;
	private @Getter ServletConfiguration servletConfiguration;
	private final Class<?> servletClass;

	public <T extends ServletWithParameters> ServletRegistration(Class<T> servletClass) {
		this.servletClass = servletClass;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		ServletWithParameters servlet = (ServletWithParameters) SpringUtils.createBean(applicationContext, servletClass);
		servletConfiguration = SpringUtils.createBean(applicationContext, ServletConfiguration.class);
		log.info("registering servlet [{}]", servlet::getName);
		servletConfiguration.loadDefaultsFromServlet(servlet);
		servletConfiguration.loadProperties();

		Map<String, String> initParams = servletConfiguration.getInitParameters();
		for(Map.Entry<String, String> entry : initParams.entrySet()) {
			String key = entry.getKey();
			String val = entry.getValue();
			addInitParameter(key, val);
		}
		setName(servletConfiguration.getName());
		setUrlMappings(servletConfiguration.getUrlMapping());
		super.setServlet(servlet);

		log.info("created servlet {} endpoint {}", this::getServletName, this::getUrlMappings);
	}
}
