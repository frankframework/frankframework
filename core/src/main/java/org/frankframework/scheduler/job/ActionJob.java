/*
   Copyright 2021-2025 WeAreFrank!

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
package org.frankframework.scheduler.job;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.doc.Mandatory;
import org.frankframework.management.Action;
import org.frankframework.scheduler.AbstractJobDef;
import org.frankframework.util.EnumUtils;

/**
 * Job which can stop/start adapters and receivers.
 *
 * {@inheritClassDoc}
 */
public class ActionJob extends AbstractJobDef {
	private @Getter String configurationName;
	private @Getter String adapterName;
	private @Getter String receiverName;
	private AvailableAction jobAction;
	private Action action;

	// Subset of the IbisAction enum as we do not want to expose all fields.
	public enum AvailableAction {
		STOPADAPTER,
		STARTADAPTER,
		STOPRECEIVER,
		STARTRECEIVER;
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		this.action = EnumUtils.parse(Action.class, "function", jobAction.name()); // Try and parse the Action as an IbisAction

		if (StringUtils.isEmpty(getAdapterName())) {
			throw new ConfigurationException("an adapterName must be specified");
		}
		if (!(getApplicationContext() instanceof Configuration configuration)) {
			throw new ConfigurationException("unable to determine configuration");
		}
		Adapter adapter = configuration.getRegisteredAdapter(getAdapterName());
		if (adapter == null) { // Make sure the adapter is registered in this configuration
			String msg = "Jobdef [" + getName() + "] got error: adapter [" + getAdapterName() + "] not registered.";
			throw new ConfigurationException(msg);
		}

		if (jobAction == AvailableAction.STOPRECEIVER || jobAction == AvailableAction.STARTRECEIVER) {
			if (StringUtils.isEmpty(getReceiverName())) {
				throw new ConfigurationException("a receiverName must be specified");
			}
			if (adapter.getReceiverByName(getReceiverName()) == null) {
				String msg = "Jobdef [" + getName() + "] got error: adapter [" + getAdapterName() + "] receiver [" + getReceiverName() + "] not registered.";
				throw new ConfigurationException(msg);
			}
		}
	}

	@Override
	public void execute() {
		getIbisManager().handleAction(action, getConfigurationName(), getAdapterName(), getReceiverName(), "scheduled job ["+getName()+"]", true);
	}

	@Mandatory
	public void setAction(AvailableAction action) {
		this.jobAction = action;
	}

	/** Configuration on which job operates */
	public void setConfigurationName(String configurationName) {
		this.configurationName = configurationName;
	}

	/** Adapter on which job operates */
	@Mandatory
	public void setAdapterName(String adapterName) {
		this.adapterName = adapterName;
	}

	/** Receiver on which job operates */
	public void setReceiverName(String receiverName) {
		this.receiverName = receiverName;
	}
}
