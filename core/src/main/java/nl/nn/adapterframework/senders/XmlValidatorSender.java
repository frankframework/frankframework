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
package nl.nn.adapterframework.senders;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.validation.AbstractXmlValidator;
import nl.nn.adapterframework.validation.XercesXmlValidator;


/**
 *<code>Sender</code> that validates the input message against a XML-Schema.
 *
 * N.B. noNamespaceSchemaLocation may contain spaces, but not if the schema is stored in a .jar or .zip file on the class path.
 * 
 * @author  Gerrit van Brakel
 * @since  
 */
public class XmlValidatorSender extends XercesXmlValidator implements ISenderWithParameters {

	private String name;
	
	@Override
	public void configure() throws ConfigurationException {
		configure(getLogPrefix());
	}
	
	@Override
	public void close() throws SenderException {
	}
	@Override
	public void open() throws SenderException {
	}

	@Override
	public String sendMessage(String correlationID, String message) throws SenderException, TimeOutException {
		return sendMessage(correlationID,message,null);
	}
	@Override
	public void addParameter(Parameter p) { 
		// class doesn't really have parameters, but implements ISenderWithParameters to get ParameterResolutionContext in sendMessage(), to obtain session
	}

	@Override
	public ParameterList getParameterList() {
		// class doesn't really have parameters, but implements ISenderWithParameters to get ParameterResolutionContext in sendMessage(), to obtain session
		return null;
	}

	@Override
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException {
		IPipeLineSession session=prc.getSession();
		String fullReasons="tja";
		try {
			String resultEvent = validate(message, session, getLogPrefix(),null,null,false);
			
			if (AbstractXmlValidator.XML_VALIDATOR_VALID_MONITOR_EVENT.equals(resultEvent)) {
				return message;
			}
			fullReasons = resultEvent; // TODO: find real fullReasons
			if (isThrowException()) {
				throw new SenderException(fullReasons);
			}
			log.warn(fullReasons);
			return message;
		} catch (Exception e) {
			if (isThrowException()) {
				throw new SenderException(e);
			}
			log.warn(fullReasons, e);
			return message;
		}
	}

	@Override
	public boolean isSynchronous() {
		return true;
	}

	protected String getLogPrefix() {
		return "["+this.getClass().getName()+"] ["+getName()+"] ";
	}
	
	@Override
	public String getName() {
		return name;
	}
	@Override
	public void setName(String name) {
		this.name=name;
	}
}
