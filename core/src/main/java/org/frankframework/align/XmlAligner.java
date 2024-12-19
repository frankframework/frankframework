/*
   Copyright 2017-2024 WeAreFrank!

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
package org.frankframework.align;

import java.net.URL;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.validation.ValidatorHandler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.xerces.impl.dv.XSSimpleType;
import org.apache.xerces.impl.xs.XMLSchemaLoader;
import org.apache.xerces.xs.ElementPSVI;
import org.apache.xerces.xs.PSVIProvider;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import org.apache.xerces.xs.XSWildcard;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.XMLFilterImpl;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.util.XmlUtils;

/**
 * XMLFilter with option to get schema information about child elements to be parsed.
 *
 * @author Gerrit van Brakel
 */
@Log4j2
public class XmlAligner extends XMLFilterImpl {
	public static final String FEATURE_NAMESPACES="http://xml.org/sax/features/namespaces";
	public static final String FEATURE_NAMESPACE_PREFIXES="http://xml.org/sax/features/namespace-prefixes";

	private @Setter PSVIProvider psviProvider;
	private @Getter @Setter boolean ignoreUndeclaredElements=false;
	protected ValidatorHandler validatorHandler;
	private @Getter @Setter List<XSModel> schemaInformation;

	private @Getter AlignmentContext context;
	private int indentLevel;
	private @Getter XSTypeDefinition typeDefinition;

	private @Getter @Setter Locator documentLocator;

	private final Deque<Set<String>> multipleOccurringElements = new ArrayDeque<>();
	private @Getter Set<String> multipleOccurringChildElements = Set.of(); // May not be null or cannot be put into ArrayDeQue
	private final Deque<Boolean> parentOfSingleMultipleOccurringChildElements = new ArrayDeque<>();
	private @Getter boolean parentOfSingleMultipleOccurringChildElement = false;
	private final Deque<Boolean> typeContainsWildcards = new ArrayDeque<>();
	private @Getter boolean typeContainsWildcard = false;

	private static final char[] INDENTOR = "\n                                                                                         ".toCharArray();
	private static final int MAX_INDENT = INDENTOR.length / 2;

	public static final String XML_SCHEMA_INSTANCE_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";
	public static final String XML_SCHEMA_NIL_ATTRIBUTE = "nil";

	private enum ChildOccurrence {
		EMPTY,ONE_SINGLE_OCCURRING_ELEMENT,ONE_MULTIPLE_OCCURRING_ELEMENT,MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING
	}

	private XmlAligner(PSVIProvider psviProvider) {
		super();
		setPsviProvider(psviProvider);
	}

	public XmlAligner(ValidatorHandler psviProvidingValidatorHandler) {
		this((PSVIProvider)psviProvidingValidatorHandler);
		psviProvidingValidatorHandler.setContentHandler(this);
		this.validatorHandler = psviProvidingValidatorHandler;
	}

	public XmlAligner(ValidatorHandler validatorHandler, List<XSModel> schemaInformation) {
		this(validatorHandler);
		this.schemaInformation=schemaInformation;
	}


	public void newLine() throws SAXException {
		int level = indentLevel;
		ignorableWhitespace(INDENTOR, 0, (Math.min(level, MAX_INDENT))*2+1);
	}

	public boolean isNil(Attributes attributes) {
		return "true".equals(attributes.getValue(XML_SCHEMA_INSTANCE_NAMESPACE, XML_SCHEMA_NIL_ATTRIBUTE));
	}

	@Override
	public void startDocument() throws SAXException {
		context = new AlignmentContext();
		super.startDocument();
	}

