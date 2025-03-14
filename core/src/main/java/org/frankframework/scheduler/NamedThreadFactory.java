/*
   Copyright 2021-2025 WeAreFrank!

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
package org.frankframework.scheduler;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;

public class NamedThreadFactory implements ThreadFactory {
	private @Setter int threadPriority = Thread.NORM_PRIORITY;
	private @Getter @Setter ThreadGroup threadGroup;

	private final AtomicInteger threadCount = new AtomicInteger();

	@Override
	public Thread newThread(Runnable runnable) {
		String threadName = getThreadName(runnable);
		Thread thread = new Thread(threadGroup, runnable, threadName);
		thread.setPriority(threadPriority);
		thread.setDaemon(false);
		return thread;
	}

	private String getThreadName(Runnable runnable) {
		return "FF-Worker-"+this.threadCount.incrementAndGet();
	}

	public void setThreadGroupName(String name) {
		threadGroup = new ThreadGroup(name);
	}
}
