/*
   Copyright 2013, 2020 Nationale-Nederlanden

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
package org.frankframework.task;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.util.LogUtil;

/**
 * TimeoutGuard interrupts running thread when timeout is exceeded.
 *
 * @author  Gerrit van Brakel
 * @since   4.9.10
 */
public class TimeoutGuard {
	protected Logger log = LogUtil.getLogger(this);

	int timeout;
	@Getter @Setter String description;
	boolean threadKilled;
	private final Exception source = new Exception("TimeoutGuard created from source point");
	private Timer timer;

	private class Killer extends TimerTask {

		private final Thread timeoutThread;

		public Killer() {
			super();
			timeoutThread = Thread.currentThread();
		}

		@Override
		public void run() {
			log.warn("Thread [{}] executing task [{}] exceeds timeout of [{}] s, interrupting.",
				timeoutThread.getName(), description, timeout);
			if (log.isDebugEnabled()) {
				log.debug("Thread [{}] executing task [{}] created from source point:",
						timeoutThread.getName(), description, source);
				log.debug("Execution stack for thread [{}] executing task [{}]:",
						timeoutThread.getName(), description, new Exception("Interrupted at point"));
			}
			threadKilled=true;
			timeoutThread.interrupt();
			abort();
		}
	}

	/**
	 * Create a new TimeoutGuard
	 * @param description name of the guard
	 */
	public TimeoutGuard(String description) {
		super();
		this.description=description;
	}

	/**
	 * Create a new TimeoutGuard and activate immediately
	 * @param timeout in seconds
	 * @param description name of the guard
	 */
	public TimeoutGuard(int timeout, String description) {
		this(description);
		activateGuard(timeout);
	}

	/**
	 * Sets and activates the timeout
	 * @param timeout in seconds
	 */
	public void activateGuard(int timeout) {
		if (timeout > 0) {
			this.timeout=timeout;
			log.debug("setting timeout of [{}s] for task [{}]", timeout, description);
			timer = new Timer("GuardTask["+description+"]");
			timer.schedule(new Killer(),timeout*1000L);
		}
	}

	/**
	 * Cancels timer, and returns true if thread has been killed by this guard or interrupted by another.
	 * <br/><br/>
	 * Call this in a finally-block to verify correct execution of the guarded process or if it has been interrupted by the guard.
	 */
	public boolean cancel() {
		if (timer!=null) {
			log.debug("deactivating TimeoutGuard for task [{}]", description);
			timer.cancel();
		}
		return Thread.interrupted() || threadKilled;
	}

	/**
	 * Implement this method to stop the process and cleanup the resources you are 'guarding'.
	 */
	protected void abort() {
		// can be called in descendants to kill the guarded job when timeout is exceeded.
	}

	public boolean threadKilled() {
		return threadKilled;
	}
}
