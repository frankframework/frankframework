package org.frankframework.testdummies;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;

import org.frankframework.core.IWrapperPipe;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.AbstractPipe;
import org.frankframework.stream.Message;

public class TestDummyWrapper extends AbstractPipe implements IWrapperPipe {

	private final String[] failOnValues;

	public TestDummyWrapper(@NonNull String... failOnValues) {
		this.failOnValues = failOnValues;
	}

	@Override
	public @NonNull PipeRunResult doPipe(@NonNull Message message, @NonNull PipeLineSession session) throws PipeRunException {
		try {
			String data = message.asString();
			for (String value : failOnValues) {
				if (StringUtils.isNoneEmpty(data, value) && data.contains(value)) {
					Message result = new Message("wrapping-failed" + getName() + "[" + data + "]");
					return new PipeRunResult(findForward("failure"), result);
				}
			}
			Message result = new Message("wrapping-success" + getName() + "[" + data + "]");
			return new PipeRunResult(findForward("success"), result);
		} catch (IOException e) {
			throw new PipeRunException(this, "Failure to get data from message", e);
		}
	}
}
