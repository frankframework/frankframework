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
package org.frankframework.pipes;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.Category;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.Forward;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.util.XmlUtils;

/**
 * Pipe that lexicographically compares two strings, that must NOT be empty.
 *
 * @ff.parameter operand1 The first operand, holds v1. Defaults to input message
 * @ff.parameter operand2 The second operand, holds v2. Defaults to input message
 * @ff.parameter ignorepatterns (optional) contains a xml table with references to substrings which have to be ignored during the comparison. This xml table has the following layout:
 * <br/>
 * <pre>{@code
 * <ignores>
 * 	   <ignore>
 * 	       <after>...</after>
 * 	       <before>...</before>
 * 	   </ignore>
 * 	   <ignore>
 * 	       <after>...</after>
 * 	       <before>...</before>
 * 	   </ignore>
 * </ignores>
 * }</pre>
 * <br/>
 * Substrings between "after" and "before" are ignored
 *
 * @author  Peter Leeuwenburgh
 */
@Forward(name = "lessthan", description = "operand1 &lt; operand2")
@Forward(name = "greaterthan", description = "operand1 &gt; operand2")
@Forward(name = "equals", description = "operand1 = operand2")
@Category(Category.Type.BASIC)
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.ROUTER)
public class CompareStringPipe extends AbstractPipe {

	private static final String LESSTHANFORWARD = "lessthan";
	private static final String GREATERTHANFORWARD = "greaterthan";
	private static final String EQUALSFORWARD = "equals";
	protected static final String OPERAND1 = "operand1";
	protected static final String OPERAND2 = "operand2";
	private static final String IGNOREPATTERNS = "ignorepatterns";
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

		ParameterList parameterList = getParameterList();
		if (!parameterList.hasParameter(OPERAND1) && !parameterList.hasParameter(OPERAND2)) {
			throw new ConfigurationException("has neither parameter [" + OPERAND1 + "] nor parameter [" + OPERAND2 + "] specified");
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
				operand1 = message.asString();
			}
		} catch (Exception e) {
			throw new PipeRunException(this, "Exception on getting operand1 from input message", e);
		}
		String operand2 = getParameterValue(pvl, OPERAND2);
		try {
			if (operand2 == null) {
				operand2 = message.asString();
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
				if ("ignores".equals(n.getNodeName())) {
					NodeList nList = n.getChildNodes();
					for (int i = 0; i <= nList.getLength() - 1; i++) {
						Node cn = nList.item(i);
						if ("ignore".equals(cn.getNodeName())) {
							NodeList cnList = cn.getChildNodes();
							String after = null;
							String before = null;
							for (int j = 0; j <= cnList.getLength() - 1; j++) {
								Node ccn = cnList.item(j);
								if ("after".equals(ccn.getNodeName())) {
									after = ccn.getFirstChild().getNodeValue();
								} else {
									if ("before".equals(ccn.getNodeName())) {
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
			log.debug("operand1 [{}]", operand1);
			log.debug("operand2 [{}]", operand2);
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
		return super.consumesSessionVariable(sessionKey);
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
