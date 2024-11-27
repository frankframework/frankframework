/*
   Copyright 2020 WeAreFrank!

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
package org.frankframework.documentbuilder.xml;

import org.frankframework.xml.XmlWriter;
import org.xml.sax.ContentHandler;

public class XmlTap extends XmlTee {

	public XmlTap() {
		super();
		setSecondContentHandler(new XmlWriter());
	}

	public XmlTap(ContentHandler handler) {
		super(handler, new XmlWriter());
	}

	public XmlWriter getWriter() {
		return (XmlWriter)getSecondContentHandler();
	}
}
