package nl.nn.adapterframework.pipes;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;

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
		String param2 = (pvl.contains("param2")) ? getParameterValue(pvl, "param2") : getParameterValue(pvl, "param1");

		if(StringUtils.isNotEmpty(param1)) {
			return new PipeRunResult(getSuccessForward(), param1);
		}
		return new PipeRunResult(getSuccessForward(), param2);
	}

}
