/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.management.gateway;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.Lifecycle;
import org.springframework.context.Phased;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.LifecycleService;

/**
 * A 'bridge' between Spring Lifecycles and Hazelcast.
 * Does not need to start Hazelcast as that's done when the object has been made.
 * But it does provide a safe (with the {@link #isRunning()} method) shutdown procedure.
 * {@link Phased} because it needs to shutdown before the {@link HazelcastInboundGateway} shuts down.
 * {@link DisposableBean Disposable} which forces immediate shutdown.
 */
public class SpringHazelcastLifecycle implements Lifecycle, Phased, DisposableBean {
	private LifecycleService lifecycle;

	public SpringHazelcastLifecycle(HazelcastInstance hzInstance) {
		lifecycle = hzInstance.getLifecycleService();
	}

	@Override
	public void start() {
		// do nothing, it starts automatically
	}

	@Override
	public void stop() {
		lifecycle.shutdown();
	}

	@Override
	public boolean isRunning() {
		return lifecycle.isRunning();
	}

	// Force earlier shutdown phase.
	@Override
	public int getPhase() {
		return Integer.MAX_VALUE;
	}

	@Override
	public void destroy() throws Exception {
		lifecycle.terminate();
	}
}
