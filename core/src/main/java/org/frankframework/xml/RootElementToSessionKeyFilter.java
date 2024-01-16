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
package org.frankframework.xml;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import org.frankframework.core.PipeLineSession;

public class RootElementToSessionKeyFilter extends FullXmlFilter {

	private PipeLineSession session;
	private String rootElementSessionKey;
	private String rootNamespaceSessionKey;

	private boolean rootElementParsed;

	public RootElementToSessionKeyFilter(PipeLineSession session, String rootElementSessionKey, String rootNamespaceSessionKey, ContentHandler handler) {
		super(handler);
		if (session!=null) {
			this.session=session;
			this.rootElementSessionKey=rootElementSessionKey;
			this.rootNamespaceSessionKey=rootNamespaceSessionKey;
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (!rootElementParsed) {
			rootElementParsed=true;
			if (session!=null) {
				if (StringUtils.isNotEmpty(rootElementSessionKey)) {
					session.put(rootElementSessionKey, localName);
				}
				if (StringUtils.isNotEmpty(rootNamespaceSessionKey)) {
					session.put(rootNamespaceSessionKey, uri);
				}
			}
		}
		super.startElement(uri, localName, qName, atts);
	}
}
