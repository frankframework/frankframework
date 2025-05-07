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
 * ResourceLimiter for limiting the number of resources that can be used in parallel.
 * Contains a maxResourceLimit that can be increased or decreased, separate from the actual permits.
 * See {@link java.util.concurrent.Semaphore} for generic details.
 */
public class ResourceLimiter extends java.util.concurrent.Semaphore {
	private int maxResourceLimit;

	public ResourceLimiter(int permits) {
		super(permits);
		maxResourceLimit = permits;
	}

	public ResourceLimiter(int permits, boolean fair) {
		super(permits, fair);
		maxResourceLimit = permits;
	}

	public void increaseMaxResourceCount(int addition) {
		if (addition < 0) throw new IllegalArgumentException("Only positive values are allowed.");
		release(addition);
		maxResourceLimit += addition;
	}

	public void reduceMaxResourceCount(int reduction) {
		if (reduction < 0) throw new IllegalArgumentException("Only positive values are allowed.");
		reducePermits(reduction);
		maxResourceLimit -= reduction;
	}

	public int getMaxResourceLimit() {
		return maxResourceLimit;
	}

	public void waitUntilAllResourcesAvailable() throws InterruptedException {
		// To wait until all resources are available again, acquire up to the resource limit
		acquire(maxResourceLimit);

		// However since we don't need to keep them unavailable, release them again after acquiring.
		release(maxResourceLimit);
	}
}
