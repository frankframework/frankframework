/*
   Copyright 2023 WeAreFrank!

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
package nl.nn.adapterframework.xml;

import java.util.Objects;

import org.xml.sax.Attributes;

public class WritableAttributes extends AttributesWrapper {

	public WritableAttributes(Attributes source) {
		super(source);
	}

	public void setValue(String qName, String value) {
		Attribute attribute = getAttributes().get(getIndex(qName));
		Objects.requireNonNull(attribute);
		attribute.value = value;
	}
}
