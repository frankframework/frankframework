/*
   Copyright 2017-2021 WeAreFrank!

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

import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.impl.dv.XSSimpleType;
import org.apache.xerces.impl.xs.XMLSchemaLoader;
import org.apache.xerces.xs.ElementPSVI;
import org.apache.xerces.xs.PSVIProvider;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
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
import org.xml.sax.helpers.XMLFilterImpl;

import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.xml.SaxException;

/**
 * XMLFilter with option to get schema information about child elements to be parsed.
 * 
 * @author Gerrit van Brakel
 */
public class XmlAligner extends XMLFilterImpl {
	protected Logger log = LogUtil.getLogger(this.getClass());
	
	public final String FEATURE_NAMESPACES="http://xml.org/sax/features/namespaces";
	public final String FEATURE_NAMESPACE_PREFIXES="http://xml.org/sax/features/namespace-prefixes";

	private PSVIProvider psviProvider;
	private boolean indent=true;

	private AlignmentContext context;
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

	private enum ChildOccurrence {
		EMPTY,ONE_SINGLE_OCCURRING_ELEMENT,ONE_MULTIPLE_OCCURRING_ELEMENT,MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING
	}

	
	public XmlAligner() {
		super();
	}

	private XmlAligner(PSVIProvider psviProvider) {
		this();
		setPsviProvider(psviProvider);
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
		if (log.isTraceEnabled()) log.trace("startElement() uri ["+namespaceUri+"] localName ["+localName+"] qName ["+qName+"]");
		// call getChildElementDeclarations with in startElement, to obtain all child elements of the current node
		typeDefinition=getTypeDefinition(psviProvider);
		if (typeDefinition==null) {
			throw new SaxException("No typeDefinition found for element ["+localName+"] in namespace ["+namespaceUri+"] qName ["+qName+"]");
		}
		multipleOccurringElements.push(multipleOccurringChildElements);
		parentOfSingleMultipleOccurringChildElements.push(parentOfSingleMultipleOccurringChildElement);
		// call findMultipleOccurringChildElements, to obtain all child elements that could be part of an array
		if (typeDefinition instanceof XSComplexTypeDefinition) {
			XSComplexTypeDefinition complexTypeDefinition = (XSComplexTypeDefinition)typeDefinition;
			multipleOccurringChildElements=findMultipleOccurringChildElements(complexTypeDefinition.getParticle());
			parentOfSingleMultipleOccurringChildElement=(ChildOccurrence.ONE_MULTIPLE_OCCURRING_ELEMENT==determineIsParentOfSingleMultipleOccurringChildElement(complexTypeDefinition.getParticle()));
			if (log.isTraceEnabled()) log.trace("element ["+localName+"] is parentOfSingleMultipleOccurringChildElement ["+parentOfSingleMultipleOccurringChildElement+"]");
		} else {
			multipleOccurringChildElements=null;
			parentOfSingleMultipleOccurringChildElement=false;
			if (log.isTraceEnabled()) log.trace("element ["+localName+"] is a SimpleType, and therefor not multiple");
		}
		super.startElement(namespaceUri, localName, qName, attributes);
		indentLevel++;
		context = new AlignmentContext(context, namespaceUri, localName, qName, attributes, typeDefinition, indentLevel, multipleOccurringChildElements, parentOfSingleMultipleOccurringChildElement);
	}
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (log.isTraceEnabled()) log.trace("endElement() uri ["+uri+"] localName ["+localName+"] qName ["+qName+"]");
		context = context.getParent();
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
	
	
	protected ChildOccurrence determineIsParentOfSingleMultipleOccurringChildElement(XSParticle particle) {
		if (particle==null) {
			log.warn("Particle is null, is this a problem? Appearantly not");
			return ChildOccurrence.EMPTY;
		} 
		XSTerm term = particle.getTerm();
		if (term==null) {
			throw new IllegalStateException("determineIsParentOfSingleMultipleOccurringChildElement particle.term is null");
		} 
		if (log.isTraceEnabled()) log.trace("term name ["+term.getName()+"] occurring unbounded ["+particle.getMaxOccursUnbounded()+"] max occur ["+particle.getMaxOccurs()+"] term ["+ToStringBuilder.reflectionToString(term)+"]");
		if (term instanceof XSModelGroup) {
			XSModelGroup modelGroup = (XSModelGroup)term;
			short compositor = modelGroup.getCompositor();			
			XSObjectList particles = modelGroup.getParticles();
			switch (compositor) {
			case XSModelGroup.COMPOSITOR_SEQUENCE:
			case XSModelGroup.COMPOSITOR_ALL: {
				if (log.isTraceEnabled()) log.trace("sequence or all particles ["+ToStringBuilder.reflectionToString(particles)+"]");
				ChildOccurrence result=ChildOccurrence.EMPTY;
				for (int i=0;i<particles.getLength();i++) {
					XSParticle childParticle = (XSParticle)particles.item(i);
					ChildOccurrence current=determineIsParentOfSingleMultipleOccurringChildElement(childParticle);
					if (log.isTraceEnabled()) log.trace("sequence or all, particle ["+i+"] current result ["+current+"]");
					switch (current) {
					case EMPTY:
						break;
					case ONE_SINGLE_OCCURRING_ELEMENT:
					case ONE_MULTIPLE_OCCURRING_ELEMENT:
						if (result.ordinal()>ChildOccurrence.EMPTY.ordinal()) {
							if (log.isTraceEnabled()) log.trace("sequence or all, result ["+result+"] current ["+current+"]");
							return ChildOccurrence.MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING;
						}
						result=current;
						break;
					case MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING:
						return ChildOccurrence.MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING;
					default:
						throw new IllegalStateException("determineIsParentOfSingleMultipleOccurringChildElement child occurrence ["+current+"]");
					}
				}
				if (log.isTraceEnabled()) log.trace("end of sequence or all, returning ["+result+"]");
				return result;
			}
			case XSModelGroup.COMPOSITOR_CHOICE: {
				if (log.isTraceEnabled()) log.trace("choice particles ["+ToStringBuilder.reflectionToString(particles)+"]");
				if (particles.getLength()==0) {
					if (log.isTraceEnabled()) log.trace("choice length 0, returning ["+ChildOccurrence.MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING+"]");
					return ChildOccurrence.EMPTY;
				}
				ChildOccurrence result=determineIsParentOfSingleMultipleOccurringChildElement((XSParticle)particles.item(0));
				if (result==ChildOccurrence.MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING) {
					if (log.isTraceEnabled()) log.trace("choice single mixed, returning ["+ChildOccurrence.MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING+"]");
					return ChildOccurrence.MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING;
				}
				for (int i=1;i<particles.getLength();i++) {
					XSParticle childParticle = (XSParticle)particles.item(i);
					ChildOccurrence current=determineIsParentOfSingleMultipleOccurringChildElement(childParticle);
					if (current!=result) {
						if (log.isTraceEnabled()) log.trace("break out of choice, returning ["+ChildOccurrence.MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING+"]");
						return ChildOccurrence.MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING;
					}
				}
				if (log.isTraceEnabled()) log.trace("end of choice, returning ["+result+"]");
				return result;
			}
			default:
				throw new IllegalStateException("determineIsParentOfSingleMultipleOccurringChildElement() modelGroup.compositor is not COMPOSITOR_SEQUENCE, COMPOSITOR_ALL or COMPOSITOR_CHOICE, but ["+compositor+"]");
			} 
		} 
		if (term instanceof XSElementDeclaration) {
			XSElementDeclaration elementDeclaration=(XSElementDeclaration)term;
			String elementName=elementDeclaration.getName();
			if (log.isTraceEnabled()) log.trace("ElementDeclaration name ["+elementName+"] unbounded ["+particle.getMaxOccursUnbounded()+"] maxOccurs ["+particle.getMaxOccurs()+"]");
			if (particle.getMaxOccursUnbounded() || particle.getMaxOccurs()>1) {
				return ChildOccurrence.ONE_MULTIPLE_OCCURRING_ELEMENT;
			}
			if (particle.getMaxOccurs()==1) {
				return ChildOccurrence.ONE_SINGLE_OCCURRING_ELEMENT;
			} 
			return ChildOccurrence.EMPTY;
		}
		if (term instanceof XSWildcard) {
			return ChildOccurrence.MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING;
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
			throw new IllegalStateException("collectChildElements() particle.term is null");
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
			if (log.isTraceEnabled()) log.trace("ElementDeclaration name ["+elementName+"]");
			elementNames.add(elementName);
		}
		return;
	}
	
