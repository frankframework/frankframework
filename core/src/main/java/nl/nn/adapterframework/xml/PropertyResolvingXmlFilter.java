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
//		System.out.println("characters: " + start + " \t " + length + "\t" + characters.trim());

		if(characters.contains(StringResolver.DELIM_START) || pendingSubstBuff.length() > 0) {
			pendingSubstBuff.append(characters);

			flushBuffer();
			return;
		}

		super.characters(ch, start, length);
	}

	//If a complete substitution is possible flush, else keep buffer.
	private void flushBuffer() throws SAXException {
		while(pendingSubstBuff.indexOf(StringResolver.DELIM_STOP) > 0) { //There could be multiple properties in the buffer
			substitude();
		}

		//Check if whatever is left in the buffer is part of a property. If not, we can flush it as well.
		if(pendingSubstBuff.indexOf(StringResolver.DELIM_START) == -1) {
			String remainder = pendingSubstBuff.toString();
			super.characters(remainder.toCharArray(), 0, remainder.length());
			pendingSubstBuff.setLength(0);
		}
	}

	private void substitude() throws SAXException {
		int start = pendingSubstBuff.indexOf(StringResolver.DELIM_START);
		int stop  = pendingSubstBuff.indexOf(StringResolver.DELIM_STOP);

		if(start > -1 && stop > -1) {//get the first property, there could be more
			String buff = pendingSubstBuff.substring(0, stop +1); // append StringResolver.DELIM_STOP.length() to stop.
			pendingSubstBuff.delete(start, stop+1);

			String resolved = StringResolver.substVars(buff, properties);
			super.characters(resolved.toCharArray(), 0, resolved.length());//TODO don't parse the resolved data as characters, it could contain xml elements
		}
	}
}
