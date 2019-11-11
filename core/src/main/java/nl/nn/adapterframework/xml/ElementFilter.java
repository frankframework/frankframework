/*
   Copyright 2019 Integration Partners

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

import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Filter that copies only a single element type, and/or its contents.
 * Optionally the root is copied too.
 * 
 * @author Gerrit van Brakel
 */
public class ElementFilter extends FullXmlFilter {

	private String targetNamespace;
	private String targetElement;
	private boolean includeTarget=false;
	private boolean includeRoot=false;
	
	private int level;
	private int globalLevel;
	
	public ElementFilter(String targetElement) {
		this(null, targetElement,true,false);
	}

	public ElementFilter(Map<String,String> namespaceMap, String targetElement, boolean includeTarget, boolean includeRoot) {
		super();
		this.includeTarget=includeTarget;
		this.includeRoot=includeRoot;
		if (namespaceMap==null) {
			this.targetElement=targetElement;
		} else {
			if (targetElement==null) {
				this.targetNamespace=namespaceMap.get(null); 
			} else {
				int colonPos=targetElement.indexOf(':');
				if (colonPos<0) {
					this.targetNamespace=namespaceMap.get(null); 
					this.targetElement=targetElement;
				} else {
					this.targetNamespace=namespaceMap.get(targetElement.substring(0,colonPos)); 
					this.targetElement=targetElement.substring(colonPos+1);				
				}
			}
		}
		log.debug("ElementFilter targetNamespace ["+targetNamespace+"] targetElement ["+targetElement+"]");
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (level<=0) {
			if ((targetNamespace==null || targetNamespace.equals(uri)) && (targetElement==null || localName.equals(targetElement))) {
				level=1;
			}
		} else {
			level++;
		}
		if (level>1 || (includeTarget && level==1) || (includeRoot && globalLevel==0)) {
			super.startElement(uri, localName, qName, atts);
		}
		globalLevel++;
	}


	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		globalLevel--;
		if (--level>0 || (includeTarget && level==0) || (includeRoot && globalLevel==0)) {
			super.endElement(uri, localName, qName);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (level>0) {
			super.characters(ch, start, length);
		}
	}


	@Override
	public void comment(char[] ch, int start, int length) throws SAXException {
		if (level>0) {
			super.comment(ch, start, length);
		}
	}

	@Override
	public void startCDATA() throws SAXException {
		if (level>0) {
			super.startCDATA();
		}
	}

	@Override
	public void endCDATA() throws SAXException {
		if (level>0) {
			super.endCDATA();
		}
	}

	@Override
	public void startEntity(String name) throws SAXException {
		if (level>0) {
			super.startEntity(name);
		}
	}

	@Override
	public void endEntity(String name) throws SAXException {
		if (level>0) {
			super.endEntity(name);
		}
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		if (level>0) {
			super.ignorableWhitespace(ch, start, length);
		}
	}


}
