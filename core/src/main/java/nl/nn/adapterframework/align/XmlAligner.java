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
import java.util.Set;
import java.util.Stack;

import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.apache.xerces.impl.dv.XSSimpleType;
import org.apache.xerces.xs.ElementPSVI;
import org.apache.xerces.xs.PSVIProvider;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import org.apache.xerces.xs.XSWildcard;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * XMLFilter with option to get schema information about child elements to be parsed.
 * 
 * @author Gerrit van Brakel
 */
public class XmlAligner extends XMLFilterImpl {
	protected Logger log = Logger.getLogger(this.getClass());
	
	public final String FEATURE_NAMESPACES="http://xml.org/sax/features/namespaces";
	public final String FEATURE_NAMESPACE_PREFIXES="http://xml.org/sax/features/namespace-prefixes";

	private final int CHILD_OCCURRENCE_EMPTY=0;
	private final int CHILD_OCCURRENCE_ONE_SINGLE_OCCURRING_ELEMENT=1;
	private final int CHILD_OCCURRENCE_ONE_MULTIPLE_OCCURRING_ELEMENT=2;
	private final int CHILD_OCCURRENCE_MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING=3;

	private final String[] CHILD_OCCURRENCE_DESCRIPTION={"Empty","OneSingle","OneMultiOcc","Mixed"};
	
	private PSVIProvider psviProvider;
	private boolean indent=true;
	private final boolean DEBUG=false; 

	private int indentLevel;
	private XSTypeDefinition typeDefinition;


	private Stack<Set<String>> multipleOccurringElements=new Stack<Set<String>>();
	private Set<String> multipleOccurringChildElements=null;
	private Stack<Boolean> parentOfSingleMultipleOccurringChildElements=new Stack<Boolean>();
	private boolean parentOfSingleMultipleOccurringChildElement=false;

	private final char[] INDENTOR="\n                                                                                         ".toCharArray();
	private final int MAX_INDENT=INDENTOR.length/2;

	public String XML_SCHEMA_INSTANCE_NAMESPACE="http://www.w3.org/2001/XMLSchema-instance";
	public String XML_SCHEMA_NIL_ATTRIBUTE="nil";

	
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
	
	public boolean isNil(Attributes attributes) {
		return "true".equals(attributes.getValue(XML_SCHEMA_INSTANCE_NAMESPACE, XML_SCHEMA_NIL_ATTRIBUTE));
	}
	
