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
package org.frankframework.extensions.sap;

import org.frankframework.core.ISenderWithParameters;

/**
 * Common interface to be implemented by SapSender implementations.
 *
 * @author Gerrit van Brakel
 * @since  7.3
 */
public interface ISapSender extends ISenderWithParameters,ISapFunctionFacade {

	void setSynchronous(boolean b);

	void setFunctionName(String string);
	void setFunctionNameParam(String string);
	void setLuwHandleSessionKey(String string);

	void setSapSystemNameParam(String string);
	String getRequestFieldName();

}