	@Override
	public void startElement(String namespaceUri, String localName, String qName, Attributes attributes) throws SAXException {
		if (log.isTraceEnabled()) log.trace("startElement() uri [{}] localName [{}] qName [{}]", namespaceUri, localName, qName);
		// call getChildElementDeclarations with in startElement, to obtain all child elements of the current node
		typeDefinition=getTypeDefinition(psviProvider);
		if (typeDefinition == null && !isTypeContainsWildcard()) {
			handleRecoverableError("No typeDefinition found for element ["+localName+"] in namespace ["+namespaceUri+"] qName ["+qName+"]", isIgnoreUndeclaredElements());
		} else {
			multipleOccurringElements.push(multipleOccurringChildElements);
			parentOfSingleMultipleOccurringChildElements.push(parentOfSingleMultipleOccurringChildElement);
			typeContainsWildcards.push(typeContainsWildcard);
			// call findMultipleOccurringChildElements, to obtain all child elements that could be part of an array
			if (typeDefinition instanceof XSComplexTypeDefinition complexTypeDefinition) {
				multipleOccurringChildElements=findMultipleOccurringChildElements(complexTypeDefinition.getParticle());
				parentOfSingleMultipleOccurringChildElement=ChildOccurrence.ONE_MULTIPLE_OCCURRING_ELEMENT==determineIsParentOfSingleMultipleOccurringChildElement(complexTypeDefinition.getParticle());
				typeContainsWildcard=typeContainsWildcard(complexTypeDefinition.getParticle());
				if (log.isTraceEnabled())
					log.trace("element [{}] is parentOfSingleMultipleOccurringChildElement [{}]", localName, parentOfSingleMultipleOccurringChildElement);
			} else {
				multipleOccurringChildElements = null;
				parentOfSingleMultipleOccurringChildElement = false;
				typeContainsWildcard = !typeContainsWildcards.isEmpty() && typeContainsWildcards.peek();
				if (log.isTraceEnabled()) {
					if (typeDefinition == null) {
						log.trace("element [{}] is a SimpleType, and therefor not multiple", localName);
					} else {
						log.trace("no type definition found for element [{}], assuming not multiple", localName);
					}
				}
			}
			super.startElement(namespaceUri, localName, qName, attributes);
		}
		indentLevel++;
		context = new AlignmentContext(context, localName, typeDefinition);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (log.isTraceEnabled()) log.trace("endElement() uri [{}] localName [{}] qName [{}]", uri, localName, qName);
		boolean knownElement = context.getTypeDefinition()!=null;
		context = context.getParent();
		indentLevel--;
		if (knownElement|| isTypeContainsWildcard()) {
			typeDefinition = null;
			super.endElement(uri, localName, qName);
			multipleOccurringChildElements=multipleOccurringElements.pop();
			parentOfSingleMultipleOccurringChildElement=parentOfSingleMultipleOccurringChildElements.pop();
			typeContainsWildcards.pop();
		}
	}


	public boolean isPresentInSet(Set<String> set, String name) {
		return set!=null && set.contains(name);
	}

	public boolean isMultipleOccurringChildInParentElement(String name) {
		return isPresentInSet(multipleOccurringElements.peek(),name);
	}

	public boolean isMultipleOccurringChildElement(String name) {
		return isPresentInSet(multipleOccurringChildElements, name);
	}

