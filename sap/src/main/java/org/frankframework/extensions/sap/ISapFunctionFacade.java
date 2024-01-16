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

	public void setCorrelationIdFieldIndex(int i);
	public void setCorrelationIdFieldName(String string);
	public void setRequestFieldIndex(int i);
	public void setRequestFieldName(String string);
	public void setReplyFieldIndex(int i);
	public void setReplyFieldName(String string);
	public void setSapSystemName(String string);
}
