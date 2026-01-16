package org.frankframework.ladybug.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class LadybugApplicationListener implements ApplicationListener<ContextRefreshedEvent>, ApplicationContextAware {
	private static final Logger APPLICATION_LOG = LogManager.getLogger("APPLICATION");
	private ApplicationContext ctx;
	@Override
	public void setApplicationContext(ApplicationContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		LadybugAccessDeniedHandler handler = ctx.getBean(LadybugAccessDeniedHandler.class);
		HandlerExceptionResolver resolver = ctx.getBean(HandlerExceptionResolver.class);
		APPLICATION_LOG.error("LadybugApplicationListener.onApplicationEvent(): Setting HandlerExceptionResolver in LadybugAccessDeniedHandler");
		handler.setHandlerExceptionResolver(resolver);
	}
}
