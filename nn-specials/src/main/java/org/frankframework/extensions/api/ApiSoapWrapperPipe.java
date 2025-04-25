/*
   Copyright 2016 Nationale-Nederlanden

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
package org.frankframework.extensions.api;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.soap.SoapWrapperPipe;
import org.frankframework.util.AppConstants;
import org.frankframework.util.SpringUtils;

/**
 * Extension to SoapWrapperPipe for API Management.
 * <p>
 * <b>Configuration </b><i>(where deviating from SoapWrapperPipe)</i><b>:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setSoapHeaderSessionKey(String) soapHeaderSessionKey}</td><td>if direction=<code>wrap</code>: </td><td>soapHeader</td></tr>
 * <tr><td>{@link #setSoapHeaderStyleSheet(String) soapHeaderStyleSheet}</td><td>if direction=<code>wrap</code>: </td><td>/xml/xsl/api/soapHeader.xsl</td></tr>
 * </table>
 * </p><p>
 * <b>/xml/xsl/api/soapHeader.xsl:</b>
 * <table border="1">
 * <tr><th>element</th><th>level</th><th>value</th></tr>
 * <tr><td>MessageHeader</td><td>0</td><td><code>MessageHeader</code> is only created when $conversationId is filled (otherwise skipped)</td></tr>
 * <tr><td>&nbsp;</td><td>&nbsp;</td><td>xmlns=$namespace</td></tr>
 * <tr><td>From</td><td>1</td><td><code>From</code> is only created when $from_in is filled (otherwise skipped) and it's created with the value of $from_out</td></tr>
 * <tr><td>HeaderFields</td><td>1</td><td>&nbsp;</td></tr>
 * <tr><td>ConversationId</td><td>2</td><td>$conversationId</td></tr>
 * </table>
 * <b>Parameters:</b>
 * <table border="1">
 * <tr><th>name</th><th>default</th></tr>
 * <tr><td>namespace</td><td>"http://api.nn.nl/MessageHeader"</td></tr>
 * <tr><td>from_in</td><td>if applicable, copied from the original (received) SOAP Header</td></tr>
 * <tr><td>from_out</td><td>property 'instance.name'</td></tr>
 * <tr><td>conversationId</td><td>if applicable, copied from the original (received) SOAP Header</td></tr>
 * </table>
 * </p>
 * @author Peter Leeuwenburgh
 */

public class ApiSoapWrapperPipe extends SoapWrapperPipe {
	protected static final String CONVERSATIONID = "conversationId";
	protected static final String FROM_IN = "from_in";
	protected static final String FROM_OUT = "from_out";

	@Override
	public void configure() throws ConfigurationException {
		if (getDirection()==Direction.WRAP) {
			if (StringUtils.isEmpty(getSoapHeaderSessionKey())) {
				setSoapHeaderSessionKey(DEFAULT_SOAP_HEADER_SESSION_KEY);
			}
			if (StringUtils.isEmpty(getSoapHeaderStyleSheet())) {
				setSoapHeaderStyleSheet("/xml/xsl/api/soapHeader.xsl");
			}
			addParameters();
		}
		super.configure();
	}

	private void addParameters() {
		ParameterList parameterList = getParameterList();
		Parameter p;
		if (!parameterList.hasParameter(CONVERSATIONID)) {
			p = SpringUtils.createBean(getApplicationContext());
			p.setName(CONVERSATIONID);
			p.setSessionKey(getSoapHeaderSessionKey());
			p.setXpathExpression("MessageHeader/HeaderFields/ConversationId");
			p.setRemoveNamespaces(true);
			// only forward the existing ConversationId, don't create a new one
			// p.setPattern("{hostname}_{uid}");
			// p.setDefaultValueMethods("pattern");
			addParameter(p);
		}
		if (!parameterList.hasParameter(FROM_IN)) {
			p = SpringUtils.createBean(getApplicationContext());
			p.setName(FROM_IN);
			p.setSessionKey(getSoapHeaderSessionKey());
			p.setXpathExpression("MessageHeader/From");
			addParameter(p);
		}
		p = SpringUtils.createBean(getApplicationContext());
		p.setName(FROM_OUT);
		p.setValue(AppConstants.getInstance().getProperty("instance.name", ""));
		addParameter(p);

	}
}
