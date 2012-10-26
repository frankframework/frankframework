/*
 * $Log: XmlWellFormedChecker.java,v $
 * Revision 1.9  2012-10-26 16:13:38  m00f069
 * Moved *Xmlvalidator*, Schema and SchemasProvider to new validation package
 *
 * Revision 1.8  2012/10/01 07:59:29  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Improved messages stored in reasonSessionKey and xmlReasonSessionKey
 * Cleaned XML validation code and documentation a bit.
 *
 * Revision 1.7  2012/06/01 10:52:49  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.6  2011/11/30 13:51:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2011/08/22 14:28:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * constants moved to XmlValidatorBase
 *
 * Revision 1.3  2008/12/09 12:47:00  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added forward parserError
 *
 * Revision 1.2  2008/08/06 16:40:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for flexible monitoring
 *
 * Revision 1.1  2006/02/06 12:42:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */

package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.validation.AbstractXmlValidator;

/**
 *<code>Pipe</code> that checks the well-formedness of the input message.
 * If <code>root</code> is given then this is also checked.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setRoot(String) root}</td><td>name of the root element</td><td>&nbsp;</td></tr>
 * </table>
 * </p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified, the value for "success"</td></tr>
 * <tr><td>"parserError"</td><td>a parser exception occurred, probably caused by non-well-formed XML. If not specified, "failure" is used in such a case</td></tr>
 * <tr><td>"failure"</td><td>if a validation error occurred</td></tr>
 * </table>
 * <br>
 * @author  Peter Leeuwenburgh
 * @since	4.4.5
 * @version Id
 */

public class XmlWellFormedChecker extends FixedForwardPipe {
	private String root = null;

	public void configure() throws ConfigurationException {
		super.configure();
		registerEvent(AbstractXmlValidator.XML_VALIDATOR_VALID_MONITOR_EVENT);
		registerEvent(AbstractXmlValidator.XML_VALIDATOR_PARSER_ERROR_MONITOR_EVENT);
	}


	public PipeRunResult doPipe(Object input, IPipeLineSession session) {
		if (XmlUtils.isWellFormed(input.toString(), getRoot())) {
			throwEvent(AbstractXmlValidator.XML_VALIDATOR_VALID_MONITOR_EVENT);
			return new PipeRunResult(getForward(), input);
		}
		throwEvent(AbstractXmlValidator.XML_VALIDATOR_PARSER_ERROR_MONITOR_EVENT);
		PipeForward forward = findForward("parserError");
		if (forward==null) {
			forward = findForward("failure");
		}
		return new PipeRunResult(forward, input);
	}

	public void setRoot(String root) {
		this.root = root;
	}
	public String getRoot() {
		return root;
	}

}
