/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020, 2021, 2023 WeAreFrank!

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
package nl.nn.adapterframework.pipes;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.Category;
import nl.nn.adapterframework.doc.ElementType;
import nl.nn.adapterframework.doc.ElementType.ElementTypes;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Pipe that lexicographically compares two strings, that must NOT be empty.
 *
 * @ff.parameter operand1 The first operand, holds v1. Defaults to input message
 * @ff.parameter operand2 The second operand, holds v2. Defaults to input message
 * @ff.parameter ignorepatterns (optional) contains a xml table with references to substrings which have to be ignored during the comparison. This xml table has the following layout:
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
 * </pre></code><br/>Substrings between "after" and "before" are ignored
 *
 * @ff.forward lessthan operand1 &lt; operand2
 * @ff.forward greaterthan operand1 &gt; operand2
 * @ff.forward equals operand1 = operand2
 *
 * @author  Peter Leeuwenburgh
 */
@Category("Basic")
@ElementType(ElementTypes.ROUTER)
public class CompareStringPipe extends AbstractPipe {

	private static final String LESSTHANFORWARD = "lessthan";
	private static final String GREATERTHANFORWARD = "greaterthan";
	private static final String EQUALSFORWARD = "equals";
	private static final String OPERAND1 = "operand1";
	private static final String OPERAND2 = "operand2";
	private static final String IGNOREPATTERNS = "ignorepatterns";

	private String sessionKey1 = null;
	private String sessionKey2 = null;
	private boolean xml = false;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (null == findForward(LESSTHANFORWARD))
			throw new ConfigurationException("forward [" + LESSTHANFORWARD + "] is not defined");

		if (null == findForward(GREATERTHANFORWARD))
			throw new ConfigurationException("forward [" + GREATERTHANFORWARD + "] is not defined");

		if (null == findForward(EQUALSFORWARD))
			throw new ConfigurationException("forward [" + EQUALSFORWARD + "] is not defined");

		if (StringUtils.isEmpty(sessionKey1) && StringUtils.isEmpty(sessionKey2)) {
			ParameterList parameterList = getParameterList();
			if (parameterList.findParameter(OPERAND1) == null && parameterList.findParameter(OPERAND2) == null) {
				throw new ConfigurationException("has neither parameter [" + OPERAND1 + "] nor parameter [" + OPERAND2 + "] specified");
			}
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		ParameterValueList pvl = null;
		if (getParameterList() != null) {
			try {
				pvl = getParameterList().getValues(message, session);
			} catch (ParameterException e) {
				throw new PipeRunException(this, "exception extracting parameters", e);
			}
		}
		String operand1 = getParameterValue(pvl, OPERAND1);
		try {
			if (operand1 == null) {
				if (StringUtils.isNotEmpty(getSessionKey1())) {
					operand1 = session.getString(getSessionKey1());
				}
				if (operand1 == null) {
					operand1 = message.asString();
				}
			}
		} catch (Exception e) {
			throw new PipeRunException(this, "Exception on getting operand1 from input message", e);
		}
		String operand2 = getParameterValue(pvl, OPERAND2);
		try {
			if (operand2 == null) {
				if (StringUtils.isNotEmpty(getSessionKey2())) {
					operand2 = session.getString(getSessionKey2());
				}
				if (operand2 == null) {
					operand2 = message.asString();
				}
			}
		} catch (Exception e) {
			throw new PipeRunException(this, "Exception on getting operand2 from input message", e);
		}
		if (isXml()) {
			try {
				operand1 = XmlUtils.canonicalize(operand1);
				operand2 = XmlUtils.canonicalize(operand2);
			} catch (Exception e) {
				throw new PipeRunException(this, "Exception on pretty printing input", e);
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
				throw new PipeRunException(this, "Exception on ignoring parts of input", e);
			}
		}

		if (log.isDebugEnabled()) {
			log.debug("operand1 [" + operand1 + "]");
			log.debug("operand2 [" + operand2 + "]");
		}

		int comparison = StringUtils.compare(operand1, operand2);
		if (comparison == 0)
			return new PipeRunResult(findForward(EQUALSFORWARD), message);
		else if (comparison < 0)
			return new PipeRunResult(findForward(LESSTHANFORWARD), message);
		else
			return new PipeRunResult(findForward(GREATERTHANFORWARD), message);

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
		StringBuilder buffer = new StringBuilder();
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
			ParameterValue pv = pvl.findParameterValue(parameterName);
			if(pv != null) {
				return pv.asStringValue(null);
			}
		}
		return null;
	}

	@Override
	public boolean consumesSessionVariable(String sessionKey) {
		return super.consumesSessionVariable(sessionKey) || sessionKey.equals(getSessionKey1()) || sessionKey.equals(getSessionKey2());
	}


	/** reference to one of the session variables to be compared. Do not use, but use Parameter operand1 instead */
	@Deprecated
	@ConfigurationWarning("Please use the parameter operand1")
	public void setSessionKey1(String string) {
		sessionKey1 = string;
	}
	public String getSessionKey1() {
		return sessionKey1;
	}

	/** reference to the other session variables to be compared. Do not use, but use Parameter operand2 instead */
	@Deprecated
	@ConfigurationWarning("Please use the parameter operand2")
	public void setSessionKey2(String string) {
		sessionKey2 = string;
	}
	public String getSessionKey2() {
		return sessionKey2;
	}

	public boolean isXml() {
		return xml;
	}

	/**
	 * when set <code>true</code> the string values to compare are considered to be xml strings and before the actual compare both xml strings are transformed to a canonical form
	 * @ff.default false
	 */
	public void setXml(boolean b) {
		xml = b;
	}
}
