package org.frankframework.testdummies;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.frankframework.core.HasName;
import org.frankframework.core.IErrorMessageFormatter;
import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;

public class TestDummyErrorMessageFormatter implements IErrorMessageFormatter {
	@Override
	public @NonNull Message format(@Nullable String errorMessage, @Nullable Throwable t, @Nullable HasName location, @Nullable Message originalMessage, @NonNull PipeLineSession session) {
		StringBuilder messageBuilder = new StringBuilder("Error");
		if (location != null) {
			messageBuilder
					.append(" in [")
					.append(location.getName())
					.append("]");
		}
		if (errorMessage != null) {
			messageBuilder
					.append(": ")
					.append(errorMessage);
		}
		if (t != null) {
			messageBuilder
					.append(" ")
					.append(t.getMessage());
		}
		if (originalMessage != null) {
			try {
				messageBuilder
						.append(" [")
						.append(originalMessage.asString())
						.append("]");
			} catch (IOException e) {
				messageBuilder
						.append(" cannot get original message data as string: ")
						.append(e.getMessage());
			}
		}
		return new Message(messageBuilder.toString());
	}
}
