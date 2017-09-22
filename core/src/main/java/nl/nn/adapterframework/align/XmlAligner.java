/*
   Copyright 2017 Nationale-Nederlanden

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
package nl.nn.adapterframework.align;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;
import org.apache.xerces.xs.ElementPSVI;
import org.apache.xerces.xs.PSVIProvider;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * XMLFilter with option to get schema information about child elements to be parsed.
 * 
 * @author Gerrit van Brakel
 */
public class XmlAligner extends XMLFilterImpl {
	protected Logger log = Logger.getLogger(this.getClass());

	private PSVIProvider psviProvider;
	private boolean indent=true;
	private final boolean DEBUG=false; 

	private int indentLevel;
	protected List<XSParticle> childElementDeclarations=null;
	protected Stack<Set<String>> multipleOccurringElements=new Stack<Set<String>>();
	protected Set<String> multipleOccurringChildElements=null;
	protected Stack<Boolean> parentOfSingleMultipleOccuringChildElements=new Stack<Boolean>();
	protected boolean parentOfSingleMultipleOccuringChildElement=false;

	private final char[] INDENTOR="\n                                                                                         ".toCharArray();
	private final int MAX_INDENT=INDENTOR.length/2;
	
	
	public XmlAligner() {
		super();
	}

	public XmlAligner(PSVIProvider psviProvider) {
		this();
		setPsviProvider(psviProvider);
	}

	public XmlAligner(XMLReader psviProvidingXmlReader) {
		this((PSVIProvider)psviProvidingXmlReader);
		psviProvidingXmlReader.setContentHandler(this);
	}

	public XmlAligner(ValidatorHandler psviProvidingValidatorHandler) {
		this((PSVIProvider)psviProvidingValidatorHandler);
		psviProvidingValidatorHandler.setContentHandler(this);
	}



	public void newLine() throws SAXException {
		newLine(0);
	}
	public void newLine(int offset) throws SAXException {
		if (indent) {
			int level=indentLevel+offset;
			ignorableWhitespace(INDENTOR, 0, (level<MAX_INDENT?level:MAX_INDENT)*2+1);
		}
	}
	
