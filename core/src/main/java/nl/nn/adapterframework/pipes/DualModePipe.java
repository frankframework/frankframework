package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

/**
*<code>Pipe</code> that validates the input message against a XML-Schema.
*
* @author Peter Leeuwenburgh / Gerrit van Brakel
*/

public class DualModePipe extends FixedForwardPipe {

	public static final String DUAL_MODE_PREFIX = "DualModePipe ";
	public static final String DUAL_MODE_RESPONSE = "RESPONSE";

	public void enableResponseMode(IPipeLineSession session) {
		session.put(DUAL_MODE_PREFIX+getName(), DUAL_MODE_RESPONSE);
	}
	
	public void disableResponseMode(IPipeLineSession session) {
		session.remove(DUAL_MODE_PREFIX+getName());
	}
	
	public boolean isOutputModeEnabled(IPipeLineSession session) {
		String mode = (String) session.get(DUAL_MODE_PREFIX+getName());
		return DUAL_MODE_RESPONSE.equals(mode);
	}
	
	public PipeRunResult doPipeForResponse(Object input, IPipeLineSession session) throws PipeRunException {
		try {
			enableResponseMode(session);
			return super.doPipe(input, session);
		} finally {
			disableResponseMode(session);
		}
	}

}
