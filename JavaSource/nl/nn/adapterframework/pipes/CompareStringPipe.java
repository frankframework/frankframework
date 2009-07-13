/*
 * $Log: CompareStringPipe.java,v $
 * Revision 1.1  2009-07-13 07:46:39  m168309
 * introduction of CompareStringPipe
 *
 */

package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe that compares lexicographically the string values of two session variables.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.CompareIntegerPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSessionKey1(String) sessionKey1}</td><td>reference to one of the session variables to be compared</td><td></td></tr>
 * <tr><td>{@link #setSessionKey2(String) sessionKey2}</td><td>reference to the other session variables to be compared</td><td></td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>lessthan</td><td>when v1 &lt; v2</td></tr>
 * <tr><td>greaterthan</td><td>when v1 &gt; v2</td></tr>
 * <tr><td>equals</td><td>when v1 = v1</td></tr>
 * </table>
 * </p>
 * @author  Peter Leeuwenburgh
 * @version Id
 */
public class CompareStringPipe extends AbstractPipe {

	private final static String LESSTHANFORWARD = "lessthan";
	private final static String GREATERTHANFORWARD = "greaterthan";
	private final static String EQUALSFORWARD = "equals";

	private String sessionKey1 = null;
	private String sessionKey2 = null;

	public void configure() throws ConfigurationException {
		super.configure();

		if (StringUtils.isEmpty(sessionKey1))
			throw new ConfigurationException(getLogPrefix(null) + "sessionKey1 must be filled");

		if (StringUtils.isEmpty(sessionKey2))
			throw new ConfigurationException(getLogPrefix(null) + "sessionKey2 must be filled");

		if (null == findForward(LESSTHANFORWARD))
			throw new ConfigurationException(getLogPrefix(null)	+ "forward ["+ LESSTHANFORWARD+ "] is not defined");

		if (null == findForward(GREATERTHANFORWARD))
			throw new ConfigurationException(getLogPrefix(null)	+ "forward ["+ GREATERTHANFORWARD+ "] is not defined");

		if (null == findForward(EQUALSFORWARD))
			throw new ConfigurationException(getLogPrefix(null)	+ "forward ["+ EQUALSFORWARD+ "] is not defined");
	}

	public PipeRunResult doPipe(Object input, PipeLineSession session)
		throws PipeRunException {

		String sessionKey1StringValue = (String) session.get(sessionKey1);
		String sessionKey2StringValue = (String) session.get(sessionKey2);

		if (log.isDebugEnabled()) {
			log.debug("sessionKey1StringValue [" + sessionKey1StringValue + "]");
			log.debug("sessionKey2StringValue [" + sessionKey2StringValue + "]");
		}

		int comparison=sessionKey1StringValue.compareTo(sessionKey2StringValue);
		if (comparison == 0)
			return new PipeRunResult(findForward(EQUALSFORWARD), input);
		else if (comparison < 0)
			return new PipeRunResult(findForward(LESSTHANFORWARD), input);
		else
			return new PipeRunResult(findForward(GREATERTHANFORWARD), input);

	}

	public void setSessionKey1(String string) {
		sessionKey1 = string;
	}
	public String getSessionKey1() {
		return sessionKey1;
	}

	public void setSessionKey2(String string) {
		sessionKey2 = string;
	}
	public String getSessionKey2() {
		return sessionKey2;
	}

}
