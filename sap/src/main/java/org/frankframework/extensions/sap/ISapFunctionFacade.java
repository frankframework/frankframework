/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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

import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.IConfigurable;

/**
 * Common interface to be implemented by SapListeners
 *
 * @author  Gerrit van Brakel
 * @since   7.3
 */
public interface ISapFunctionFacade extends IConfigurable, HasPhysicalDestination {

	void setCorrelationIdFieldIndex(int i);
	void setCorrelationIdFieldName(String string);
	void setRequestFieldIndex(int i);
	void setRequestFieldName(String string);
	void setReplyFieldIndex(int i);
	void setReplyFieldName(String string);
	void setSapSystemName(String string);
}
