/*
   Copyright 2022 WeAreFrank!

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
package org.frankframework.configuration.digester;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.core.Resource;
import org.frankframework.util.XmlUtils;
import org.frankframework.xml.BodyOnlyFilter;
import org.frankframework.xml.ClassLoaderURIResolver;
import org.frankframework.xml.FullXmlFilter;
import org.frankframework.xml.SaxException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

public class IncludeFilter extends FullXmlFilter {

	private static final char[] NEWLINE = "\n".toCharArray();

	private static final String TARGET_ELEMENT = "Include";
	private static final String TARGET_ATTRIBUTE = "ref";

	private final Resource resource;
	private final ClassLoaderURIResolver uriResolver;

	public IncludeFilter(ContentHandler handler, Resource resource) {
		super(handler);
		this.resource = resource;
		uriResolver = new ClassLoaderURIResolver(resource);
	}

	private void comment(String message) throws SAXException {
		comment(message.toCharArray(), 0, message.length());
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (localName.equals(TARGET_ELEMENT)) {
			String ref = atts.getValue(TARGET_ATTRIBUTE);
			characters(NEWLINE, 0, 1);
			comment("start include '"+ref+"'");
			characters(NEWLINE, 0, 1);
			if (StringUtils.isNotEmpty(ref)) {
				Resource subResource;
				try {
					subResource = uriResolver.resolveToResource(ref, resource.getSystemId());
				} catch (TransformerException e) {
					throw new SaxException("Cannot open include ["+ref+"]", e);
				}
				if (subResource==null) {
					throw new SaxException("Cannot find include ["+ref+"]");
				}
				XMLFilterImpl handlerTail = new BodyOnlyFilter(getContentHandler(), false);
				ContentHandler includeHandler = handlerTail;
				// the below filters need to be included if the filter is placed higher in the chain
				// includeHandler = new OnlyActiveFilter(includeHandler, appConstants);
				// includeHandler = new ElementPropertyResolver(includeHandler, appConstants);
				includeHandler = new IncludeFilter(includeHandler, subResource);
				XMLReader curParent = getParent();
				try {
					if (curParent!=null) {
						setParent(handlerTail);
					}
					XmlUtils.parseXml(subResource, includeHandler);
				} catch (IOException e) {
					throw new SaxException("Cannot parse include ["+ref+"]", e);
				} finally {
					if (curParent!=null) {
						setParent(curParent);
					}
				}
			}
			characters(NEWLINE, 0, 1);
			comment("end include '"+ref+"'");
			return;
		}
		super.startElement(uri, localName, qName, atts);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (localName.equals(TARGET_ELEMENT)) {
			return;
		}
		super.endElement(uri, localName, qName);
	}

}