	@Override
	public void startElement(String namespaceUri, String localName, String qName, Attributes attributes) throws SAXException {
		if (DEBUG && log.isDebugEnabled()) log.debug("startElement() uri ["+namespaceUri+"] localName ["+localName+"] qName ["+qName+"]");
		// call getChildElementDeclarations with in startElement, to obtain all child elements of the current node
		childElementDeclarations=getChildElementDeclarations(psviProvider);
		multipleOccurringElements.push(multipleOccurringChildElements);
		// call findMultipleOccurringChildElements, to obtain all child elements that could be part of an array
		multipleOccurringChildElements=findMultipleOccurringChildElements();
		parentOfSingleMultipleOccuringChildElements.push(parentOfSingleMultipleOccuringChildElement);
		parentOfSingleMultipleOccuringChildElement=isParentOfSingleMultipleOccurringChildElement();
		super.startElement(namespaceUri, localName, qName, attributes);
		indentLevel++;
	}
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (DEBUG && log.isDebugEnabled()) log.debug("endElement() uri ["+uri+"] localName ["+localName+"] qName ["+qName+"]");
		indentLevel--;
		super.endElement(uri, localName, qName);
		multipleOccurringChildElements=multipleOccurringElements.pop();
		parentOfSingleMultipleOccuringChildElement=parentOfSingleMultipleOccuringChildElements.pop();
	}


	public boolean isPresentInSet(Set<String> set, String name) {
		return set!=null && set.contains(name);
	}
	public boolean isMultipleOccuringOnlyChildElement(String name) {
		return isPresentInSet(multipleOccurringChildElements,name);
	}
	public boolean isMultipleOccuringChildInParentElement(String name) {
		return isPresentInSet(multipleOccurringElements.peek(),name);
	}


	protected boolean isSingleMultipleOccurringChildElement() {
		return parentOfSingleMultipleOccuringChildElements.peek();
	}
	
	protected boolean isParentOfSingleMultipleOccurringChildElement() {
		if (childElementDeclarations==null || childElementDeclarations.isEmpty()) {
			if (DEBUG && log.isDebugEnabled()) log.debug("isParentOfSingleMultipleOccurringChildElement() no childElementDeclarations");
			return false;
		}
		if (childElementDeclarations.size()>1) {
			if (DEBUG && log.isDebugEnabled()) log.debug("isParentOfSingleMultipleOccurringChildElement() multiple childElementDeclarations");
			return false;
		}
		XSParticle particle=childElementDeclarations.get(0);
		XSTerm term = particle.getTerm();
		if (DEBUG && log.isDebugEnabled()) log.debug("isParentOfSingleMultipleOccurringChildElement() single child element ["+term.getName()+"] occurring unbounded ["+particle.getMaxOccursUnbounded()+"] max occur ["+particle.getMaxOccurs()+"]");
		return particle.getMaxOccursUnbounded() || particle.getMaxOccurs()>1;
	}

	protected Set<String> findMultipleOccurringChildElements() {
		if (childElementDeclarations==null) {
			if (DEBUG && log.isDebugEnabled()) log.debug("findMultipleOccurringChildElements() no childElementDeclarations");
			return null;
		}
		Set<String> result=new HashSet<String>();
		for (XSParticle particle:childElementDeclarations) {
			XSTerm term = particle.getTerm();
			String name=term.getName();
			if (DEBUG && log.isDebugEnabled()) log.debug("findMultipleOccurringChildElements() child element ["+name+"] occurring unbounded ["+particle.getMaxOccursUnbounded()+"] max occur ["+particle.getMaxOccurs()+"]");
			if (particle.getMaxOccursUnbounded() || particle.getMaxOccurs()>1) {
				if (DEBUG && log.isDebugEnabled()) log.debug("findMultipleOccurringChildElements() multiple occurring child element ["+name+"]");
				result.add(name);
			}
		}
		return result;
	}
	public List<XSParticle> getChildElementDeclarations(PSVIProvider psviProvider) {
		ElementPSVI elementPSVI = psviProvider.getElementPSVI();
		if (DEBUG && log.isDebugEnabled()) log.debug("elementPSVI ["+ToStringBuilder.reflectionToString(elementPSVI)+"]");
		XSElementDeclaration elementDeclaration = elementPSVI.getElementDeclaration();
		if (DEBUG && log.isDebugEnabled()) log.debug("elementPSVI element declaration ["+ToStringBuilder.reflectionToString(elementDeclaration)+"]");
		if (elementDeclaration==null) {
			return null;
		}
		XSTypeDefinition typeDefinition = elementDeclaration.getTypeDefinition();
		if (DEBUG && log.isDebugEnabled()) log.debug("elementDeclaration typeDefinition ["+ToStringBuilder.reflectionToString(typeDefinition)+"]");
		
		if (typeDefinition instanceof XSComplexTypeDefinition) {
			XSComplexTypeDefinition complexTypeDefinition = (XSComplexTypeDefinition)typeDefinition;
			XSParticle particle = complexTypeDefinition.getParticle();
			if (particle==null) {
				log.warn("typeDefinition particle is null, is this a problem?");					
			} else {
				if (DEBUG && log.isDebugEnabled()) log.debug("typeDefinition particle ["+ToStringBuilder.reflectionToString(particle,ToStringStyle.MULTI_LINE_STYLE)+"]");
				XSTerm term = particle.getTerm();
				if (DEBUG && log.isDebugEnabled()) log.debug("particle term ["+ToStringBuilder.reflectionToString(term,ToStringStyle.MULTI_LINE_STYLE)+"]");
				
				if (term instanceof XSModelGroup) {
					XSModelGroup modelGroup = (XSModelGroup)term;
					short compositor = modelGroup.getCompositor();
					if (compositor==XSModelGroup.COMPOSITOR_SEQUENCE || compositor==XSModelGroup.COMPOSITOR_ALL) {
						XSObjectList particles = modelGroup.getParticles();
						if (DEBUG && log.isDebugEnabled()) log.debug("modelGroup particles ["+ToStringBuilder.reflectionToString(particles,ToStringStyle.MULTI_LINE_STYLE)+"]");
						List<XSParticle> result = new LinkedList<XSParticle>();

						for (int i=0;i<particles.getLength();i++) {
							XSParticle childParticle = (XSParticle)particles.item(i);
							XSTerm childTerm = childParticle.getTerm();
							if (childTerm instanceof XSElementDeclaration) {
								XSElementDeclaration childElementDeclaration=(XSElementDeclaration)childTerm;
//								if (DEBUG && log.isDebugEnabled()) log.debug("childElementDeclaration ["+ToStringBuilder.reflectionToString(childElementDeclaration,ToStringStyle.MULTI_LINE_STYLE)+"]");
								if (DEBUG && log.isDebugEnabled()) log.debug("childElementDeclaration name ["+childElementDeclaration.getName()+"]");
								result.add(childParticle);
							} else {
								log.warn("IGNORING childTerm because it is not a XSElementDeclaration, but is ["+childTerm.getClass().getName()+"]");
								if (DEBUG && log.isDebugEnabled()) log.debug("childTerm ["+ToStringBuilder.reflectionToString(childTerm,ToStringStyle.MULTI_LINE_STYLE)+"]");
							}
						}
						childElementDeclarations=result;
						return result;
					} 
					if (compositor==XSModelGroup.COMPOSITOR_CHOICE) {
						log.warn("SHOULD HANDLE properly: modelGroup compositor is a COMPOSITOR_CHOICE");
					} else {
						log.warn("UNKNOWN compositor in modelGroup ["+compositor+"], not COMPOSITOR_SEQUENCE, COMPOSITOR_ALL or COMPOSITOR_CHOICE");
					}
				} else {
					log.warn("IGNORING particle term because it is not a XSModelGroup but a ["+term.getClass().getName()+"]");
				}
			}
		} else {
			if (DEBUG && log.isDebugEnabled()) log.debug("simple type, no children");
		}
		return null;
	}

	
	public void setPsviProvider(PSVIProvider psviProvider) {
		this.psviProvider=psviProvider;
	}
	
}