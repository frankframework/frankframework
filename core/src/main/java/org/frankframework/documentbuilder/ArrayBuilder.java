/*
   Copyright 2021, 2023 WeAreFrank!

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
package org.frankframework.documentbuilder;

import org.xml.sax.SAXException;

public abstract class ArrayBuilder extends StructureBuilder implements IArrayBuilder {

	public void addElement(String value) throws SAXException {
		addElement().setValue(value);
	}

	public void addElement(Number value) throws SAXException {
		addElement().setValue(value);
	}

	public void addElement(boolean value) throws SAXException {
		addElement().setValue(value);
	}

	public void addNumberElement(String value) throws SAXException {
		addElement().setNumberValue(value);
	}

	public ObjectBuilder addObjectElement() throws SAXException {
		INodeBuilder field = addElement();
		ObjectBuilder result = field.startObject();
		result.field=field;
		return result;
	}

	public ArrayBuilder addArrayElement(String elementName) throws SAXException {
		INodeBuilder field = addElement();
		ArrayBuilder result = field.startArray(elementName);
		result.field=field;
		return result;
	}

}
