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
package nl.nn.adapterframework.parameters;

import java.util.ArrayList;
import java.util.Iterator;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;


/**
 * List of parameters.
 * 
 * @author Gerrit van Brakel
 */
public class ParameterList extends ArrayList<Parameter> {
	
	public ParameterList() {
		super();
	}

	public ParameterList(int i) {
		super(i);
	}
	
	public void configure() throws ConfigurationException {
		for (int i=0; i<size(); i++) {
			getParameter(i).configure();
		}
	}
	
	public Parameter getParameter(int i) {
		return get(i);
	}
	
	public Parameter findParameter(String name) {
		for (Iterator<Parameter> it=iterator();it.hasNext();) {
			Parameter p = it.next();
			if (p!=null && p.getName().equals(name)) {
				return p;
			}
		}
		return null;
	}
	
	public boolean parameterEvaluationRequiresInputMessage() {
		for (Parameter p:this) {
			if (p.requiresInputValueForResolution()) {
				return true;
			}
		}
		return false;
	}
	

	public ParameterValueList getValues(Message message, PipeLineSession session) throws ParameterException {
		return getValues(message, session, true);
	}
	/**
	 * Returns an array list of <link>ParameterValue<link> objects
	 */
	public ParameterValueList getValues(Message message, PipeLineSession session, boolean namespaceAware) throws ParameterException {
		ParameterValueList result = new ParameterValueList();
		for (Parameter parm:this) {
			String parmSessionKey = parm.getSessionKey();
			// if a parameter has sessionKey="*", then a list is generated with a synthetic parameter referring to 
			// each session variable whose name starts with the name of the original parameter
			if ("*".equals(parmSessionKey)) {
				String parmName = parm.getName();
				for (String sessionKey: session.keySet()) {
					if (!PipeLineSession.tsReceivedKey.equals(sessionKey) && !PipeLineSession.tsSentKey.equals(sessionKey)) {
						if ((sessionKey.startsWith(parmName) || "*".equals(parmName))) {
							Parameter newParm = new Parameter();
							newParm.setName(sessionKey);
							newParm.setSessionKey(sessionKey); // TODO: Should also set the parameter.type, based on the type of the session key.
							try {
								newParm.configure();
							} catch (ConfigurationException e) {
								throw new ParameterException(e);
							}
							result.add(getValue(result, newParm, message, session, namespaceAware));
						}
					}
				}
			} else {
				result.add(getValue(result, parm, message, session, namespaceAware));
			}
		}
		return result;
	}

	private ParameterValue getValue(ParameterValueList alreadyResolvedParameters, Parameter p, Message message, PipeLineSession session, boolean namespaceAware) throws ParameterException {
		return new ParameterValue(p, p.getValue(alreadyResolvedParameters, message, session, namespaceAware));
	}

	
}
