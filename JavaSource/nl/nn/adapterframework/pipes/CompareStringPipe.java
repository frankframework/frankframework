/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
/*
 * $Log: CompareStringPipe.java,v $
 * Revision 1.5  2012-06-01 10:52:49  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.4  2011/11/30 13:51:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2011/03/24 11:07:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added xml attribute, replaced attributes sessionKey1 and sessionKey2 by parameters operand1 and operand2, and added parameter ignorepatterns
 *
 * Revision 1.1  2009/07/13 07:46:39  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * introduction of CompareStringPipe
 *
 */

package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Pipe that compares lexicographically two strings.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.CompareIntegerPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSessionKey1(String) sessionKey1} <i>deprecated</i></td><td>reference to one of the session variables to be compared</td><td></td></tr>
 * <tr><td>{@link #setSessionKey2(String) sessionKey2} <i>deprecated</i></td><td>reference to the other session variables to be compared</td><td></td></tr>
 * <tr><td>{@link #setXml(boolean) xml}</td><td>when set <code>true</code> the string values to compare are considered to be xml strings and before the actual compare both xml strings are transformed to a canonical form</td><td>false</td></tr>
 * </table>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>the parameters <code>operand1</code> and <code>operand2</code> are compared. If one of these parameters doesn't exist the input message is taken.
 * If parameter <code>ignorepatterns</code> exists it contains a xml table with references to substrings which have to be ignored during the comparison. This xml table has the following layout:
 * <br/><code><pre>
 *	&lt;ignores&gt;
 *		&lt;ignore&gt;
 *			&lt;after&gt;...&lt;/after&gt;
 *			&lt;before&gt;...&lt;/before&gt;
 *		&lt;/ignore&gt;
 *		&lt;ignore&gt;
 *			&lt;after&gt;...&lt;/after&gt;
 *			&lt;before&gt;...&lt;/before&gt;
 *		&lt;/ignore&gt;
 *	&lt;/ignores&gt;
 * </pre></code><br/>Substrings between "after" and "before" are ignored</td></tr>
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
 * @version $Id$
 */
public class CompareStringPipe extends AbstractPipe {

	private final static String LESSTHANFORWARD = "lessthan";
	private final static String GREATERTHANFORWARD = "greaterthan";
	private final static String EQUALSFORWARD = "equals";
	private final static String OPERAND1 = "operand1";
	private final static String OPERAND2 = "operand2";
	private final static String IGNOREPATTERNS = "ignorepatterns";

	private String sessionKey1 = null;
	private String sessionKey2 = null;
	private boolean xml = false;

	public void configure() throws ConfigurationException {
		super.configure();

		if (null == findForward(LESSTHANFORWARD))
			throw new ConfigurationException(getLogPrefix(null) + "forward [" + LESSTHANFORWARD + "] is not defined");

		if (null == findForward(GREATERTHANFORWARD))
			throw new ConfigurationException(getLogPrefix(null) + "forward [" + GREATERTHANFORWARD + "] is not defined");

		if (null == findForward(EQUALSFORWARD))
			throw new ConfigurationException(getLogPrefix(null) + "forward [" + EQUALSFORWARD + "] is not defined");

		if (StringUtils.isEmpty(sessionKey1) && StringUtils.isEmpty(sessionKey2)) {
			boolean operand1Exists = false;
			boolean operand2Exists = false;
			ParameterList parameterList = getParameterList();
			for (int i = 0; i < parameterList.size(); i++) {
				Parameter parameter = parameterList.getParameter(i);
				if (parameter.getName().equalsIgnoreCase(OPERAND1)) {
					operand1Exists = true;
				} else {
					if (parameter.getName().equalsIgnoreCase(OPERAND2)) {
						operand2Exists = true;
					}
				}
			}
			if (!operand1Exists && !operand2Exists) {
				throw new ConfigurationException(getLogPrefix(null) + "has neither parameter [" + OPERAND1 + "] nor parameter [" + OPERAND2 + "] specified");
			}
		}
	}

	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		ParameterValueList pvl = null;
		if (getParameterList() != null) {
			ParameterResolutionContext prc = new ParameterResolutionContext((String) input, session);
			try {
				pvl = prc.getValues(getParameterList());
			} catch (ParameterException e) {
				throw new PipeRunException(this, getLogPrefix(session) + "exception extracting parameters", e);
			}
		}

		String operand1 = getParameterValue(pvl, OPERAND1);
		if (operand1 == null) {
			if (StringUtils.isNotEmpty(getSessionKey1())) {
				operand1 = (String) session.get(getSessionKey1());
			}
			if (operand1 == null) {
				operand1 = (String) input;
			}
		}
		String operand2 = getParameterValue(pvl, OPERAND2);
		if (operand2 == null) {
			if (StringUtils.isNotEmpty(getSessionKey2())) {
				operand2 = (String) session.get(getSessionKey2());
			}
			if (operand2 == null) {
				operand2 = (String) input;
			}
		}

		if (isXml()) {
			try {
				operand1 = XmlUtils.canonicalize(operand1);
				operand2 = XmlUtils.canonicalize(operand2);
			} catch (Exception e) {
				throw new PipeRunException(this, getLogPrefix(session) + " Exception on pretty printing input", e);
			}
		}

		String ip = getParameterValue(pvl, IGNOREPATTERNS);
		if (ip != null) {
			try {
				Node n = XmlUtils.buildNode(ip);
				if (n.getNodeName().equals("ignores")) {
					NodeList nList = n.getChildNodes();
					for (int i = 0; i <= nList.getLength() - 1; i++) {
						Node cn = nList.item(i);
						if (cn.getNodeName().equals("ignore")) {
							NodeList cnList = cn.getChildNodes();
							String after = null;
							String before = null;
							for (int j = 0; j <= cnList.getLength() - 1; j++) {
								Node ccn = cnList.item(j);
								if (ccn.getNodeName().equals("after")) {
									after = ccn.getFirstChild().getNodeValue();
								} else {
									if (ccn.getNodeName().equals("before")) {
										before = ccn.getFirstChild().getNodeValue();
									}

								}
							}
							operand1 = ignoreBetween(operand1, after, before);
							operand2 = ignoreBetween(operand2, after, before);
						}
					}
				}
			} catch (Exception e) {
				throw new PipeRunException(this, getLogPrefix(session) + " Exception on ignoring parts of input", e);
			}
		}

		if (log.isDebugEnabled()) {
			log.debug("operand1 [" + operand1 + "]");
			log.debug("operand2 [" + operand2 + "]");
		}

		int comparison = operand1.compareTo(operand2);
		if (comparison == 0)
			return new PipeRunResult(findForward(EQUALSFORWARD), input);
		else if (comparison < 0)
			return new PipeRunResult(findForward(LESSTHANFORWARD), input);
		else
			return new PipeRunResult(findForward(GREATERTHANFORWARD), input);

	}

	private String ignoreBetween(String source, String after, String before) {
		int afterLength = after.length();
		int beforeLength = before.length();

		int start = source.indexOf(after);
		if (start == -1) {
			return source;
		}
		int stop = source.indexOf(before, start + afterLength);
		if (stop == -1) {
			return source;
		}

		char[] sourceArray = source.toCharArray();
		StringBuffer buffer = new StringBuffer();
		int srcPos = 0;

		while (start != -1 && stop != -1) {
			buffer.append(sourceArray, srcPos, start + afterLength);
			if (isXml()) {
				buffer.append("<!-- ignored text -->");
			} else {
				buffer.append("{ignored text}");
			}
			buffer.append(sourceArray, stop, beforeLength);
			srcPos = stop + beforeLength;
			start = source.indexOf(after, srcPos);
			stop = source.indexOf(before, start + afterLength);
		}
		buffer.append(sourceArray, srcPos, sourceArray.length - srcPos);
		return buffer.toString();
	}

	private String getParameterValue(ParameterValueList pvl, String parameterName) {
		ParameterList parameterList = getParameterList();
		if (pvl != null && parameterList != null) {
			for (int i = 0; i < parameterList.size(); i++) {
				Parameter parameter = parameterList.getParameter(i);
				if (parameter.getName().equalsIgnoreCase(parameterName)) {
					return pvl.getParameterValue(i).asStringValue(null);
				}
			}
		}
		return null;
	}

	public void setSessionKey1(String string) {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = getLogPrefix(null) + "The attribute sessionKey1 has been deprecated. Please use the parameter operand1";
		configWarnings.add(log, msg);
		sessionKey1 = string;
	}
	public String getSessionKey1() {
		return sessionKey1;
	}

	public void setSessionKey2(String string) {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = getLogPrefix(null) + "The attribute sessionKey2 has been deprecated. Please use the parameter operand2";
		configWarnings.add(log, msg);
		sessionKey2 = string;
	}
	public String getSessionKey2() {
		return sessionKey2;
	}

	public boolean isXml() {
		return xml;
	}
	public void setXml(boolean b) {
		xml = b;
	}
}
