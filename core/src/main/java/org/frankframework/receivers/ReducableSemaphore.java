/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.receivers;

/**
 * See {@link java.util.concurrent.Semaphore} for generic details.
 * Only difference is that the method reducePermits is made public. See {@link #reducePermits(int)}.
 */
public class ReducableSemaphore extends java.util.concurrent.Semaphore {
	public ReducableSemaphore(int permits) {
		super(permits);
	}

	/**
	 * Non-blocking decrement internal counter.
	 * Purpose: enable the UI to decrease the max amount of threads, without stopping a thread.
	 * Override to make it public
	 */
	@Override
	public void reducePermits(int reduction) {
		super.reducePermits(reduction);
	}

}
