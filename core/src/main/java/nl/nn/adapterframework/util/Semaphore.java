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

/**
 * A semaphore is a flag used to check whether a resource is currently being 
 * used by another thread or process and wait for the other process to release it.
 * <p>For example, if a process wants to use the printer,
 * it first needs to make sure the printer is available by checking to see if the semaphore
 * has been set. If it is set, it needs to wait to the process that currently has it is finished. however, 
 * If the printer were free, the process would set the semaphore and start using the printer, blocking 
 * access to all other processes until it finished. <p>
 * <p>Semphores are a classical technique for protecting critical sections of code from being
 * simultaneously executed by more than one thread. A semaphore is a generalisation of a monitor.
 * A monitor allows only one thread to lock an object at once. A semaphore allows N processes. </p> 
 * <p>The process of grabbing a semaphore for semi-exclusive use is called downing the semaphore because they are 
 * implemented with a countdown integer that decrements for each lock and increments for each unlock.
 * If a semaphore is fully occupied, new threads wanting to use it will wait until some thread releases its 
 * lock by upping the semaphore. For a semaphore to work, the check for full, and the decrement must be done 
 * all in one atomic uninterruptible instruction. This is done by the {@link #release()} method.</p>
 *
 * @author Gerrit van Brakel
 */
public class Semaphore {
	private int counter;

	public Semaphore() {
		this(0);
	}

	public Semaphore(int i) {
		if(i < 0)
			throw new IllegalArgumentException(i + " < 0");
		counter = i;
	}

	/**
	 * Decrements internal counter, blocking if the counter is already zero or less.
	 * @exception InterruptedException passed from this.wait().
	 */
	public synchronized void acquire() throws InterruptedException {
		while(counter <= 0) {
			this.wait();
		}
		counter--;
	}

	/**
	 * non blocking decrements internal counter.
	 */
	public synchronized void tighten() {
		counter--;
	}

//	/**
//	 * Decrements internal counter, blocking if the counter is already
//	 * zero.
//	 *
//	 * @exception InterruptedException passed from this.wait().
//	 * @exception TimeOutException if the time specified has passed, but the counter cannot be decreased.
//	 */
//	public synchronized void acquire(long timeout) throws InterruptedException, TimeOutException {
//		if (counter <= 0) {
//			this.wait(timeout);
//		}
//		if (counter==0) {
//			throw new TimeOutException("Timeout of ["+timeout+"] ms expired");
//		}
//		counter--;
//	}

	/**
	 * Increments internal counter, possibly awakening the thread wait()ing in acquire()
	 */
	public synchronized void release() {
		counter++;
		if(counter == 1) {
			this.notify();
		}
	}

	public synchronized boolean isReleased() {
		return counter > 0;
	}
}
