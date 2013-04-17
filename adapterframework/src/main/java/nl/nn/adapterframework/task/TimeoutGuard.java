/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.task;

import java.util.Timer;
import java.util.TimerTask;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * TimeoutGuard interrupts running thread when timeout is exceeded.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9.10  
 * @version $Id$
 */
public class TimeoutGuard {
	protected Logger log = LogUtil.getLogger(this);

	int timeout;
	String description;
	boolean threadKilled;

	private Timer timer;
	
	private class Killer extends TimerTask {

		private Thread thread;
		
		public Killer() {
			super();
			thread = Thread.currentThread();
		}

		public void run() {
			log.warn("Thread ["+thread.getName()+"] executing task ["+description+"] exceeds timeout of ["+timeout+"] s, interuppting");
			threadKilled=true;
			thread.interrupt();
		}
	}

	public TimeoutGuard(String description) {
		super();
		this.description=description;
	}
	
	public TimeoutGuard(int timeout, String description) {
		this(description);
		activateGuard(timeout);
	}
	
	public void activateGuard(int timeout) {
		if (timeout > 0) {
			this.timeout=timeout;
			if (log.isDebugEnabled()) log.debug("setting timeout of ["+timeout+"] s for task ["+description+"]");
			timer= new Timer();
			timer.schedule(new Killer(),timeout*1000);
		}
	}

//	public void activateGuard(Date runMaxUntil) {
//		if (log.isDebugEnabled()) log.debug("setting timeout at ["+DateUtils.format(runMaxUntil)+"] for task ["+description+"]");
//		timer= new Timer();
//		timer.schedule(new Killer(),runMaxUntil);
//	}
	
	/**
	 * cancels timer, and returns true if thread has been killed by this guard or interrupted by another.
	 */
	public boolean cancel() {	
		if (timer!=null) {
			if (log.isDebugEnabled()) log.debug("deactivating TimeoutGuard for task ["+description+"]");
			timer.cancel();	
		}
		return Thread.interrupted() || threadKilled; 
	}

	public String getDescription() {
		return description;
	}
	public void setDescription(String string) {
		description = string;
	}

	public boolean threadKilled() {
		return threadKilled;
	}
}
