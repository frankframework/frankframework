/*
   Copyright 2021 WeAreFrank!

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

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.scheduler.JobDef;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.RunStateEnum;

public class RecoverAdaptersJob extends JobDef {
	protected Logger heartbeatLog = LogUtil.getLogger("HEARTBEAT");

	@Override
	public void execute(IbisManager ibisManager) {
		int countAdapter=0;
		int countAdapterStateStarted=0;
		int countReceiver=0;
		int countReceiverStateStarted=0;
		for (Adapter adapter: ibisManager.getRegisteredAdapters()) {
			countAdapter++;
			RunStateEnum adapterRunState = adapter.getRunState();
			boolean startAdapter = false;
			if (adapterRunState.equals(RunStateEnum.ERROR)) { //if not previously configured, there is no point in trying to do this again.
				log.debug("trying to recover adapter [" + adapter.getName() + "]");

				if (!adapter.configurationSucceeded()) { //This should only happen once, so only try to (re-)configure if it failed in the first place!
					try {
						adapter.configure();
					} catch (ConfigurationException e) {
						// log the warning and do nothing, it couldn't configure before, it still can't...
						log.warn("error configuring adapter [" + adapter.getName() + "] while trying to recover", e);
					} 
				}

				if (adapter.configurationSucceeded()) {
					startAdapter = adapter.isAutoStart(); // if configure has succeeded and adapter was in state ERROR try to auto (re-)start the adapter
				}

				log.debug("finished recovering adapter [" + adapter.getName() + "]");
			}

			String message = "adapter [" + adapter.getName() + "] has state [" + adapterRunState + "]";
			if (adapterRunState.equals(RunStateEnum.STARTED)) {
				countAdapterStateStarted++;
				heartbeatLog.info(message);
			} else if (adapterRunState.equals(RunStateEnum.ERROR)) {
				heartbeatLog.error(message);
			} else {
				heartbeatLog.warn(message);
			}

			for (Receiver<?> receiver: adapter.getReceivers()) {
				countReceiver++;

				RunStateEnum receiverRunState = receiver.getRunState();
				if (adapterRunState.equals(RunStateEnum.STARTED) && receiverRunState.equals(RunStateEnum.ERROR) && receiver.configurationSucceeded()) { //Only try to (re-)start receivers in a running adapter. Receiver configure is done in Adapter.configure
					log.debug("trying to recover receiver [" + receiver.getName() + "] of adapter [" + adapter.getName() + "]");

					receiver.startRunning();

					log.debug("finished recovering receiver [" + receiver.getName() + "] of adapter [" + adapter.getName() + "]");
				}

				receiverRunState = receiver.getRunState();
				message = "receiver [" + receiver.getName() + "] of adapter [" + adapter.getName() + "] has state [" + receiverRunState + "]";
				if (receiverRunState.equals(RunStateEnum.STARTED)) {
					countReceiverStateStarted++;
					heartbeatLog.info(message);
				} else if (receiverRunState.equals(RunStateEnum.ERROR)) {
					heartbeatLog.error(message);
				} else {
					heartbeatLog.warn(message);
				}
			}

			if (startAdapter) { // can only be true if adapter was in error before and AutoStart is enabled
				adapter.startRunning(); //ASync startup can still cause the Adapter to end up in an ERROR state
			}
		}
		heartbeatLog.info("[" + countAdapterStateStarted + "/" + countAdapter + "] adapters and [" + countReceiverStateStarted + "/" + countReceiver + "] receivers have state [" + RunStateEnum.STARTED + "]");
	}
}
