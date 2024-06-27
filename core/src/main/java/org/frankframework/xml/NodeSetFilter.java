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
package org.frankframework.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Filter that copies only a single element type, and/or its contents.
 * Optionally the root is copied too.
 *
 * @author Gerrit van Brakel
 */
public class NodeSetFilter extends FullXmlFilter {

	private String targetNamespace;
	private String targetElement;
	private boolean includeTarget=false;
	private boolean includeRoot=false;

	private int level;
	private int globalLevel;
	private boolean inTargetElement;
	private boolean copying;

	private final List<PrefixMapping> pendingNamespaceDefinitions=new ArrayList<>();

	public NodeSetFilter(String targetElement, ContentHandler handler) {
		this(null, targetElement, true, false, handler);
	}

	public NodeSetFilter(Map<String,String> namespaceMap, String targetElement, boolean includeTarget, boolean includeRoot, ContentHandler handler) {
		super(handler);
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
	}

	/**
	 * Called before processing of each node of the NodeSet is started.
	 */
	public void startNode(String uri, String localName, String qName) throws SAXException {
		// can be implemented by descender classes when necessary
	}
	/**
	 * Called every time the processing of a node of the NodeSet has finished.
	 */
	public void endNode(String uri, String localName, String qName) throws SAXException {
		// can be implemented by descender classes when necessary
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		if (copying || includeRoot && globalLevel==0) {
			super.startPrefixMapping(prefix, uri);
		} else {
			pendingNamespaceDefinitions.add(new PrefixMapping(prefix, uri));
		}
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		if (copying || includeRoot && globalLevel==0) {
			super.endPrefixMapping(prefix);
		} else {
			if (pendingNamespaceDefinitions.size()<=0) {
				log.warn("pendingNamespaceDefinitions empty, cannot remove prefix [{}]", prefix);
				return;
			}
			PrefixMapping topMapping=pendingNamespaceDefinitions.remove(pendingNamespaceDefinitions.size()-1);
//			if (!prefix.equals(topMapping.getPrefix())) {
//				// do this on debug, because removal is not always in order, but in order of definition in the element
//				if (log.isTraceEnabled()) log.trace("prefixmapping to remove with prefix ["+prefix+"] does not match expected ["+topMapping.getPrefix()+"]");
//			}
		}
	}

	private boolean onTargetElement(String uri, String localName) {
		return (targetNamespace==null || targetNamespace.equals(uri)) && (targetElement==null || localName.equals(targetElement));
	}

	private boolean mappingIsOverridden(PrefixMapping mapping, int index) {
		for (int i=index+1; i<pendingNamespaceDefinitions.size(); i++) {
			if (pendingNamespaceDefinitions.get(i).getPrefix().equals(mapping.getPrefix())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (!inTargetElement) {
			if (onTargetElement(uri, localName)) {
				inTargetElement=true;
				level=1;
			}
		} else {
			level++;
		}
		if (level>1 || (includeTarget && level==1) || (includeRoot && globalLevel==0)) {
			if (!copying) {
				copying=true;
				startNode(uri, localName, qName);
				for (int i=0; i<pendingNamespaceDefinitions.size(); i++) {
					PrefixMapping mapping=pendingNamespaceDefinitions.get(i);
					if (!mappingIsOverridden(mapping, i)) {
						startPrefixMapping(mapping.getPrefix(), mapping.getUri());
					}
				}
			}
			super.startElement(uri, localName, qName, atts);
		}
		globalLevel++;
	}


	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		globalLevel--;
		if (--level>0 || (includeTarget && level==0) || (includeRoot && globalLevel==0)) {
			super.endElement(uri, localName, qName);
			if (level == (includeTarget ? 0 : 1)) {
				for(int i=pendingNamespaceDefinitions.size()-1;i>=0;i--) {
					PrefixMapping mapping=pendingNamespaceDefinitions.get(i);
					if (!mappingIsOverridden(mapping, i)) {
						endPrefixMapping(mapping.getPrefix());
					}
				}
				endNode(uri, localName, qName);
				copying=false;
			}
		}
		if (level<=0) {
			inTargetElement=false;
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
		if (copying) {
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