	protected Set<String> findMultipleOccurringChildElements(XSParticle particle) {
		Set<String> result=new HashSet<String>();
		if (particle==null) {
			log.warn("typeDefinition particle is null, is this a problem?");	
			return result;
		}
		XSTerm term = particle.getTerm();
		if (term==null) {
			throw new IllegalStateException("findMultipleOccurringChildElements particle.term is null");
		} 
		if (log.isTraceEnabled()) log.trace("term name ["+term.getName()+"] occurring unbounded ["+particle.getMaxOccursUnbounded()+"] max occur ["+particle.getMaxOccurs()+"] term ["+ToStringBuilder.reflectionToString(term)+"]");
		if (particle.getMaxOccursUnbounded()||particle.getMaxOccurs()>1) {
			collectChildElements(particle,result);
			return result;
		} 
		if (term instanceof XSModelGroup) {
			XSModelGroup modelGroup = (XSModelGroup)term;
			XSObjectList particles = modelGroup.getParticles();
				if (log.isTraceEnabled()) log.trace("modelGroup particles ["+ToStringBuilder.reflectionToString(particles)+"]");
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
			if (log.isTraceEnabled()) log.trace("getAttributeUses typeDefinition is null");
			return null;
		}
		if (typeDefinition instanceof XSComplexTypeDefinition) {
			XSComplexTypeDefinition complexTypeDefinition=(XSComplexTypeDefinition)typeDefinition;
			return complexTypeDefinition.getAttributeUses();
		} 
		if (log.isTraceEnabled()) log.trace("typeDefinition ["+typeDefinition.getClass().getSimpleName()+"] SimpleType, no attributes");
		return null;
	}