	private ChildOccurrence determineIsParentOfSingleMultipleOccurringChildElement(XSParticle particle) {
		if (particle == null) {
			log.warn("Particle is null, is this a problem? Appearantly not");
			return ChildOccurrence.EMPTY;
		}
		XSTerm term = particle.getTerm();
		if (term == null) {
			throw new IllegalStateException("determineIsParentOfSingleMultipleOccurringChildElement particle.term is null");
		}
		if (log.isTraceEnabled())
			log.trace("term name [{}] occurring unbounded [{}] max occur [{}] term [{}]", term.getName(), particle.getMaxOccursUnbounded(), particle.getMaxOccurs(), ToStringBuilder.reflectionToString(term));
		if (term instanceof XSModelGroup modelGroup) {
			short compositor = modelGroup.getCompositor();
			XSObjectList particles = modelGroup.getParticles();
			switch (compositor) {
			case XSModelGroup.COMPOSITOR_SEQUENCE:
			case XSModelGroup.COMPOSITOR_ALL: {
				if (log.isTraceEnabled()) log.trace("sequence or all particles [{}]", ToStringBuilder.reflectionToString(particles));
				ChildOccurrence result=ChildOccurrence.EMPTY;
				for (int i = 0; i < particles.getLength(); i++) {
					XSParticle childParticle = (XSParticle)particles.item(i);
					ChildOccurrence current=determineIsParentOfSingleMultipleOccurringChildElement(childParticle);
					if (log.isTraceEnabled()) log.trace("sequence or all, particle [{}] current result [{}]", i, current);
					switch (current) {
					case EMPTY:
						break;
					case ONE_SINGLE_OCCURRING_ELEMENT:
					case ONE_MULTIPLE_OCCURRING_ELEMENT:
						if (result.ordinal()>ChildOccurrence.EMPTY.ordinal()) {
							if (log.isTraceEnabled()) log.trace("sequence or all, result [{}] current [{}]", result, current);
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
				if (log.isTraceEnabled()) log.trace("end of sequence or all, returning [{}]", result);
				return result;
			}
			case XSModelGroup.COMPOSITOR_CHOICE: {
				if (log.isTraceEnabled()) log.trace("choice particles [{}]", ToStringBuilder.reflectionToString(particles));
				if (particles.getLength() == 0) {
					if (log.isTraceEnabled()) log.trace("choice length 0, returning [{}]", ChildOccurrence.MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING);
					return ChildOccurrence.EMPTY;
				}
				ChildOccurrence result = determineIsParentOfSingleMultipleOccurringChildElement((XSParticle) particles.item(0));
				if (result == ChildOccurrence.MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING) {
					if (log.isTraceEnabled()) log.trace("choice single mixed, returning [{}]", ChildOccurrence.MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING);
					return ChildOccurrence.MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING;
				}
				for (int i = 1; i < particles.getLength(); i++) {
					XSParticle childParticle = (XSParticle) particles.item(i);
					ChildOccurrence current = determineIsParentOfSingleMultipleOccurringChildElement(childParticle);
					if (current != result) {
						if (log.isTraceEnabled()) log.trace("break out of choice, returning [{}]", ChildOccurrence.MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING);
						return ChildOccurrence.MULTIPLE_ELEMENTS_OR_NOT_MULTIPLE_OCCURRING;
					}
				}
				if (log.isTraceEnabled()) log.trace("end of choice, returning [{}]", result);
				return result;
			}
			default:
				throw new IllegalStateException("determineIsParentOfSingleMultipleOccurringChildElement() modelGroup.compositor is not COMPOSITOR_SEQUENCE, COMPOSITOR_ALL or COMPOSITOR_CHOICE, but ["+compositor+"]");
			}
		}
		if (term instanceof XSElementDeclaration elementDeclaration) {
			String elementName=elementDeclaration.getName();
			if (log.isTraceEnabled())
				log.trace("ElementDeclaration name [{}] unbounded [{}] maxOccurs [{}]", elementName, particle.getMaxOccursUnbounded(), particle.getMaxOccurs());
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

	public static boolean typeContainsWildcard(XSParticle particle) {
		if (particle==null) {
			return false;
		}
		XSTerm term = particle.getTerm();
		if (term==null) {
			throw new IllegalStateException("checkIfTypeIsWildcard particle.term is null");

		}
		if (term instanceof XSWildcard) {
			return true;
		}
		if (term instanceof XSElementDeclaration) {
			return false;
		}
		if (term instanceof XSModelGroup modelGroup) {
			XSObjectList particles = modelGroup.getParticles();
			for (int i=0;i<particles.getLength();i++) {
				if (typeContainsWildcard((XSParticle)particles.item(i))) {
					return true;
				}
			}
			return false;
		}
		throw new IllegalStateException("typeIsWildcard unknown Term type ["+term.getClass().getName()+"]");
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
		if (term instanceof XSModelGroup modelGroup) {
			XSObjectList particles = modelGroup.getParticles();
			for (int i=0;i<particles.getLength();i++) {
				XSParticle childParticle = (XSParticle)particles.item(i);
				collectChildElements(childParticle, elementNames);
			}
			return;
		}
		if (term instanceof XSElementDeclaration elementDeclaration) {
			String elementName=elementDeclaration.getName();
			if (log.isTraceEnabled()) log.trace("ElementDeclaration name [{}]", elementName);
			elementNames.add(elementName);
		}
	}

	protected @Nonnull Set<String> findMultipleOccurringChildElements(XSParticle particle) {
		Set<String> result=new HashSet<>();
		if (particle == null) {
			log.warn("typeDefinition particle is null, is this a problem?");
			return result;
		}
		XSTerm term = particle.getTerm();
		if (term == null) {
			throw new IllegalStateException("findMultipleOccurringChildElements particle.term is null");
		}
		log.trace("term name [{}] occurring unbounded [{}] max occur [{}] term [{}]", term::getName, particle::getMaxOccursUnbounded, particle::getMaxOccurs, ()->ToStringBuilder.reflectionToString(term));
		if (particle.getMaxOccursUnbounded() || particle.getMaxOccurs() > 1) {
			collectChildElements(particle,result);
			return result;
		}
		if (term instanceof XSModelGroup modelGroup) {
			@SuppressWarnings("unchecked") List<XSParticle> particles = modelGroup.getParticles();
			log.trace("modelGroup particles [{}]", ()->ToStringBuilder.reflectionToString(particles));
			for (XSParticle childParticle : particles) {
				result.addAll(findMultipleOccurringChildElements(childParticle));
			}
		}
		return result;
	}

	public @Nonnull List<XSAttributeUse> getAttributeUses() {
		return getAttributeUses(typeDefinition);
	}

	public @Nonnull List<XSAttributeUse> getAttributeUses(XSTypeDefinition typeDefinition) {
		if (typeDefinition==null) {
			log.trace("getAttributeUses typeDefinition is null");
			return List.of();
		}
		if (typeDefinition instanceof XSComplexTypeDefinition complexTypeDefinition) {
			//noinspection unchecked
			return complexTypeDefinition.getAttributeUses();
		}
		if (log.isTraceEnabled()) log.trace("typeDefinition [{}] SimpleType, no attributes", typeDefinition.getClass().getSimpleName());
		return List.of();
	}

	public @Nullable XSTypeDefinition getTypeDefinition(PSVIProvider psviProvider) {
		ElementPSVI elementPSVI = psviProvider.getElementPSVI();
		XSElementDeclaration elementDeclaration = elementPSVI.getElementDeclaration();
		if (elementDeclaration == null) {
			return null;
		}
		return elementDeclaration.getTypeDefinition();
	}


	public @Nullable XSSimpleType getElementType() {
		if (typeDefinition instanceof XSSimpleType type) {
			return type;
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

		log.debug("setting feature [{}] to [{}]", feature, value);
		super.setFeature(feature, value);
	}


	protected static ValidatorHandler getValidatorHandler(URL schemaURL) throws SAXException {
		return XmlUtils.getValidatorHandler(schemaURL);
	}

	protected static List<XSModel> getSchemaInformation(URL schemaURL) {
		XMLSchemaLoader xsLoader = new XMLSchemaLoader();
		XSModel xsModel = xsLoader.loadURI(schemaURL.toExternalForm());
		return Collections.singletonList(xsModel);
	}

	public void handleRecoverableError(String message, boolean ignoreFlag) throws SAXParseException {
		ErrorHandler errorHandler = getErrorHandler();
		if (errorHandler!=null) {
			try {
				SAXParseException saxException = new SAXParseException(message, getDocumentLocator());
				if (ignoreFlag) {
					errorHandler.warning(saxException);
				} else {
					errorHandler.error(saxException);
				}
				return;
			} catch (SAXException e) {
				log.warn("exception handling error", e);
			}
		}
		if (!ignoreFlag) {
			throw new SAXParseException(message, getDocumentLocator());
		}
	}

	protected @Nullable XSElementDeclaration findElementDeclarationForName(String namespace, String name) throws SAXException {
		Set<XSElementDeclaration> elementDeclarations=findElementDeclarationsForName(namespace, name);
		if (elementDeclarations.isEmpty()) {
			log.warn("No element declarations found for [{}]:[{}]", namespace, name);
			return null;
		}
		if (elementDeclarations.size() > 1) {
			XSElementDeclaration[] elementDeclarationsArray = elementDeclarations.toArray(new XSElementDeclaration[0]);
			throw new SAXException("multiple ["+elementDeclarations.size()+"] elementDeclarations found for ["+namespace+"]:["+name+"]: first two ["+elementDeclarationsArray[0].getNamespace()+":"+elementDeclarationsArray[0].getName()+"]["+elementDeclarationsArray[1].getNamespace()+":"+elementDeclarationsArray[1].getName()+"]");
		}
		return (XSElementDeclaration) elementDeclarations.toArray()[0];
	}

	protected @Nonnull Set<XSElementDeclaration> findElementDeclarationsForName(String namespace, String name) {
		Set<XSElementDeclaration> result=new LinkedHashSet<>();
		if (schemaInformation==null) {
			throw new IllegalStateException("No SchemaInformation specified, cannot find namespaces for ["+namespace+"]:["+name+"]");
		}
		for (XSModel model: schemaInformation) {
			XSNamedMap components = model.getComponents(XSConstants.ELEMENT_DECLARATION);
			for (int i=0;i<components.getLength();i++) {
				XSElementDeclaration item=(XSElementDeclaration)components.item(i);
				if ((namespace==null || namespace.equals(item.getNamespace())) && (name==null || name.equals(item.getName()))) {
					if (log.isTraceEnabled()) log.trace("name [{}] found in namespace [{}]", item.getName(), item.getNamespace());
					result.add(item);
				}
			}
		}
		return result;
	}

}
