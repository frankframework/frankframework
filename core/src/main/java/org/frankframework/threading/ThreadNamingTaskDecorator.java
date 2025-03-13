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

import jakarta.annotation.Nonnull;

import org.springframework.core.task.TaskDecorator;

public class ThreadNamingTaskDecorator implements TaskDecorator {

	/**
	 * Reverts the thread-name back to the original.
	 */
	@Override
	public @Nonnull Runnable decorate(@Nonnull Runnable runnable) {
		return () -> {
			final String threadName = Thread.currentThread().getName();
			try {
				// Give thread some name when we start a task on it
				runnable.run();
			} finally {
				// Restore the original name
				Thread.currentThread().setName(threadName);
			}
		};
	}
}
