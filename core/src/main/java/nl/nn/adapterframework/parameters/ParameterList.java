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
}
