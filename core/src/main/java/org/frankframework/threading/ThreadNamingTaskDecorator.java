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
