/*
   Copyright 2022-2025 WeAreFrank!

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
package org.frankframework.unmanaged;

import static org.frankframework.util.DateFormatUtils.FULL_GENERIC_FORMATTER;

import java.time.Instant;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.Nonnull;

import org.apache.logging.log4j.Logger;

import lombok.Setter;

import org.frankframework.core.Adapter;
import org.frankframework.lifecycle.events.AdapterMessageEvent;
import org.frankframework.lifecycle.events.MessageEventLevel;
import org.frankframework.util.LogUtil;
import org.frankframework.util.RunState;

public class PollGuard extends TimerTask {
	private final Logger log = LogUtil.getLogger(this);
	private @Setter SpringJmsConnector springJmsConnector;
	private long lastCheck;
	private long previousLastPollFinishedTime;
	private boolean timeoutDetected = false;

	private static final AtomicInteger pollTimeouts = new AtomicInteger();

	public PollGuard() {
		lastCheck = System.currentTimeMillis();
	}

	@Override
	public void run() {
		long lastPollFinishedTime = springJmsConnector.getLastPollFinishedTime();
		if (log.isTraceEnabled()) {
			log.trace("{} check last poll finished time {}", springJmsConnector::getLogPrefix, ()-> FULL_GENERIC_FORMATTER.format(Instant.ofEpochMilli(lastPollFinishedTime)));
		}
		long currentCheck = System.currentTimeMillis();
		if (lastPollFinishedTime < lastCheck) {												// if the last poll finished more than the pollGuardInterval seconds ago
			if (lastPollFinishedTime != previousLastPollFinishedTime						//   and we did not earlier check for this same value
				&& springJmsConnector.getThreadsProcessing().get() == 0						//   and we are not still processing a message
				&& springJmsConnector.getReceiver().getRunState() == RunState.STARTED		//   and we are ready to pro
				&& !springJmsConnector.getJmsContainer().isRecovering()) {					//   and we are not already in the process of recovering

				previousLastPollFinishedTime = lastPollFinishedTime;						// then we consider this too long, and suspect a problem.
				timeoutDetected = true;
				int pollTimeoutNr=pollTimeouts.incrementAndGet();
				logMessage("JMS poll timeout ["+pollTimeoutNr+"] last poll finished ["+((currentCheck-lastPollFinishedTime)/1000)+"] s ago, an attempt will be made to stop and start listener", MessageEventLevel.WARN);

				// Try to auto-recover the listener, when PollGuard detects `no activity` AND `threadsProcessing` == 0
				try {
					springJmsConnector.getReceiver().stop();
				} catch (Exception e) {
					log.warn(() -> "JMS poll timeout ["+pollTimeoutNr+"] handling caught Exception when stopping receiver ["+springJmsConnector.getListener().getReceiver().getName()+"]", e);
				} finally {
					log.warn("JMS poll timeout [{}] handling restarting receiver [{}]",
							pollTimeoutNr, springJmsConnector.getListener().getReceiver().getName());
					try {
						// Before restarting the receiver, update poll-finished time to current time so that
						// the PollGuard is not instantly triggered again.
						springJmsConnector.setLastPollFinishedTime(currentCheck);
						springJmsConnector.getReceiver().start();
						if (springJmsConnector.getReceiver().isInRunState(RunState.EXCEPTION_STARTING)) {
							logMessage("PollGuard: Failed to restart, no exception", MessageEventLevel.ERROR);
						} else {
							log.warn("JMS poll timeout [{}] handling restarted receiver [{}]",
									pollTimeoutNr, springJmsConnector.getListener().getReceiver().getName());
						}
					} catch (Exception e) {
						error("PollGuard: Error restarting receiver [" + springJmsConnector.getReceiver().getName() + "]", e);
					}
				}
			}
		} else {
			if (timeoutDetected) {
				timeoutDetected = false;
				logMessage("JMS poll timeout appears to be resolved, total number of timeouts detected ["+pollTimeouts.intValue()+"]", MessageEventLevel.WARN);
			}
		}
		lastCheck = currentCheck;
	}

	private void logMessage(String message, MessageEventLevel level) {
		log.warn("{}{}", springJmsConnector.getLogPrefix(), message);
		Adapter adapter = springJmsConnector.getReceiver().getAdapter();
		adapter.publishEvent(new AdapterMessageEvent(adapter, springJmsConnector.getReceiver(), message, level));
	}

	private void error(String message, @Nonnull Throwable t) {
		log.error(() -> "%s%s".formatted(springJmsConnector.getLogPrefix(), message), t);
		Adapter adapter = springJmsConnector.getReceiver().getAdapter();
		adapter.publishEvent(new AdapterMessageEvent(adapter, message, t));
	}
}
