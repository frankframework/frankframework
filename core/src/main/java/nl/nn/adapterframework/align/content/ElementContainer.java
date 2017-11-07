/*
   Copyright 2017 Nationale-Nederlanden

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
package nl.nn.adapterframework.align.content;

import org.apache.xerces.xs.XSSimpleTypeDefinition;

public interface ElementContainer {

	public void setNull();
	public void setAttribute(String Name, String value, XSSimpleTypeDefinition attTypeDefinition);
	public void characters(char[] ch, int start, int length, boolean numericType, boolean booleanType);
}
