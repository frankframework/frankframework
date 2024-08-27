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
package org.frankframework.configuration;

import org.frankframework.lifecycle.ConfigurableLifecycle;
import org.frankframework.util.flow.FlowDiagramManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;

/**
 * Generate a flow over the digested {@link Configuration}.
 * Uses {@link Configuration#getLoadedConfiguration()}.
 */
@Log4j2
public class ConfigurationFlowGenerator implements ConfigurableLifecycle, ApplicationContextAware {

	@Setter
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
