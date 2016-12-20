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
package nl.nn.adapterframework.extensions.api;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.soap.SoapWrapperPipe;

import org.apache.commons.lang.StringUtils;

/**
 * Extension to SoapWrapperPipe for API Management.
 *
 * @author Peter Leeuwenburgh
 */

public class ApiSoapWrapperPipe extends SoapWrapperPipe {
	protected final static String CONVERSATIONID = "conversationId";

	@Override
	public void configure() throws ConfigurationException {
		if ("wrap".equalsIgnoreCase(getDirection())) {
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
		if (parameterList.findParameter(CONVERSATIONID) == null) {
			p = new Parameter();
			p.setName(CONVERSATIONID);
			p.setSessionKey(getSoapHeaderSessionKey());
			p.setXpathExpression("MessageHeader/HeaderFields/ConversationId");
			p.setRemoveNamespaces(true);
			// only forward the existing ConversationId, don't create a new one
			// p.setPattern("{hostname}_{uid}");
			// p.setDefaultValueMethods("pattern");
			addParameter(p);
		}
	}
}
