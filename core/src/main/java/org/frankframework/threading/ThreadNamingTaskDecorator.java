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
package org.frankframework.threading;

import java.util.concurrent.atomic.AtomicLong;

import jakarta.annotation.Nonnull;

import org.springframework.core.task.TaskDecorator;

import org.frankframework.util.ClassUtils;

public class ThreadNamingTaskDecorator implements TaskDecorator {
	private final AtomicLong counter = new AtomicLong();

	@Override
	public @Nonnull Runnable decorate(@Nonnull Runnable runnable) {
		return () -> {
			final String threadName = Thread.currentThread().getName();
			try {
				// Give thread some name when we start a task on it
				Thread.currentThread().setName(ClassUtils.nameOf(runnable) + "-FFWorker-" + counter.incrementAndGet());
				runnable.run();
			} finally {
				// Restore the original name
				Thread.currentThread().setName(threadName);
			}
		};
	}
}
