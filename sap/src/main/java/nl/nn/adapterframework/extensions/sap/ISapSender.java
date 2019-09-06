/*
   Copyright 2013,2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.sap;

import nl.nn.adapterframework.core.ISenderWithParameters;

/**
 * Common interface to be implemented by SapSender implementations.
 * 
 * @author Gerrit van Brakel
 * @since  7.3
 */
public interface ISapSender extends ISenderWithParameters,ISapFunctionFacade {

	public void setSynchronous(boolean b);

	public void setFunctionName(String string);
	public void setFunctionNameParam(String string);
	public void setLuwHandleSessionKey(String string);

	public void setSapSystemNameParam(String string);
	public String getRequestFieldName();

}
