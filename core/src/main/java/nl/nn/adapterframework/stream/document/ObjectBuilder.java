/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.stream.document;

import org.xml.sax.SAXException;

public abstract class ObjectBuilder extends StructureBuilder implements IObjectBuilder {

	public void add(String name, String value) throws SAXException {
		addField(name).setValue(value);
	}

	public void add(String name, long value) throws SAXException {
		addField(name).setValue(value);
	}

	public void add(String name, boolean value) throws SAXException {
		addField(name).setValue(value);
	}
	
	public ObjectBuilder addObjectField(String name) throws SAXException {
		INodeBuilder field = addField(name);
		ObjectBuilder result = field.startObject();
		result.field=field;
		return result;
	}

	public ArrayBuilder addArrayField(String name, String elementName) throws SAXException {
		INodeBuilder field = addField(name);
		ArrayBuilder result = field.startArray(elementName);
		result.field=field;
		return result;
	}

}
