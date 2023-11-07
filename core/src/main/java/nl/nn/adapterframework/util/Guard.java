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
package nl.nn.adapterframework.util;

import nl.nn.adapterframework.core.TimeoutException;

/**
 * A Guard is the counterpart of the {@link Semaphore} that waits till all resources have been released.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class Guard {

	private int counter;

	public Guard() {
		this(0);
	}

	public Guard(int numberOfResourcesToWaitFor) {
		if(numberOfResourcesToWaitFor < 0) throw new IllegalArgumentException(numberOfResourcesToWaitFor + " < 0");
		counter = numberOfResourcesToWaitFor;
	}

	/**
	 * Wait for the counter to get zero.
	 *
	 * @exception InterruptedException passed from this.wait().
	 */
	public synchronized void waitForAllResources() throws InterruptedException {
		while(counter != 0) {
			this.wait();
		}
	}

	/**
	 * Wait for the counter to get zero.
	 *
	 * @exception InterruptedException passed from this.wait().
	 * @exception TimeoutException if the time specified has passed, but the counter did not reach zero.
	 */
	public synchronized void waitForAllResources(long timeout) throws InterruptedException, TimeoutException {
		while (counter != 0) {
			this.wait(timeout);
		}
		if (counter!=0) {
			throw new TimeoutException("Timeout of ["+timeout+"] ms expired");
		}
	}

	public synchronized void addResource() {
		counter++;
	}

	/**
	 * decrements internal counter, possibly awakening the thread waiting for
	 * release wait()ing in acquire()
	 */
	public synchronized void releaseResource() {
		counter--;
		if(counter == 0) {
			this.notify();
		}
	}

	public synchronized boolean isReleased() {
		return counter == 0;
	}
}