	@Override
	public void startElement(String namespaceUri, String localName, String qName, Attributes attributes) throws SAXException {
		if (DEBUG) log.debug("startElement() uri ["+namespaceUri+"] localName ["+localName+"] qName ["+qName+"]");
		// call getChildElementDeclarations with in startElement, to obtain all child elements of the current node
		typeDefinition=getTypeDefinition(psviProvider);
		multipleOccurringElements.push(multipleOccurringChildElements);
		parentOfSingleMultipleOccurringChildElements.push(parentOfSingleMultipleOccurringChildElement);
		// call findMultipleOccurringChildElements, to obtain all child elements that could be part of an array
		if (typeDefinition instanceof XSComplexTypeDefinition) {
			XSComplexTypeDefinition complexTypeDefinition = (XSComplexTypeDefinition)typeDefinition;
			multipleOccurringChildElements=findMultipleOccurringChildElements(complexTypeDefinition.getParticle());
			parentOfSingleMultipleOccurringChildElement=(CHILD_OCCURRENCE_ONE_MULTIPLE_OCCURRING_ELEMENT==determineIsParentOfSingleMultipleOccurringChildElement(complexTypeDefinition.getParticle()));
			if (DEBUG) log.debug("element ["+localName+"] is parentOfSingleMultipleOccurringChildElement ["+parentOfSingleMultipleOccurringChildElement+"]");
		} else {
			multipleOccurringChildElements=null;
			parentOfSingleMultipleOccurringChildElement=false;
			if (DEBUG) log.debug("element ["+localName+"] is a SimpleType, and therefor not multiple");
		}
		super.startElement(namespaceUri, localName, qName, attributes);
		indentLevel++;
	}
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (DEBUG) log.debug("endElement() uri ["+uri+"] localName ["+localName+"] qName ["+qName+"]");
		indentLevel--;
		typeDefinition=null;
		super.endElement(uri, localName, qName);
		multipleOccurringChildElements=multipleOccurringElements.pop();
		parentOfSingleMultipleOccurringChildElement=parentOfSingleMultipleOccurringChildElements.pop();
	}


	public boolean isPresentInSet(Set<String> set, String name) {
		return set!=null && set.contains(name);
	}
	
	public boolean isMultipleOccurringChildInParentElement(String name) {
		return isPresentInSet(multipleOccurringElements.peek(),name);
	}

	public boolean isMultipleOccurringChildElement(String name) {
		return isPresentInSet(multipleOccurringChildElements,name);
	}

	public Set<String> getMultipleOccurringChildElements() {
		return multipleOccurringChildElements;
	}


	protected boolean isParentOfSingleMultipleOccurringChildElement() {
		return parentOfSingleMultipleOccurringChildElement;
	}
	
	
	protected int determineIsParentOfSingleMultipleOccurringChildElement(XSParticle particle) {
		if (particle==null) {
			log.warn("Particle is null, is this a problem? Appearantly not");
			return CHILD_OCCURRENCE_EMPTY;
		} 
		XSTerm term = particle.getTerm();
		if (term==null) {
			throw new IllegalStateException("determineIsParentOfSingleMultipleOccurringChildElement particle.term is null");
		} 
		if (DEBUG) log.debug("determineIsParentOfSingleMultipleOccurringChildElement() term name ["+term.getName()+"] occurring unbounded ["+particle.getMaxOccursUnbounded()+"] max occur ["+particle.getMaxOccurs()+"] term ["+ToStringBuilder.reflectionToString(term)+"]");
		if (term instanceof XSModelGroup) {
			XSModelGroup modelGroup = (XSModelGroup)term;
			short compositor = modelGroup.getCompositor();			
			XSObjectList particles = modelGroup.getParticles();
			switch (compositor) {
			case XSModelGroup.COMPOSITOR_SEQUENCE:
			case XSModelGroup.COMPOSITOR_ALL: {
				if (DEBUG) log.debug("determineIsParentOfSingleMultipleOccurringChildElement() sequence or all particles ["+ToStringBuilder.reflectionToString(particles)+"]");
				int result=CHILD_OCCURRENCE_EMPTY;
				for (int i=0;i<particles.getLength();i++) {
					XSParticle childParticle = (XSParticle)particles.item(i);
					int current=determineIsParentOfSingleMultipleOccurringChildElement(childParticle);
					if (DEBUG) log.debug("determineIsParentOfSingleMultipleOccurringChildElement() sequence or all, particle ["+i+"] current result ["+CHILD_OCCURRENCE_DESCRIPTION[current]+"]");
					switch (current) {
					case CHILD_OCCURRENCE_EMPTY:
						break;
					case CHILD_OCCURRENCE_ONE_SINGLE_OCCURRING_ELEMENT:
					case CHILD_OCCURRENCE_ONE_MULTIPLE_OCCURRING_ELEMENT:
						if (result>CHILD_OCCURRENCE_EMPTY) {
							if (DEBUG) log.debug("determineIsParentOfSingleMultipleOccurringChildElement() sequence or all, result ["+CHILD_OCCURRENCE_DESCRIPTION[result]+"] current ["+CHILD_OCCURRENCE_DESCRIPTION[current]+"]");
							return CHILD_OCCURRENCE_MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING;
						}
						result=current;
						break;
					case CHILD_OCCURRENCE_MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING:
						return CHILD_OCCURRENCE_MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING;
					default:
						throw new IllegalStateException("determineIsParentOfSingleMultipleOccurringChildElement child occurrence ["+CHILD_OCCURRENCE_DESCRIPTION[current]+"]");
					}
				}
				if (DEBUG) log.debug("determineIsParentOfSingleMultipleOccurringChildElement() end of sequence or all, returning ["+CHILD_OCCURRENCE_DESCRIPTION[result]+"]");
				return result;
			}
			case XSModelGroup.COMPOSITOR_CHOICE: {
				if (DEBUG) log.debug("determineIsParentOfSingleMultipleOccurringChildElement() choice particles ["+ToStringBuilder.reflectionToString(particles)+"]");
				if (particles.getLength()==0) {
					if (DEBUG) log.debug("determineIsParentOfSingleMultipleOccurringChildElement() choice length 0, returning ["+CHILD_OCCURRENCE_DESCRIPTION[CHILD_OCCURRENCE_MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING]+"]");
					return CHILD_OCCURRENCE_EMPTY;
				}
				int result=determineIsParentOfSingleMultipleOccurringChildElement((XSParticle)particles.item(0));
				if (result==CHILD_OCCURRENCE_MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING) {
					if (DEBUG) log.debug("determineIsParentOfSingleMultipleOccurringChildElement() choice single mixed, returning ["+CHILD_OCCURRENCE_DESCRIPTION[CHILD_OCCURRENCE_MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING]+"]");
					return CHILD_OCCURRENCE_MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING;
				}
				for (int i=1;i<particles.getLength();i++) {
					XSParticle childParticle = (XSParticle)particles.item(i);
					int current=determineIsParentOfSingleMultipleOccurringChildElement(childParticle);
					if (current!=result) {
						if (DEBUG) log.debug("determineIsParentOfSingleMultipleOccurringChildElement() break out of choice, returning ["+CHILD_OCCURRENCE_DESCRIPTION[CHILD_OCCURRENCE_MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING]+"]");
						return CHILD_OCCURRENCE_MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING;
					}
				}
				if (DEBUG) log.debug("determineIsParentOfSingleMultipleOccurringChildElement() end of choice, returning ["+CHILD_OCCURRENCE_DESCRIPTION[result]+"]");
				return result;
			}
			default:
				throw new IllegalStateException("determineIsParentOfSingleMultipleOccurringChildElement modelGroup.compositor is not COMPOSITOR_SEQUENCE, COMPOSITOR_ALL or COMPOSITOR_CHOICE, but ["+compositor+"]");
			} 
		} 
		if (term instanceof XSElementDeclaration) {
			XSElementDeclaration elementDeclaration=(XSElementDeclaration)term;
			String elementName=elementDeclaration.getName();
			if (DEBUG) log.debug("determineIsParentOfSingleMultipleOccurringChildElement() ElementDeclaration name ["+elementName+"] unbounded ["+particle.getMaxOccursUnbounded()+"] maxOccurs ["+particle.getMaxOccurs()+"]");
			if (particle.getMaxOccursUnbounded() || particle.getMaxOccurs()>1) {
				return CHILD_OCCURRENCE_ONE_MULTIPLE_OCCURRING_ELEMENT;
			}
			if (particle.getMaxOccurs()==1) {
				return CHILD_OCCURRENCE_ONE_SINGLE_OCCURRING_ELEMENT;
			} 
			return CHILD_OCCURRENCE_EMPTY;
		}
		if (term instanceof XSWildcard) {
			return CHILD_OCCURRENCE_MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING;
		} 
		throw new IllegalStateException("determineIsParentOfSingleMultipleOccurringChildElement unknown Term type ["+term.getClass().getName()+"]");
	}


	protected void collectChildElements(XSParticle particle, Set<String> elementNames) {
		if (particle==null) {
			log.warn("collectChildElements() particle is null, is this a problem?");	
			return;
		}
		XSTerm term = particle.getTerm();
		if (term==null) {
			throw new IllegalStateException("collectChildElements particle.term is null");
		} 
		if (term instanceof XSModelGroup) {
			XSModelGroup modelGroup = (XSModelGroup)term;
			XSObjectList particles = modelGroup.getParticles();
			for (int i=0;i<particles.getLength();i++) {
				XSParticle childParticle = (XSParticle)particles.item(i);
				collectChildElements(childParticle, elementNames);
			}
			return;
		} 
		if (term instanceof XSElementDeclaration) {
			XSElementDeclaration elementDeclaration=(XSElementDeclaration)term;
			String elementName=elementDeclaration.getName();
			if (DEBUG) log.debug("collectChildElements() ElementDeclaration name ["+elementName+"]");
			elementNames.add(elementName);
		}
		return;
	}
	
	protected Set<String> findMultipleOccurringChildElements(XSParticle particle) {
		Set<String> result=new HashSet<String>();
		if (particle==null) {
			log.warn("findMultipleOccurringChildElements() typeDefinition particle is null, is this a problem?");	
			return result;
		}
		XSTerm term = particle.getTerm();
		if (term==null) {
			throw new IllegalStateException("findMultipleOccurringChildElements particle.term is null");
		} 
		if (DEBUG) log.debug("findMultipleOccurringChildElements() term name ["+term.getName()+"] occurring unbounded ["+particle.getMaxOccursUnbounded()+"] max occur ["+particle.getMaxOccurs()+"] term ["+ToStringBuilder.reflectionToString(term)+"]");
		if (particle.getMaxOccursUnbounded()||particle.getMaxOccurs()>1) {
			collectChildElements(particle,result);
			return result;
		} 
		if (term instanceof XSModelGroup) {
			XSModelGroup modelGroup = (XSModelGroup)term;
			XSObjectList particles = modelGroup.getParticles();
				if (DEBUG) log.debug("findMultipleOccurringChildElements() modelGroup particles ["+ToStringBuilder.reflectionToString(particles)+"]");
				for (int i=0;i<particles.getLength();i++) {
					XSParticle childParticle = (XSParticle)particles.item(i);
					result.addAll(findMultipleOccurringChildElements(childParticle));
				}
		} 
		return result;
	}

	public XSObjectList getAttributeUses() {
		return getAttributeUses(typeDefinition);
	}
	
	public XSObjectList getAttributeUses(XSTypeDefinition typeDefinition) {
		if (typeDefinition==null) {
			if (DEBUG) log.debug("getAttributeUses typeDefinition is null");
			return null;
		}
		if (typeDefinition instanceof XSComplexTypeDefinition) {
			XSComplexTypeDefinition complexTypeDefinition=(XSComplexTypeDefinition)typeDefinition;
			return complexTypeDefinition.getAttributeUses();
		} 
		if (DEBUG) log.debug("typeDefinition ["+typeDefinition.getClass().getSimpleName()+"] SimpleType, no attributes");
		return null;
	}

	public XSTypeDefinition getTypeDefinition(PSVIProvider psviProvider) {
		ElementPSVI elementPSVI = psviProvider.getElementPSVI();
		if (DEBUG) log.debug("getTypeDefinition() elementPSVI ["+ToStringBuilder.reflectionToString(elementPSVI)+"]");
		XSElementDeclaration elementDeclaration = elementPSVI.getElementDeclaration();
		if (DEBUG) log.debug("getTypeDefinition() elementPSVI element declaration ["+ToStringBuilder.reflectionToString(elementDeclaration)+"]");
		if (elementDeclaration==null) {
			return null;
		}
		XSTypeDefinition typeDefinition = elementDeclaration.getTypeDefinition();
		if (DEBUG) log.debug("getTypeDefinition() elementDeclaration typeDefinition ["+ToStringBuilder.reflectionToString(typeDefinition)+"]");
		return typeDefinition;
	}
	

	public void setPsviProvider(PSVIProvider psviProvider) {
		this.psviProvider=psviProvider;
	}

	public XSTypeDefinition getTypeDefinition() {
		return typeDefinition;
	}

	public XSSimpleType getElementType() {
		if (typeDefinition instanceof XSSimpleType) {
			return (XSSimpleType)typeDefinition;
		}
		return null;
	}

	@Override
	public void setFeature(String feature, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
		log.warn("setting feature ["+feature+"] to ["+value+"]");
		if (feature.equals(FEATURE_NAMESPACES)) {
			if (!value) {
				throw new SAXNotSupportedException("Cannot set feature ["+feature+"] to ["+value+"]");
			}
			return;
		} 
		if (feature.equals(FEATURE_NAMESPACE_PREFIXES)) {
			if (value) {
				throw new SAXNotSupportedException("Cannot set feature ["+feature+"] to ["+value+"]");
			}
			return;
		} 
		super.setFeature(feature, value);
	}

}
