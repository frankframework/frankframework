package org.frankframework.testdummies;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.core.IDualModeValidator;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.pipes.AbstractValidator;
import org.frankframework.stream.Message;

public class TestDummyValidator extends AbstractValidator implements IDualModeValidator {

	private final String[] failOnValue;
	private final boolean dualModeValidator;

	public TestDummyValidator(boolean dualModeValidator, String... failOnValue) {
		this.dualModeValidator = dualModeValidator;
		this.failOnValue = failOnValue;
	}

	public TestDummyValidator(String... failOnValue) {
		this(false, failOnValue);
	}

	@Override
	protected PipeForward validate(Message messageToValidate, PipeLineSession session, boolean responseMode, String messageRoot) throws PipeRunException {
		try {
			String data = messageToValidate.asString();
			for (String value : failOnValue) {
				if (StringUtils.isNotEmpty(data) && StringUtils.isNotEmpty(value) && data.contains(value)) {
					return findForward("failure");
				}
			}
			return findForward("success");
		} catch (IOException e) {
			throw new PipeRunException(this, "Failure to get data from message", e);
		}
	}

	@Override
	public boolean isConfiguredForMixedValidation() {
		return dualModeValidator;
	}
}
