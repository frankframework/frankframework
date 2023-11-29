/*
   Copyright 2021, 2022 WeAreFrank!

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
package nl.nn.adapterframework.scheduler.job;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.AdapterManager;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.doc.Mandatory;
import nl.nn.adapterframework.management.IbisAction;
import nl.nn.adapterframework.scheduler.JobDef;
import nl.nn.adapterframework.util.EnumUtils;

public class IbisActionJob extends JobDef {
	private @Getter @Setter AdapterManager adapterManager;
	private @Getter String configurationName;
	private @Getter String adapterName;
	private @Getter String receiverName;
	private Action jobAction;
	private IbisAction ibisAction;

	// Subset of the IbisAction enum as we do not want to expose all fields.
	public enum Action {
		STOPADAPTER,
		STARTADAPTER,
		STOPRECEIVER,
		STARTRECEIVER;
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		this.ibisAction = EnumUtils.parse(IbisAction.class, "function", jobAction.name()); // Try and parse the Action as an IbisAction

		if (StringUtils.isEmpty(getAdapterName())) {
			throw new ConfigurationException("an adapterName must be specified");
		}
		Adapter adapter = adapterManager.getAdapter(getAdapterName());
		if(adapter == null) { //Make sure the adapter is registered in this configuration
			String msg="Jobdef [" + getName() + "] got error: adapter [" + getAdapterName() + "] not registered.";
			throw new ConfigurationException(msg);
		}

		if (jobAction == Action.STOPRECEIVER || jobAction == Action.STARTRECEIVER) {
			if (StringUtils.isEmpty(getReceiverName())) {
				throw new ConfigurationException("a receiverName must be specified");
			}
			if (adapter.getReceiverByName(getReceiverName()) == null) {
				String msg="Jobdef [" + getName() + "] got error: adapter [" + getAdapterName() + "] receiver ["+getReceiverName()+"] not registered.";
				throw new ConfigurationException(msg);
			}
		}
	}

	@Override
	public void execute() {
		getIbisManager().handleAction(ibisAction, getConfigurationName(), getAdapterName(), getReceiverName(), "scheduled job ["+getName()+"]", true);
	}

	@Mandatory
	public void setAction(Action action) {
		this.jobAction = action;
	}

	/** Configuration on which job operates */
	public void setConfigurationName(String configurationName) {
		this.configurationName = configurationName;
	}

	/** Adapter on which job operates
	 * @ff.mandatory
	 */
	public void setAdapterName(String adapterName) {
		this.adapterName = adapterName;
	}

	/** Receiver on which job operates */
	public void setReceiverName(String receiverName) {
		this.receiverName = receiverName;
	}
}
