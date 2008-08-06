/*
 * $Log: XmlWellFormedChecker.java,v $
 * Revision 1.2  2008-08-06 16:40:34  europe\L190409
 * added support for flexible monitoring
 *
 * Revision 1.1  2006/02/06 12:42:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */

package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.XmlUtils;

/**
 *<code>Pipe</code> that checks the well-formedness of the input message.
 * If <code>root</code> is given then this is also checked.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setRoot(String) root}</td><td>name of the root element</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * @author  Peter Leeuwenburgh
 * @since	4.4.5
 * @version Id
 */

public class XmlWellFormedChecker extends FixedForwardPipe {
	private String root = null;

	public void configure() throws ConfigurationException {
		super.configure();
		registerEvent(XmlValidator.XML_VALIDATOR_VALID_MONITOR_EVENT);
		registerEvent(XmlValidator.XML_VALIDATOR_PARSER_ERROR_MONITOR_EVENT);
	}


	public PipeRunResult doPipe(Object input, PipeLineSession session) {
		if (XmlUtils.isWellFormed(input.toString(), getRoot())) {
			throwEvent(XmlValidator.XML_VALIDATOR_VALID_MONITOR_EVENT);
			return new PipeRunResult(getForward(), input);
		}
		else {
			throwEvent(XmlValidator.XML_VALIDATOR_PARSER_ERROR_MONITOR_EVENT);
			return new PipeRunResult(findForward("failure"), input);
		}
	}

	public void setRoot(String root) {
		this.root = root;
	}
	public String getRoot() {
		return root;
	}

}
