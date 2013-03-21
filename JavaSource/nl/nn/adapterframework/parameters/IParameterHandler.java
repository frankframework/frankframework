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

import nl.nn.adapterframework.core.ParameterException;

/**
 * Helper interface to quickly do something for all resolved parameters 
 * 
 * @author John Dekker
 */
public interface IParameterHandler {
	
	/**
	 * Methods is called for each resolved parameter in the order in which they are defined
	 * @param paramName name of the parameter
	 * @param paramValue value of the parameter
	 * @throws ParameterException
	 */
	void handleParam(String paramName, Object paramValue) throws ParameterException;
}
