/*
   Copyright 2021, 2022 WeAreFrank!

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

public interface JsonEventHandler {

	void startDocument() throws SAXException;

	void endDocument() throws SAXException;

	void startObject() throws SAXException;

	void startObjectEntry(String key) throws SAXException;

	void endObject() throws SAXException;

	void startArray() throws SAXException;

	void endArray() throws SAXException;

	// Must be able to handle String, long, BigDecimal, boolean, Date and null
	void primitive(Object value) throws SAXException;

	// handles a string value as numeric (i.e without quotes in JSON)
	void number(String value) throws SAXException;
}
