package org.frankframework.pipes;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;

public class ParameterValueTestPipe extends FixedForwardPipe {

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		ParameterValueList pvl = null;
		if (getParameterList() != null) {
			try {
				pvl = getParameterList().getValues(message, session);
			} catch (ParameterException e) {
				throw new PipeRunException(this, "exception on extracting parameters", e);
			}
		}

		String param1 = getParameterValue(pvl, "param1");
		String param2 = pvl.contains("param2") ? getParameterValue(pvl, "param2") : getParameterValue(pvl, "param1");

		if(StringUtils.isNotEmpty(param1)) {
			return new PipeRunResult(getSuccessForward(), param1);
		}
		return new PipeRunResult(getSuccessForward(), param2);
	}

}
