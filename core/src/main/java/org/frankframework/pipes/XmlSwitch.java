/*
   Copyright 2013, 2016, 2019, 2020 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.pipes;


import org.frankframework.doc.Protected;

/**
 * Selects an exitState, based on either the content of the input message, by means
 * of an XSLT-stylesheet, the content of a session variable or, by default, by returning the name of the root-element.
 * <br/>
 * @ff.note This pipe has been renamed to {@link SwitchPipe} which has also gained {@link SwitchPipe#setJsonPathExpression(String)} functionality.
 */
public class XmlSwitch extends SwitchPipe {
		// Empty class, exists as alias for the SwitchPipe to ease gradual migration to the new name.


	@Override
	@Protected
	public void setJsonPathExpression(String jsonPathExpression) {
		// Setter exists to add @Protected annotation to forbid this method on the XmlSwitch and push people to the SwitchPipe.
		super.setJsonPathExpression(jsonPathExpression);
	}
}
