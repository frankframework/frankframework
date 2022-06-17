/*
   Copyright 2022 WeAreFrank!
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
package nl.nn.adapterframework.unmanaged;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.RunState;
import nl.nn.adapterframework.util.MessageKeeper.MessageKeeperLevel;

class PollGuard extends TimerTask {
	private Logger log = LogUtil.getLogger(this);
	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DateUtils.FORMAT_FULL_GENERIC);
	private SpringJmsConnector springJmsConnector;
	private long lastCheck;
	private long previousLastPollFinishedTime;
	private boolean timeoutDetected = false;

	private static AtomicInteger pollTimeouts = new AtomicInteger();
	
	PollGuard() {
		lastCheck = System.currentTimeMillis();
	}

	public void setSpringJmsConnector(SpringJmsConnector springJmsConnector) {
		this.springJmsConnector = springJmsConnector;
	}

	@Override
	public void run() {
		long lastPollFinishedTime = springJmsConnector.getLastPollFinishedTime();
		if (log.isTraceEnabled()) {
			log.trace(springJmsConnector.getLogPrefix() + "check last poll finished time " + simpleDateFormat.format(new Date(lastPollFinishedTime)));
		}
		long currentCheck = System.currentTimeMillis();
		if (lastPollFinishedTime < lastCheck) {
 			if (lastPollFinishedTime != previousLastPollFinishedTime
				&& springJmsConnector.threadsProcessing.getValue() == 0
				&& springJmsConnector.getReceiver().getRunState() == RunState.STARTED
				&& !springJmsConnector.getJmsContainer().isRecovering()) {
 				previousLastPollFinishedTime = lastPollFinishedTime;
 				timeoutDetected = true;
 				warn("JMS poll timeout ["+pollTimeouts.incrementAndGet()+"] last poll finished ["+((currentCheck-lastPollFinishedTime)/1000)+"] s ago, an attempt will be made to stop and start listener");

 				// Try to auto-recover the listener, when PollGuard detects `no activity` AND `threadsProcessing` == 0
 				springJmsConnector.getListener().getReceiver().stopRunning();
 				springJmsConnector.getListener().getReceiver().startRunning();
 			}
		} else {
			if (timeoutDetected) {
				timeoutDetected = false;
				warn("JMS poll timeout appears to be resolved, total number of timeouts detected ["+pollTimeouts.intValue()+"]");
			}
		}
		lastCheck = currentCheck;
	}

	private void warn(String message) {
		log.warn(springJmsConnector.getLogPrefix() + message);
		springJmsConnector.getReceiver().getAdapter().getMessageKeeper().add(message, MessageKeeperLevel.WARN);
	}

}
