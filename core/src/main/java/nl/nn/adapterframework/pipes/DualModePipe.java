package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

/**
*<code>Pipe</code> that validates the input message against a XML-Schema.
*
* <p><b>Configuration:</b>
* <table border="1">
* <tr><th>attributes</th><th>description</th><th>default</th></tr>
* <tr><td>className</td><td>nl.nn.adapterframework.pipes.XmlValidator</td><td>&nbsp;</td></tr>
* <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
* <tr><td>{@link #setAutoMode(boolean) autoMode}</td><td>when true, switches automatically from input to output mode</td><td>true</td></tr>
*
* </table>
* @author Peter Leeuwenburgh / Gerrit van Brakel
*/

public class DualModePipe extends FixedForwardPipe {

	public static final String DUAL_MODE_PREFIX = "DualModePipe ";
	public static final String DUAL_MODE_OUTPUT = "OUTPUT";

	private boolean autoMode=true;

	public void enableOutputMode(IPipeLineSession session) {
		session.put(DUAL_MODE_PREFIX+getName(), DUAL_MODE_OUTPUT);
	}
	
	public void disableOutputMode(IPipeLineSession session) {
		session.remove(DUAL_MODE_PREFIX+getName());
	}
	
	public boolean isOutputModeEnabled(IPipeLineSession session) {
		String mode = (String) session.get(DUAL_MODE_PREFIX+getName());
		return DUAL_MODE_OUTPUT.equals(mode);
	}
	
	@Override
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		try {
			return super.doPipe(input, session);
		} finally {
			if (!isOutputModeEnabled(session) && isAutoMode()) {
				enableOutputMode(session);
			}
		}
	}

	public boolean isAutoMode() {
		return autoMode;
	}
	public void setAutoMode(boolean autoMode) {
		this.autoMode = autoMode;
	}


}