	public XSTypeDefinition getTypeDefinition(PSVIProvider psviProvider) {
		ElementPSVI elementPSVI = psviProvider.getElementPSVI();
		//if (log.isTraceEnabled()) log.trace("elementPSVI ["+ToStringBuilder.reflectionToString(elementPSVI)+"]");
		XSElementDeclaration elementDeclaration = elementPSVI.getElementDeclaration();
		//if (log.isTraceEnabled()) log.trace("elementPSVI element declaration ["+ToStringBuilder.reflectionToString(elementDeclaration)+"]");
		if (elementDeclaration==null) {
			return null;
		}
		XSTypeDefinition typeDefinition = elementDeclaration.getTypeDefinition();
		//if (log.isTraceEnabled()) log.trace("elementDeclaration typeDefinition ["+ToStringBuilder.reflectionToString(typeDefinition)+"]");
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

		log.debug("setting feature ["+feature+"] to ["+value+"]");
		super.setFeature(feature, value);
	}

	public AlignmentContext getContext() {
		return context;
	}


	protected static ValidatorHandler getValidatorHandler(URL schemaURL) throws SAXException {
		SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = sf.newSchema(schemaURL); 
		return schema.newValidatorHandler();
	}

	protected static List<XSModel> getSchemaInformation(URL schemaURL) {
		XMLSchemaLoader xsLoader = new XMLSchemaLoader();
		XSModel xsModel = xsLoader.loadURI(schemaURL.toExternalForm());
		List<XSModel> schemaInformation= new LinkedList<XSModel>();
		schemaInformation.add(xsModel);
		return schemaInformation;
	}

}
