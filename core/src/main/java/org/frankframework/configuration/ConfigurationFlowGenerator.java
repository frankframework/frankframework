package org.frankframework.configuration;

import org.frankframework.lifecycle.ConfigurableLifecycle;
import org.frankframework.util.flow.FlowDiagramManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;

/**
 * Generate a flow over the digested {@link Configuration}.
 * Uses {@link Configuration#getLoadedConfiguration()}.
 */
@Log4j2
@Order(Ordered.LOWEST_PRECEDENCE)
public class ConfigurationFlowGenerator implements ConfigurableLifecycle, ApplicationContextAware {

	@Autowired
	private FlowDiagramManager flowDiagramManager;

	@Setter
	private ApplicationContext applicationContext;

	@Override
	public int getPhase() {
		return 0;
	}

	@Override
	public void start() {
		//Do nothing
	}

	@Override
	public void stop() {
		//Do nothing
	}

	@Override
	public boolean isRunning() {
		return false;
	}

	@Override
	public void configure() {
		if(!(applicationContext instanceof Configuration configuration)) {
			throw new IllegalStateException("no suitable Configuration found");
		}

		try {
			flowDiagramManager.generate(configuration);
		} catch (Exception e) { //Don't throw an exception when generating the flow fails
			ConfigurationWarnings.add(configuration, log, "Error generating flow diagram for configuration ["+configuration.getName()+"]", e);
		}
	}
}
