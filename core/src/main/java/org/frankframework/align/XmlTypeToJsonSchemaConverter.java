/*
   Copyright 2020-2022 WeAreFrank!

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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonStructure;
import lombok.Getter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSMultiValueFacet;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import org.apache.xerces.xs.XSWildcard;
import org.frankframework.util.LogUtil;

public class XmlTypeToJsonSchemaConverter  {
	protected Logger log = LogUtil.getLogger(this.getClass());

	private static final String JSON_SCHEMA = "http://json-schema.org/draft-04/schema#";
	private static final String XML_SCHEMA_NS = "http://www.w3.org/2001/XMLSchema";
	private static final String DEFINITIONS_PATH = "#/definitions/";
	public static final String SCHEMA_DEFINITION_PATH = "#/components/schemas/";

	private final List<XSModel> models;
	private final boolean skipArrayElementContainers;
	private final boolean skipRootElement;
	private final String schemaLocation;
	private final String definitionsPath;
	private final String attributePrefix="@";
	private final String mixedContentLabel="#text";

	private enum SimpleType {
		STRING("string"),
		INTEGER("integer"),
		NUMBER("number"),
		DATE("date"),
		DATETIME("date-time"),
		BOOLEAN("boolean");

		private final @Getter String type;

		SimpleType(String type) {
			this.type = type;
		}

	}

	public XmlTypeToJsonSchemaConverter(List<XSModel> models, boolean skipArrayElementContainers, boolean skipRootElement, String schemaLocation) {
		this(models, skipArrayElementContainers, skipRootElement, schemaLocation, DEFINITIONS_PATH);
	}

	public XmlTypeToJsonSchemaConverter(List<XSModel> models, boolean skipArrayElementContainers, boolean skipRootElement, String schemaLocation, String definitionsPath) {
		this.models=models;
		this.skipArrayElementContainers=skipArrayElementContainers;
		this.skipRootElement=skipRootElement;
		this.schemaLocation=schemaLocation;
		this.definitionsPath=definitionsPath;
	}

	public XmlTypeToJsonSchemaConverter(List<XSModel> models, boolean skipArrayElementContainers, String definitionsPath) {
		this(models, skipArrayElementContainers, false, null, definitionsPath);
	}

	public JsonStructure createJsonSchema(String elementName, String namespace) {
		XSElementDeclaration elementDecl=findElementDeclaration(elementName, namespace);
		if (elementDecl==null && namespace!=null) {
			elementDecl=findElementDeclaration(elementName, null);
		}
		if (elementDecl==null) {
			log.warn("Cannot find declaration for element [{}]", elementName);
			if (log.isTraceEnabled())
				for (XSModel model:models) {
					log.trace("model [{}]", ToStringBuilder.reflectionToString(model, ToStringStyle.MULTI_LINE_STYLE));
				}
			return null;
		}
		return createJsonSchema(elementName, elementDecl);
	}

	private XSElementDeclaration findElementDeclaration(String elementName, String namespace) {
		for (XSModel model:models) {
			if (log.isDebugEnabled()) log.debug("search for element [{}] in namespace [{}]", elementName, namespace);
			XSElementDeclaration elementDecl = model.getElementDeclaration(elementName, namespace);
			if (elementDecl!=null) {
				if (log.isTraceEnabled())
					log.trace("findTypeDefinition found elementDeclaration [{}]", ToStringBuilder.reflectionToString(elementDecl, ToStringStyle.MULTI_LINE_STYLE));
				return elementDecl;
			}
		}
		if (namespace==null) {
			for (XSModel model:models) {
				StringList namespaces = model.getNamespaces();
				for (int i=0;i<namespaces.getLength();i++) {
					namespace = namespaces.item(i);
					if (log.isDebugEnabled()) log.debug("search for element [{}] in namespace [{}]", elementName, namespace);
					XSElementDeclaration elementDecl = model.getElementDeclaration(elementName, namespace);
					if (elementDecl!=null) {
						if (log.isTraceEnabled())
							log.trace("findTypeDefinition found elementDeclaration [{}]", ToStringBuilder.reflectionToString(elementDecl, ToStringStyle.MULTI_LINE_STYLE));
						return elementDecl;
					}
				}
			}
		}

		return null;
	}

	public JsonStructure createJsonSchema(String elementName, XSElementDeclaration elementDecl) {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add("$schema", JSON_SCHEMA);
		if(elementDecl.getNamespace() != null){
			builder.add("$id", elementDecl.getNamespace());
		}
		if(schemaLocation != null){
			builder.add("description", "Auto-generated by Frank!Framework based on " + schemaLocation);
		}
		if (skipRootElement) {
			builder.add("$ref", definitionsPath+elementName);
		} else {
			addType(builder, "object").add("additionalProperties", false);
			builder.add("properties", Json.createObjectBuilder().add(elementName, Json.createObjectBuilder().add("$ref", definitionsPath+elementName).build()));
		}
		JsonObject definitionsBuilderResult = getDefinitions();
		if(!definitionsBuilderResult.isEmpty()){
			builder.add("definitions", definitionsBuilderResult);
		}
		return builder.build();
	}

	public JsonObject getDefinitions() {
		JsonObjectBuilder definitionsBuilder = Json.createObjectBuilder();
		for (XSModel model:models) {
			XSNamedMap elements = model.getComponents(XSConstants.ELEMENT_DECLARATION);
			for (int i=0; i<elements.getLength(); i++) {
				XSElementDeclaration elementDecl = (XSElementDeclaration)elements.item(i);
				handleElementDeclarationSingleOccurrence(definitionsBuilder, elementDecl);
			}
			XSNamedMap types = model.getComponents(XSConstants.TYPE_DEFINITION);
			for (int i=0; i<types.getLength(); i++) {
				XSTypeDefinition typeDefinition = (XSTypeDefinition)types.item(i);
				String typeNamespace = typeDefinition.getNamespace();
				if (typeNamespace==null || !typeDefinition.getNamespace().equals(XML_SCHEMA_NS)) {
					definitionsBuilder.add(typeDefinition.getName(), getDefinition(typeDefinition));
				}
			}
		}
		return definitionsBuilder.build();
	}

	private JsonObject getDefinition(XSTypeDefinition typeDefinition) {
		return getDefinition(typeDefinition, false);
	}

	private JsonObject getDefinitionWithReferences(XSTypeDefinition typeDefinition) {
		return getDefinition(typeDefinition, true);
	}

	private JsonObject getDefinition(XSTypeDefinition typeDefinition, boolean shouldCreateReferences) {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		switch (typeDefinition.getTypeCategory()) {
			case XSTypeDefinition.SIMPLE_TYPE:
				handleSimpleTypeDefinition(typeDefinition, builder);
				break;
			case XSTypeDefinition.COMPLEX_TYPE:
				XSComplexTypeDefinition complexTypeDefinition = (XSComplexTypeDefinition)typeDefinition;
				switch (complexTypeDefinition.getContentType()) {
					case XSComplexTypeDefinition.CONTENTTYPE_EMPTY:
						if (log.isTraceEnabled()) log.trace("getDefinition complexTypeDefinition.contentType is Empty, no child elements");
						break;
					case XSComplexTypeDefinition.CONTENTTYPE_SIMPLE:
						if (log.isTraceEnabled()) log.trace("getDefinition complexTypeDefinition.contentType is Simple, no child elements (only characters)");
						handleComplexTypeDefinitionOfSimpleContentType(complexTypeDefinition, shouldCreateReferences, builder);
						break;
					case XSComplexTypeDefinition.CONTENTTYPE_ELEMENT:
						if (log.isTraceEnabled())
							log.trace("getDefinition complexTypeDefinition.contentType is Element, complexTypeDefinition [{}]", ToStringBuilder.reflectionToString(complexTypeDefinition, ToStringStyle.MULTI_LINE_STYLE));
						handleComplexTypeDefinitionOfElementContentType(complexTypeDefinition, shouldCreateReferences, builder);
						break;
					case XSComplexTypeDefinition.CONTENTTYPE_MIXED:
						if (log.isTraceEnabled()) log.trace("getDefinition complexTypeDefinition.contentType is Mixed");
						handleComplexTypeDefinitionOfSimpleContentType(complexTypeDefinition, shouldCreateReferences, builder);
						break;
					default:
						throw new IllegalStateException("getDefinition complexTypeDefinition.contentType is not Empty,Simple,Element or Mixed, but ["+complexTypeDefinition.getContentType()+"]");
					}
					if (log.isTraceEnabled()) log.trace(ToStringBuilder.reflectionToString(complexTypeDefinition,ToStringStyle.MULTI_LINE_STYLE));
				break;
			default:
				throw new IllegalStateException("getDefinition typeDefinition.typeCategory is not Complex or Simple, but ["+typeDefinition.getTypeCategory()+"]");
			}
		return builder.build();
	}

	private void handleSimpleTypeDefinition(XSTypeDefinition typeDefinition, JsonObjectBuilder builder){
		XSSimpleTypeDefinition simpleTypeDefinition = (XSSimpleTypeDefinition)typeDefinition;
		if (log.isTraceEnabled()) log.trace("typeDefinition.name [{}]", typeDefinition.getName());
		if (log.isTraceEnabled()) log.trace("simpleTypeDefinition.getBuiltInKind [{}]", simpleTypeDefinition.getBuiltInKind());
		if (log.isTraceEnabled()) log.trace(ToStringBuilder.reflectionToString(typeDefinition,ToStringStyle.MULTI_LINE_STYLE));

		SimpleType dataType = getSimpleType(simpleTypeDefinition.getBuiltInKind());
		switch(dataType) {
			case INTEGER:
			case NUMBER:
				addType(builder, dataType.getType());

				applyFacet(simpleTypeDefinition, builder, "maximum", XSSimpleTypeDefinition.FACET_MAXINCLUSIVE);
				applyFacet(simpleTypeDefinition, builder, "minimum", XSSimpleTypeDefinition.FACET_MININCLUSIVE);
				applyFacet(simpleTypeDefinition, builder, "exclusiveMaximum", XSSimpleTypeDefinition.FACET_MAXEXCLUSIVE);
				applyFacet(simpleTypeDefinition, builder, "exclusiveMinimum", XSSimpleTypeDefinition.FACET_MINEXCLUSIVE);
				applyFacet(simpleTypeDefinition, builder, "enum", XSSimpleTypeDefinition.FACET_ENUMERATION);
				break;
			case BOOLEAN:
				addType(builder, dataType.getType());
				break;
			case STRING:
				addType(builder, dataType.getType());

				applyFacet(simpleTypeDefinition, builder, "maxLength", XSSimpleTypeDefinition.FACET_MAXLENGTH);
				applyFacet(simpleTypeDefinition, builder, "minLength", XSSimpleTypeDefinition.FACET_MINLENGTH);
				applyFacet(simpleTypeDefinition, builder, "pattern", XSSimpleTypeDefinition.FACET_PATTERN);
				applyFacet(simpleTypeDefinition, builder, "enum", XSSimpleTypeDefinition.FACET_ENUMERATION);
				break;
			case DATE:
			case DATETIME:
				addType(builder, SimpleType.STRING.getType());

				builder.add("format", dataType.getType());

				applyFacet(simpleTypeDefinition, builder, "pattern", XSSimpleTypeDefinition.FACET_PATTERN);
				applyFacet(simpleTypeDefinition, builder, "enum", XSSimpleTypeDefinition.FACET_ENUMERATION);
				break;
		}
	}

	private void handleComplexTypeDefinitionOfElementContentType(XSComplexTypeDefinition complexTypeDefinition, boolean shouldCreateReferences, JsonObjectBuilder builder){
		if(shouldCreateReferences){
			String complexTypeDefinitionName = complexTypeDefinition.getName();

			if(complexTypeDefinitionName == null && complexTypeDefinition.getContext() != null  && complexTypeDefinition.getContext().getNamespaceItem() != null){
				complexTypeDefinitionName = complexTypeDefinition.getContext().getName(); // complex type definition name defaults to name of context
			}

			if(complexTypeDefinitionName != null){
				if (log.isTraceEnabled()) log.trace("handleComplexTypeDefinitionOfElementContentType creating ref!");

				builder.add("$ref", definitionsPath+complexTypeDefinitionName);
				return;
			}
		}

		XSObjectList attributeUses = complexTypeDefinition.getAttributeUses();

		XSParticle particle = complexTypeDefinition.getParticle();
		handleParticle(builder, particle, attributeUses);
	}


	private void handleComplexTypeDefinitionOfSimpleContentType(XSComplexTypeDefinition complexTypeDefinition, boolean shouldCreateReferences, JsonObjectBuilder builder){
		if(shouldCreateReferences){
			String complexTypeDefinitionName = complexTypeDefinition.getName();

			if(complexTypeDefinitionName == null && complexTypeDefinition.getContext() != null  && complexTypeDefinition.getContext().getNamespaceItem() != null){
				complexTypeDefinitionName = complexTypeDefinition.getContext().getName(); // complex type definition name defaults to name of context
			}

			if(complexTypeDefinitionName != null){
				if (!("anyType".equals(complexTypeDefinitionName) && complexTypeDefinition.getNamespace().endsWith(XML_SCHEMA_NS))) {
					if (log.isTraceEnabled()) log.trace("handleComplexTypeDefinitionOfElementContentType creating ref!");
					builder.add("$ref", definitionsPath+complexTypeDefinitionName);
				}

				return;
			}
		}

		XSObjectList attributeUses = complexTypeDefinition.getAttributeUses();

		buildObject(builder, null, attributeUses, mixedContentLabel, complexTypeDefinition.getBaseType());
	}

	private void handleParticleForOneOf(JsonObjectBuilder builder, XSParticle particle) {
		handleParticle(builder, particle, null, false, true);
	}

	private void handleParticleForPropertiesWithoutAttributes(JsonObjectBuilder builder, XSParticle particle) {
		handleParticle(builder, particle, null, true, false);
	}

	private void handleParticle(JsonObjectBuilder builder, XSParticle particle, XSObjectList attributeUses) {
		handleParticle(builder, particle, attributeUses, false, false);
	}

	private void handleParticle(JsonObjectBuilder builder, XSParticle particle, XSObjectList attributeUses, boolean forProperties, boolean forOneOf) {
		if (particle==null) {
			throw new NullPointerException("particle is null");
		}
		XSTerm term = particle.getTerm();
		if (term==null) {
			throw new NullPointerException("particle.term is null");
		}
		if (term instanceof XSModelGroup group) {
			handleModelGroup(builder, group, attributeUses, forProperties);
			return;
		}
		if (term instanceof XSElementDeclaration elementDeclaration) {
			boolean multiOccurring = particle.getMaxOccursUnbounded() || particle.getMaxOccurs()>1;
			if (elementDeclaration.getScope()==XSConstants.SCOPE_GLOBAL) {
				String elementName = elementDeclaration.getName();
				if(forOneOf) {
					JsonArrayBuilder requiredArrayBuilder = Json.createArrayBuilder();
					requiredArrayBuilder.add(elementName);
					builder.add("required", requiredArrayBuilder);
				} else {
					JsonObject typeDefininition = Json.createObjectBuilder().add("$ref", definitionsPath+elementName).build();
					if (multiOccurring) {
						JsonObjectBuilder arrayBuilder = Json.createObjectBuilder();
						addType(arrayBuilder, "array", particle);
						arrayBuilder.add("items", typeDefininition);

						builder.add(elementName, arrayBuilder.build());
					} else {
						builder.add(elementName, typeDefininition);
					}
				}
			} else if(forOneOf){
				handleElementDeclarationForOneOf(builder, elementDeclaration);
			} else {
				handleElementDeclaration(builder, elementDeclaration, multiOccurring, true);
			}
			return;
		}
		if (term instanceof XSWildcard wildcard) {
			handleWildcard(wildcard);
			return;
		}
		throw new IllegalStateException("handleTerm unknown Term type ["+term.getClass().getName()+"]");
	}

	private void handleModelGroup(JsonObjectBuilder builder, XSModelGroup modelGroup, XSObjectList attributeUses, boolean forProperties) {
		short compositor = modelGroup.getCompositor();
		XSObjectList particles = modelGroup.getParticles();
		if (log.isTraceEnabled()) log.trace("modelGroup [{}]", ToStringBuilder.reflectionToString(modelGroup, ToStringStyle.MULTI_LINE_STYLE));
		if (log.isTraceEnabled()) log.trace("modelGroup particles [{}]", ToStringBuilder.reflectionToString(particles, ToStringStyle.MULTI_LINE_STYLE));
		switch (compositor) {
			case XSModelGroup.COMPOSITOR_SEQUENCE:
			case XSModelGroup.COMPOSITOR_ALL:
				handleCompositorsAllAndSequence(builder, particles, attributeUses);
				return;
			case XSModelGroup.COMPOSITOR_CHOICE:
				if(forProperties) {
					handleCompositorChoiceForProperties(builder, particles);
				} else {
					handleCompositorChoiceForOneOf(builder, particles);
				}
				return;
			default:
				throw new IllegalStateException("handleModelGroup modelGroup.compositor is not COMPOSITOR_SEQUENCE, COMPOSITOR_ALL or COMPOSITOR_CHOICE, but ["+compositor+"]");
		}
	}

	private void handleCompositorsAllAndSequence(JsonObjectBuilder builder, XSObjectList particles, XSObjectList attributeUses) {
		if (log.isTraceEnabled()) log.trace("modelGroup COMPOSITOR_SEQUENCE or COMPOSITOR_ALL");
		if (skipArrayElementContainers && particles.getLength()==1) {
			XSParticle childParticle = (XSParticle)particles.item(0);
			if (childParticle.getMaxOccursUnbounded() || childParticle.getMaxOccurs()>1) {
				if (log.isTraceEnabled())
					log.trace("skippable array element childParticle [{}]", ToStringBuilder.reflectionToString(particles.item(0), ToStringStyle.MULTI_LINE_STYLE));
				buildSkippableArrayContainer(childParticle, builder);
				return;
			}
		}
		buildObject(builder, particles, attributeUses, null, null);
	}

	private void handleCompositorChoiceForProperties(JsonObjectBuilder builder, XSObjectList particles) {
		if (log.isTraceEnabled()) log.trace("modelGroup COMPOSITOR_CHOICE forProperties");
		for (int i=0;i<particles.getLength();i++) {
			XSParticle childParticle = (XSParticle)particles.item(i);
			if (log.isTraceEnabled())
				log.trace("childParticle [{}][{}] for properties", i, ToStringBuilder.reflectionToString(childParticle, ToStringStyle.MULTI_LINE_STYLE));
			handleParticleForPropertiesWithoutAttributes(builder, childParticle);
		}
	}

	private void handleCompositorChoiceForOneOf(JsonObjectBuilder builder, XSObjectList particles) {
		if (log.isTraceEnabled()) log.trace("modelGroup COMPOSITOR_CHOICE for oneOf");
		JsonArrayBuilder oneOfBuilder = Json.createArrayBuilder();
		for (int i=0;i<particles.getLength();i++) {
			XSParticle childParticle = (XSParticle)particles.item(i);
			if (log.isTraceEnabled()) log.trace("childParticle [{}][{}]", i, ToStringBuilder.reflectionToString(childParticle, ToStringStyle.MULTI_LINE_STYLE));
			JsonObjectBuilder typeBuilder = Json.createObjectBuilder();
			handleParticleForOneOf(typeBuilder, childParticle);
			oneOfBuilder.add(typeBuilder.build());
		}
		builder.add("oneOf", oneOfBuilder.build());
	}

	private void handleElementDeclarationSingleOccurrence(JsonObjectBuilder builder, XSElementDeclaration elementDeclaration) {
		handleElementDeclaration(builder, elementDeclaration, false, false);
	}

	private void handleElementDeclarationForOneOf(JsonObjectBuilder builder, XSElementDeclaration elementDeclaration) {
		String elementName=elementDeclaration.getName();
		XSTypeDefinition elementTypeDefinition = elementDeclaration.getTypeDefinition();
		if (elementTypeDefinition.getAnonymous() || XML_SCHEMA_NS.equals(elementTypeDefinition.getNamespace())) {
			JsonObject definition = getDefinitionWithReferences(elementTypeDefinition);
			if(!definition.isEmpty()) {
				JsonArrayBuilder requiredArrayBuilder = Json.createArrayBuilder();
				requiredArrayBuilder.add(elementName);
				builder.add("required", requiredArrayBuilder);
			}
		} else {
			JsonArrayBuilder requiredArrayBuilder = Json.createArrayBuilder();
			requiredArrayBuilder.add(elementName);
			builder.add("required", requiredArrayBuilder);
		}
	}

	private void handleElementDeclaration(JsonObjectBuilder builder, XSElementDeclaration elementDeclaration, boolean multiOccurring, boolean shouldCreateReferences) {
		String elementName=elementDeclaration.getName();
		//if (log.isTraceEnabled()) log.trace("XSElementDeclaration name ["+elementName+"]");
		if (log.isTraceEnabled())
			log.trace("XSElementDeclaration element [{}][{}]", elementName, ToStringBuilder.reflectionToString(elementDeclaration, ToStringStyle.MULTI_LINE_STYLE));

		XSTypeDefinition elementTypeDefinition = elementDeclaration.getTypeDefinition();
		JsonObject definition = null;
		if (elementTypeDefinition.getAnonymous() || XML_SCHEMA_NS.equals(elementTypeDefinition.getNamespace())) {
			definition = getDefinition(elementTypeDefinition, shouldCreateReferences);
		} else {
			definition = Json.createObjectBuilder().add("$ref", definitionsPath+elementTypeDefinition.getName()).build();
		}
		if (elementDeclaration.getNillable()) {
			definition=nillable(definition);
		}
		if (multiOccurring) {
			JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
			addType(objectBuilder, "array");
			if(definition!=null && !definition.isEmpty()) {
				objectBuilder.add("items", definition);
			}
			builder.add(elementName, objectBuilder.build());
		} else {
			if (definition!=null) {
				builder.add(elementName, definition);
			}
		}

	}

	// Currently commented out because builder param isnt used
	// private void buildWildcard(JsonObjectBuilder builder, XSTerm term){
	private void handleWildcard(XSWildcard wildcard){
		short processContents = wildcard.getProcessContents();
		if (processContents != XSWildcard.PC_LAX && processContents != XSWildcard.PC_SKIP && processContents != XSWildcard.PC_STRICT) {
			throw new IllegalStateException("handleWildcard wildcard.processContents is not PC_LAX, PC_SKIP or PC_STRICT, but [" + wildcard.getProcessContents() + "]");
		}

		short constraintType = wildcard.getConstraintType();
		if (constraintType != XSWildcard.NSCONSTRAINT_ANY && constraintType != XSWildcard.NSCONSTRAINT_LIST && constraintType != XSWildcard.NSCONSTRAINT_NOT) {
			throw new IllegalStateException("handleWildcard wildcard.namespaceConstraint is not ANY, LIST or NOT, but [" + wildcard.getConstraintType() + "]");
		}
	}

	private void buildObject(JsonObjectBuilder builder, XSObjectList particles, XSObjectList attributeUses, String textAttribute, XSTypeDefinition baseType){
		addType(builder, "object");
		JsonObjectBuilder propertiesBuilder = Json.createObjectBuilder();
		if (attributeUses!=null) {
			for (int i=0; i< attributeUses.getLength(); i++) {
				XSAttributeUse attributeUse = (XSAttributeUse)attributeUses.get(i);
				XSAttributeDeclaration attributeDecl = attributeUse.getAttrDeclaration();
				propertiesBuilder.add(attributePrefix+attributeDecl.getName(), getDefinitionWithReferences(attributeDecl.getTypeDefinition()));
			}
		}
		if (textAttribute!=null && ((attributeUses!=null && attributeUses.getLength()>0) || (particles!=null && particles.getLength()>0))) {
			JsonObject elementType = baseType!=null ? getDefinitionWithReferences(baseType) : addType(Json.createObjectBuilder(), "string").build();
			propertiesBuilder.add(textAttribute, elementType);
		}
		mapParticles(builder, particles, propertiesBuilder);
	}


	private void mapParticles(JsonObjectBuilder builder, XSObjectList particles, JsonObjectBuilder propertiesBuilder) {
		JsonArrayBuilder requiredPropertiesBuilder = Json.createArrayBuilder();
		boolean wildcardFound = false;
		if (particles != null) {
			List<XSModelGroup> modelGroups = new ArrayList<>();
			for (int i=0;i<particles.getLength();i++) {
				XSParticle childParticle = (XSParticle)particles.item(i);
				if (log.isTraceEnabled())
					log.trace("childParticle [{}][{}]", i, ToStringBuilder.reflectionToString(childParticle, ToStringStyle.MULTI_LINE_STYLE));

				XSTerm childTerm = childParticle.getTerm();
				if(childTerm instanceof XSModelGroup group) {
					modelGroups.add(group);
				} else if(childTerm instanceof XSElementDeclaration elementDeclaration) {
					String elementName = elementDeclaration.getName();

					if(elementName != null && childParticle.getMinOccurs() != 0) {
						requiredPropertiesBuilder.add(elementName);
					}
				}
				if (XmlAligner.typeContainsWildcard(childParticle)) {
					wildcardFound=true;
				}
				handleParticleForPropertiesWithoutAttributes(propertiesBuilder, childParticle);
			}
			if(modelGroups.size() == 1) {
				handleModelGroup(builder, modelGroups.get(0), null, false);
			} else if(modelGroups.size() > 1) {
				JsonArrayBuilder allOfBuilder = Json.createArrayBuilder();
				for (XSModelGroup modelGroup : modelGroups) {
					JsonObjectBuilder oneOfBuilder = Json.createObjectBuilder();
					handleModelGroup(oneOfBuilder, modelGroup, null, false);
					allOfBuilder.add(oneOfBuilder);
				}
				builder.add("allOf", allOfBuilder.build());
			}
		}
		builder.add("additionalProperties", wildcardFound);
		JsonObject propertiesObject = propertiesBuilder.build();
		if(!propertiesObject.isEmpty()) {
			builder.add("properties", propertiesObject);
		}
		JsonArray requiredArray = requiredPropertiesBuilder.build();
		if(!requiredArray.isEmpty()){
			builder.add("required", requiredArray);
		}
	}

	private void buildSkippableArrayContainer(XSParticle childParticle, JsonObjectBuilder builder){
		JsonObjectBuilder refBuilder = Json.createObjectBuilder();
		handleParticle(refBuilder, childParticle, null);

		XSTerm childTerm = childParticle.getTerm();
		if( childTerm instanceof XSElementDeclaration elementDeclaration ){
			XSTypeDefinition elementTypeDefinition = elementDeclaration.getTypeDefinition();
			JsonStructure definition = getDefinitionWithReferences(elementTypeDefinition);

			addType(builder, "array", childParticle);
			if (elementDeclaration.getNillable()) {
				definition=nillable(definition);
			}
			builder.add("items", definition);
		}
	}

	private JsonObject nillable(JsonStructure type) {
		JsonObjectBuilder typeBuilder = Json.createObjectBuilder();
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		arrayBuilder.add(type);
		arrayBuilder.add(addType(Json.createObjectBuilder(), "null"));

		typeBuilder.add("anyOf", arrayBuilder.build());
		return typeBuilder.build();
	}

	private void applyFacet(XSSimpleTypeDefinition simpleTypeDefinition, JsonObjectBuilder builder, String key, short facet){
		if(simpleTypeDefinition.getFacet(facet) != null){
			String lexicalFacetValue = simpleTypeDefinition.getLexicalFacetValue(facet);
			if(lexicalFacetValue != null){
				switch(facet){
					case XSSimpleTypeDefinition.FACET_MAXINCLUSIVE:
					case XSSimpleTypeDefinition.FACET_MININCLUSIVE:
					case XSSimpleTypeDefinition.FACET_MAXEXCLUSIVE:
					case XSSimpleTypeDefinition.FACET_MINEXCLUSIVE:
					case XSSimpleTypeDefinition.FACET_MAXLENGTH:
					case XSSimpleTypeDefinition.FACET_MINLENGTH:
						/*
							Not sure about this..

							simpleTypeDefinition.getLexicalFacetValue(facet) returns a numeric value as string
							if value > MAX_INT, Integer.parseInt(value) will throw NumberFormatException

							currently this exception is catched and retried as Long.ParseLong(value)
							but what if this throws NumberFormatException?

							how to deal with this properly?
							-----
							UPDATE:
							Tried parsing as long and logging the value when couldn't parse, appears to be a 20 digit numeric value
							which would require to use BigInteger

							What is the best method to do this? Try and catch int, long & then bigint or directly to big int?
						*/
						try {
							builder.add(key, Integer.parseInt(lexicalFacetValue));
						} catch (NumberFormatException nfe) {
							log.warn("Couldn't parse value [{}] as Integer... retrying as Long", lexicalFacetValue);

							try {
								builder.add(key, Long.parseLong(lexicalFacetValue));
							} catch (NumberFormatException nfex) {
								log.warn("Couldn't parse value [{}] as Long... retrying as BigInteger", lexicalFacetValue);

								try {
									builder.add(key, new BigInteger(lexicalFacetValue));
								} catch (NumberFormatException nfexx) {
									log.warn("Couldn't parse value [{}] as BigInteger", lexicalFacetValue);
								}
							}
						}
						break;
					default:
						// hmm never reaches this block?
						log.debug("Setting value [{}] as String for facet [{}]", lexicalFacetValue, simpleTypeDefinition.getFacet(facet));
						builder.add(key, lexicalFacetValue);
						break;
				}
			} else if (facet == XSSimpleTypeDefinition.FACET_PATTERN || facet == XSSimpleTypeDefinition.FACET_ENUMERATION) {
				XSObjectList multiValuedFacets = simpleTypeDefinition.getMultiValueFacets();

				for (int i=0; i<multiValuedFacets.getLength(); i++) {
					XSMultiValueFacet multiValuedFacet = (XSMultiValueFacet) multiValuedFacets.item(i);

					if (log.isTraceEnabled()) {
						log.trace("Inspecting single multi valued facet [{}] which is named [{}] which is of type [{}]", multiValuedFacet, multiValuedFacet.getName(), multiValuedFacet.getType());
						log.trace("Inspecting multiValuedFacet.getLexicalFacetValues() for [{}] which has value of [{}]", multiValuedFacet.getName(), multiValuedFacet.getLexicalFacetValues());
						log.trace("Inspecting multiValuedFacet.getEnumerationValues() for [{}] which has value of [{}]", multiValuedFacet.getName(), multiValuedFacet.getEnumerationValues());
						log.trace("Inspecting multiValuedFacet.getFacetKind() == enum for [{}] which has value of [{}]", multiValuedFacet.getName(), multiValuedFacet.getFacetKind() == XSSimpleTypeDefinition.FACET_ENUMERATION);
						log.trace("Inspecting multiValuedFacet.getFacetKind() == pattern for [{}] which has value of [{}]", multiValuedFacet.getName(), multiValuedFacet.getFacetKind() == XSSimpleTypeDefinition.FACET_PATTERN);
					}

					if(facet == multiValuedFacet.getFacetKind()){
						StringList lexicalFacetValues = multiValuedFacet.getLexicalFacetValues();

						/*
							Isn't this strange?
							This assumes that an enumeration/pattern value is always a string,

							don't we need to try and parse?
						*/

						if(facet == XSSimpleTypeDefinition.FACET_ENUMERATION){
							JsonArrayBuilder enumBuilder = Json.createArrayBuilder();
							for (int x=0; x<lexicalFacetValues.getLength(); x++) {
								lexicalFacetValue = lexicalFacetValues.item(x);
								enumBuilder.add(lexicalFacetValue);
							}

							builder.add(key, enumBuilder.build());
						}
						else if(facet == XSSimpleTypeDefinition.FACET_PATTERN){
							builder.add(key, lexicalFacetValues.item(0));
						}
					}
				}
			}
		}
	}

	private JsonObjectBuilder addType(JsonObjectBuilder builder, String type) {
		return addType(builder, type, null);
	}

	private JsonObjectBuilder addType(JsonObjectBuilder builder, String type, XSParticle particle) {
		builder.add("type", type);
		if(particle != null) {
			if(particle.getMinOccurs() > 0) {
				builder.add("minItems", particle.getMinOccurs());
			}
			if(particle.getMaxOccurs() >= 0 && !particle.getMaxOccursUnbounded()) {
				builder.add("maxItems", particle.getMaxOccurs());
			}
		}
		return builder;
	}

	private SimpleType getSimpleType(short builtInKind){
		switch(builtInKind) {
			case XSConstants.BOOLEAN_DT:
				return SimpleType.BOOLEAN;
			case XSConstants.SHORT_DT:
			case XSConstants.INT_DT:
			case XSConstants.INTEGER_DT:
			case XSConstants.NEGATIVEINTEGER_DT:
			case XSConstants.NONNEGATIVEINTEGER_DT:
			case XSConstants.NONPOSITIVEINTEGER_DT:
			case XSConstants.POSITIVEINTEGER_DT:
			case XSConstants.BYTE_DT:
			case XSConstants.UNSIGNEDBYTE_DT:
			case XSConstants.UNSIGNEDINT_DT:
			case XSConstants.UNSIGNEDSHORT_DT:
				return SimpleType.INTEGER;
			case XSConstants.UNSIGNEDLONG_DT:
			case XSConstants.LONG_DT:
			case XSConstants.DECIMAL_DT:
			case XSConstants.FLOAT_DT:
			case XSConstants.DOUBLE_DT:
				return SimpleType.NUMBER;
			case XSConstants.DATE_DT:
				return SimpleType.DATE;
			case XSConstants.DATETIME_DT:
				return SimpleType.DATETIME;
			default:
				return SimpleType.STRING;
		}
	}

}
