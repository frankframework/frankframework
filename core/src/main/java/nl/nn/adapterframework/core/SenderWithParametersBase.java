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
package nl.nn.adapterframework.core;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.senders.SenderBase;

/**
 * Provides a base class for senders with parameters.
 * 
 * @author Gerrit van Brakel
 * @since  4.3
 */
public abstract class SenderWithParametersBase extends SenderBase implements ISenderWithParameters {
	
	protected ParameterList paramList = null;

	@Override
	public void configure() throws ConfigurationException {
		if (paramList!=null) {
			paramList.configure();
		}
	}

	@Override
	// can make this sendMessage() 'final', debugging handled by the abstract sendMessage() defined by ISenderWithParameters, that includes the parameterResolutionContext
	public final String sendMessage(String correlationID, String message) throws SenderException, TimeOutException  {
		return sendMessage(correlationID,message,null);
	}

	@Override
	public void addParameter(Parameter p) {
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}

	/**
	 * return the Parameters
	 */
	public ParameterList getParameterList() {
		return paramList;
	}

}
