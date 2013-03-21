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
 * $Log: IParameterHandler.java,v $
 * Revision 1.3  2011-11-30 13:52:03  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2005/10/24 09:59:23  John Dekker <john.dekker@ibissource.org>
 * Add support for pattern parameters, and include them into several listeners,
 * senders and pipes that are file related
 *
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
