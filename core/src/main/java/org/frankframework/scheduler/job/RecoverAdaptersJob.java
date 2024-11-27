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
package org.frankframework.scheduler.job;

import org.apache.logging.log4j.Logger;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.IbisManager;
import org.frankframework.core.Adapter;
import org.frankframework.receivers.Receiver;
import org.frankframework.scheduler.AbstractJobDef;
import org.frankframework.util.LogUtil;
import org.frankframework.util.RunState;

/**
 * Frank!Framework Adapter recovery-job, which monitors all adapter states, attempts to recover them if required,
 * and logs this information to the {@code HEARTBEAT} log appender.
 * 
 * @ff.info This is a default job that can be controlled with the property {@literal recover.adapters.interval}.
 */
public class RecoverAdaptersJob extends AbstractJobDef {
	protected Logger heartbeatLog = LogUtil.getLogger("HEARTBEAT");

	@Override
	public void execute() {
		int countAdapter=0;
		int countAdapterStateStarted=0;
		int countReceiver=0;
		int countReceiverStateStarted=0;
		IbisManager ibisManager = getIbisManager();
		for (Configuration configuration : ibisManager.getConfigurations()) {
			if(configuration.isActive()) {
				for (Adapter adapter: configuration.getRegisteredAdapters()) {
					countAdapter++;
					RunState adapterRunState = adapter.getRunState();
					boolean startAdapter = false;
					if (adapterRunState==RunState.ERROR) { // If not previously configured, there is no point in trying to do this again.
						log.debug("trying to recover adapter [{}]", adapter::getName);

						if (!adapter.configurationSucceeded()) { // This should only happen once, so only try to (re-)configure if it failed in the first place!
							try {
								adapter.configure();
							} catch (ConfigurationException e) {
								// log the warning and do nothing, it couldn't configure before, it still can't...
								log.warn("error configuring adapter [{}] while trying to recover", adapter.getName(), e);
							}
						}

						if (adapter.configurationSucceeded()) {
							startAdapter = adapter.isAutoStart(); // if configure has succeeded and adapter was in state ERROR try to auto (re-)start the adapter
						}

						log.debug("finished recovering adapter[{}]", adapter::getName);
					}

					String message = "adapter [" + adapter.getName() + "] has state [" + adapterRunState + "]";
					if (adapterRunState==RunState.STARTED) {
						countAdapterStateStarted++;
						heartbeatLog.info(message);
					} else if (adapterRunState==RunState.ERROR) {
						heartbeatLog.error(message);
					} else {
						heartbeatLog.warn(message);
					}

					for (Receiver<?> receiver: adapter.getReceivers()) {
						countReceiver++;

						RunState receiverRunState = receiver.getRunState();
						// Only try to (re-)start receivers in a running adapter. Receiver configure is done in Adapter.configure
						if (adapterRunState==RunState.STARTED && (receiverRunState==RunState.ERROR || receiverRunState==RunState.EXCEPTION_STARTING) && receiver.configurationSucceeded()) {
							log.debug("trying to recover receiver [{}] of adapter [{}]", receiver::getName, adapter::getName);

							receiver.startRunning();

							log.debug("finished recovering receiver [{}] of adapter [{}]", receiver::getName, adapter::getName);
						}

						receiverRunState = receiver.getRunState();
						message = "receiver [" + receiver.getName() + "] of adapter [" + adapter.getName() + "] has state [" + receiverRunState + "]";
						if (receiverRunState==RunState.STARTED) {
							countReceiverStateStarted++;
							heartbeatLog.info(message);
						} else if (receiverRunState==RunState.ERROR) {
							heartbeatLog.error(message);
						} else {
							heartbeatLog.warn(message);
						}
					}

					if (startAdapter) { // Can only be true if adapter was in error before and AutoStart is enabled
						adapter.startRunning(); // ASync startup can still cause the Adapter to end up in an ERROR state
					}
				}
			}
		}
		heartbeatLog.info("[{}/{}] adapters and [{}/{}] receivers have state [{}]", countAdapterStateStarted, countAdapter, countReceiverStateStarted, countReceiver, RunState.STARTED);
	}

}
