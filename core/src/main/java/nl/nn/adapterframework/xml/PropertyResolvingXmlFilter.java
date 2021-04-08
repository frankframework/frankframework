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
package nl.nn.adapterframework.xml;

import java.util.Properties;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.util.StringResolver;

public class PropertyResolvingXmlFilter extends FullXmlFilter {
	private Properties properties;
	private StringBuffer pendingSubstBuff = new StringBuffer();

	public PropertyResolvingXmlFilter(ContentHandler contentHandler, Properties properties) {
		super(contentHandler);
		if(properties == null) {
			throw new IllegalArgumentException("no properties defined");
		}

		this.properties = properties;
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		String characters = new String(ch, start, length);
		System.err.println("characters- " + start + " \t " + length + "\t" + characters.trim());

		if(pendingSubstBuff.length() > 0) {
			if(characters.contains(StringResolver.DELIM_STOP)) {
				pendingSubstBuff.append(characters);
				flushBuffer();
				return;
			}
		}

		if(characters.contains(StringResolver.DELIM_START)) {//TODO test if char[] contains 2 properties
			if(!characters.contains(StringResolver.DELIM_STOP)) { //store in buffer, we don't have the entire property to substitute
				pendingSubstBuff.append(characters);
				return;
			}

			pendingSubstBuff.append(characters);
			flushBuffer();
			return;
		}

		// No start or stop was found, assume we are in a property, append all
		pendingSubstBuff.append(characters);
	}

	private void flushBuffer() throws SAXException {
		String resolved = StringResolver.substVars(pendingSubstBuff.toString(), properties);
		super.characters(resolved.toCharArray(), 0, resolved.length());//TODO don't parse the resolved data as characters, it could contain xml elements
		pendingSubstBuff.setLength(0);
	}
}
