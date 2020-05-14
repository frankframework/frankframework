/*
   Copyright 2020 WeAreFrank!

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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

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

import nl.nn.adapterframework.util.LogUtil;

public class XmlTypeToJsonSchemaConverter  {
	protected Logger log = LogUtil.getLogger(this.getClass());

	private final String JSON_SCHEMA = "http://json-schema.org/draft-04/schema#";
	private final String XML_SCHEMA_NS = "http://www.w3.org/2001/XMLSchema";

	private List<XSModel> models;
	private boolean skipArrayElementContainers;
	private boolean skipRootElement;
	private String schemaLocation;
	private String definitionsPath;

	protected final boolean DEBUG=false; 

	public XmlTypeToJsonSchemaConverter(List<XSModel> models, boolean skipArrayElementContainers, boolean skipRootElement, String schemaLocation) {
		this(models, skipArrayElementContainers, skipRootElement, schemaLocation, "#/definitions/");
	}
	public XmlTypeToJsonSchemaConverter(List<XSModel> models, boolean skipArrayElementContainers, boolean skipRootElement, String schemaLocation, String definitionsPath) {
		this.models=models;
		this.skipArrayElementContainers=skipArrayElementContainers;
		this.skipRootElement=skipRootElement;
		this.schemaLocation=schemaLocation;
		this.definitionsPath=definitionsPath;
	}

	public JsonStructure createJsonSchema(String elementName, String namespace) {
		XSElementDeclaration elementDecl=findElementDeclaration(elementName, namespace);
		if (elementDecl==null && namespace!=null) {
			elementDecl=findElementDeclaration(elementName, null);
		}
		if (elementDecl==null) {
			log.warn("Cannot find declaration for element ["+elementName+"]");
			if (DEBUG) 
				for (XSModel model:models) {
					log.debug("model ["+ToStringBuilder.reflectionToString(model,ToStringStyle.MULTI_LINE_STYLE)+"]");
				}
			return null;
		}
		return createJsonSchema(elementName, elementDecl);
	}

	public XSElementDeclaration findElementDeclaration(String elementName, String namespace) {
		for (XSModel model:models) {
			if (log.isDebugEnabled()) log.debug("search for element ["+elementName+"] in namespace ["+namespace+"]");
			XSElementDeclaration elementDecl = model.getElementDeclaration(elementName, namespace);
			if (elementDecl!=null) {
				if (DEBUG) log.debug("findTypeDefinition found elementDeclaration ["+ToStringBuilder.reflectionToString(elementDecl,ToStringStyle.MULTI_LINE_STYLE)+"]");
				return elementDecl;
			}
		}
		if (namespace==null) {
			for (XSModel model:models) {
				StringList namespaces = model.getNamespaces();
				for (int i=0;i<namespaces.getLength();i++) {
					namespace = (String)namespaces.item(i);
					if (log.isDebugEnabled()) log.debug("search for element ["+elementName+"] in namespace ["+namespace+"]");
					XSElementDeclaration elementDecl = model.getElementDeclaration(elementName, namespace);
					if (elementDecl!=null) {
						if (DEBUG) log.debug("findTypeDefinition found elementDeclaration ["+ToStringBuilder.reflectionToString(elementDecl,ToStringStyle.MULTI_LINE_STYLE)+"]");
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
			builder.add("type", "object").add("additionalProperties", false);
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
				handleElementDeclaration(definitionsBuilder, elementDecl, false, false);
			}
			XSNamedMap types = model.getComponents(XSConstants.TYPE_DEFINITION);
			for (int i=0; i<types.getLength(); i++) {
				XSTypeDefinition typeDefinition = (XSTypeDefinition)types.item(i);
				String typeNamespace = typeDefinition.getNamespace();
				if (typeNamespace==null || !typeDefinition.getNamespace().equals(XML_SCHEMA_NS)) {
					definitionsBuilder.add(typeDefinition.getName(), getDefinition(typeDefinition, false));
				}
			}
		}
		return definitionsBuilder.build();
	}
	
	public JsonObject getDefinition(XSTypeDefinition typeDefinition, boolean shouldCreateReferences) {
		JsonObjectBuilder builder = Json.createObjectBuilder();

		switch (typeDefinition.getTypeCategory()) {
		case XSTypeDefinition.COMPLEX_TYPE:
			XSComplexTypeDefinition complexTypeDefinition = (XSComplexTypeDefinition)typeDefinition;
			switch (complexTypeDefinition.getContentType()) {
			case XSComplexTypeDefinition.CONTENTTYPE_EMPTY:
				if (DEBUG) log.debug("getDefinition complexTypeDefinition.contentType is Empty, no child elements");
				break;
			case XSComplexTypeDefinition.CONTENTTYPE_SIMPLE:
				if (DEBUG) log.debug("getDefinition complexTypeDefinition.contentType is Simple, no child elements (only characters)");
				break;
			case XSComplexTypeDefinition.CONTENTTYPE_ELEMENT:
				if (DEBUG) log.debug("getDefinition complexTypeDefinition.contentType is Element, complexTypeDefinition ["+ToStringBuilder.reflectionToString(complexTypeDefinition,ToStringStyle.MULTI_LINE_STYLE)+"]");
				handleComplexTypeDefinitionOfElementContentType(complexTypeDefinition, shouldCreateReferences, builder);
				break;
			case XSComplexTypeDefinition.CONTENTTYPE_MIXED:
				if (DEBUG) log.debug("getDefinition complexTypeDefinition.contentType is Mixed");
				break;
			default:
				throw new IllegalStateException("getDefinition complexTypeDefinition.contentType is not Empty,Simple,Element or Mixed, but ["+complexTypeDefinition.getContentType()+"]");
			}
			if (DEBUG) log.debug(ToStringBuilder.reflectionToString(complexTypeDefinition,ToStringStyle.MULTI_LINE_STYLE));
			break;
		case XSTypeDefinition.SIMPLE_TYPE:
			handleSimpleTypeDefinition(typeDefinition, builder);
			break;
		default:
			throw new IllegalStateException("getDefinition typeDefinition.typeCategory is not Complex or Simple, but ["+typeDefinition.getTypeCategory()+"]");
		}
		return builder.build();
	}

	private void handleSimpleTypeDefinition(XSTypeDefinition typeDefinition, JsonObjectBuilder builder){
		XSSimpleTypeDefinition simpleTypeDefinition = (XSSimpleTypeDefinition)typeDefinition;
		if (DEBUG) log.debug("typeDefinition.name ["+typeDefinition.getName()+"]");
		if (DEBUG) log.debug("simpleTypeDefinition.getBuiltInKind ["+simpleTypeDefinition.getBuiltInKind()+"]");
		if (DEBUG) log.debug(ToStringBuilder.reflectionToString(typeDefinition,ToStringStyle.MULTI_LINE_STYLE));

		short builtInKind = simpleTypeDefinition.getBuiltInKind();
		String dataType = getJsonDataType(builtInKind);
		
		if (dataType.equalsIgnoreCase("integer") || dataType.equalsIgnoreCase("number")) {
			builder.add("type", dataType.toLowerCase());

			applyFacet(simpleTypeDefinition, builder, "maximum", XSSimpleTypeDefinition.FACET_MAXINCLUSIVE);
			applyFacet(simpleTypeDefinition, builder, "minimum", XSSimpleTypeDefinition.FACET_MININCLUSIVE);
			applyFacet(simpleTypeDefinition, builder, "exclusiveMaximum", XSSimpleTypeDefinition.FACET_MAXEXCLUSIVE);
			applyFacet(simpleTypeDefinition, builder, "exclusiveMinimum", XSSimpleTypeDefinition.FACET_MINEXCLUSIVE);
			applyFacet(simpleTypeDefinition, builder, "enum", XSSimpleTypeDefinition.FACET_ENUMERATION);
		} else if (dataType.equalsIgnoreCase("boolean")) {
			builder.add("type", "boolean");
		} else if (dataType.equalsIgnoreCase("string")) {	
			builder.add("type", "string");
		
			applyFacet(simpleTypeDefinition, builder, "maxLength", XSSimpleTypeDefinition.FACET_MAXLENGTH);
			applyFacet(simpleTypeDefinition, builder, "minLength", XSSimpleTypeDefinition.FACET_MINLENGTH);
			applyFacet(simpleTypeDefinition, builder, "pattern", XSSimpleTypeDefinition.FACET_PATTERN);
			applyFacet(simpleTypeDefinition, builder, "enum", XSSimpleTypeDefinition.FACET_ENUMERATION);
		} else if (dataType.equalsIgnoreCase("date") || dataType.equalsIgnoreCase("date-time") || dataType.equalsIgnoreCase("time")) {		
			builder.add("type", "string");
			
			builder.add("format", dataType);

			applyFacet(simpleTypeDefinition, builder, "pattern", XSSimpleTypeDefinition.FACET_PATTERN);
			applyFacet(simpleTypeDefinition, builder, "enum", XSSimpleTypeDefinition.FACET_ENUMERATION);
		}
	}

	private void handleComplexTypeDefinitionOfElementContentType(XSComplexTypeDefinition complexTypeDefinition, boolean shouldCreateReferences, JsonObjectBuilder builder){
		if(shouldCreateReferences){
			String complexTypeDefinitionName = complexTypeDefinition.getName();

			if(complexTypeDefinitionName == null && complexTypeDefinition.getContext() != null  && complexTypeDefinition.getContext().getNamespaceItem() != null){
				complexTypeDefinitionName = complexTypeDefinition.getContext().getName(); // complex type definition name defaults to name of context
			}

			if(complexTypeDefinitionName != null){
				if (DEBUG) log.debug("handleComplexTypeDefinitionOfElementContentType creating ref!");

				builder.add("$ref", definitionsPath+complexTypeDefinitionName);
//				if(!namedJsonObjects.contains(complexTypeDefinitionName)){
//					buildReference(complexTypeDefinition, complexTypeDefinitionName);
//				}
				return;
			}
		}
		
		XSObjectList attributeUses = complexTypeDefinition.getAttributeUses();

		// Currently commented out because block has no effect

		// if (attributeUses.getLength()>0) {
		// 	for (int i=0; i<attributeUses.getLength(); i++) {
		// 		XSAttributeUse attributeUse = (XSAttributeUse)attributeUses.get(i);
		// 		if (DEBUG) log.debug("handleElementContents complexTypeDefinition.contentType is Element, attribute ["+ToStringBuilder.reflectionToString(attributeUse.getAttrDeclaration(),ToStringStyle.MULTI_LINE_STYLE)+"]");

		// 		XSAttributeDeclaration attrDeclaration = attributeUse.getAttrDeclaration();
		// 		if (DEBUG) log.debug("handleElementContents attrDeclaration.getValueConstraintValue ["+ToStringBuilder.reflectionToString(attrDeclaration.getValueConstraintValue(),ToStringStyle.MULTI_LINE_STYLE)+"]");
		// 		if (DEBUG) log.debug("handleElementContents attrDeclaration.getTypeDefinition ["+ToStringBuilder.reflectionToString(attrDeclaration.getTypeDefinition(),ToStringStyle.MULTI_LINE_STYLE)+"]");
		// 		if (DEBUG) log.debug("handleElementContents attrDeclaration.getEnclosingCTDefinition ["+ToStringBuilder.reflectionToString(attrDeclaration.getValueConstraintValue(),ToStringStyle.MULTI_LINE_STYLE)+"]");
		// 	}
		// }

		XSParticle particle = complexTypeDefinition.getParticle();
		handleParticle(builder, particle, attributeUses);
	}
	
	public void handleParticle(JsonObjectBuilder builder, XSParticle particle, XSObjectList attributeUses) {
		if (particle==null) {
			throw new NullPointerException("particle is null");
		} 
		XSTerm term = particle.getTerm();
		if (term==null) {
			throw new NullPointerException("particle.term is null");
		}
		handleTerm(builder,term,attributeUses, particle.getMaxOccursUnbounded() || particle.getMaxOccurs()>1);
	}

	public void handleTerm(JsonObjectBuilder builder, XSTerm term, XSObjectList attributeUses, boolean multiOccurring) {
		if (term instanceof XSModelGroup) {
			handleModelGroup(builder, (XSModelGroup)term, attributeUses);
			return;
		} 
		if (term instanceof XSElementDeclaration) {
			XSElementDeclaration elementDeclaration = (XSElementDeclaration)term;
			if (elementDeclaration.getScope()==XSConstants.SCOPE_GLOBAL) {
				JsonObject typeDefininition = Json.createObjectBuilder().add("$ref", "#/definitions/"+elementDeclaration.getName()).build();
				if (multiOccurring) {
					JsonObjectBuilder arrayBuilder = Json.createObjectBuilder();
					arrayBuilder.add("type", "array");
					arrayBuilder.add("items", typeDefininition);

					builder.add(elementDeclaration.getName(), arrayBuilder.build());
				} else {
					builder.add(elementDeclaration.getName(), typeDefininition);
				}
			} else {
				handleElementDeclaration(builder, elementDeclaration, multiOccurring, true);
			}
			return;
		}
		if (term instanceof XSWildcard) {
			handleWildcard((XSWildcard)term);
			return;
		} 
		throw new IllegalStateException("handleTerm unknown Term type ["+term.getClass().getName()+"]");
	}

	private void handleModelGroup(JsonObjectBuilder builder, XSModelGroup modelGroup, XSObjectList attributeUses){
		short compositor = modelGroup.getCompositor();			
		XSObjectList particles = modelGroup.getParticles();
		if (DEBUG) log.debug("modelGroup ["+ToStringBuilder.reflectionToString(modelGroup,ToStringStyle.MULTI_LINE_STYLE)+"]");
		if (DEBUG) log.debug("modelGroup particles ["+ToStringBuilder.reflectionToString(particles,ToStringStyle.MULTI_LINE_STYLE)+"]");
		switch (compositor) {
		case XSModelGroup.COMPOSITOR_SEQUENCE:
		case XSModelGroup.COMPOSITOR_ALL:
			handleCompositorsAllAndSequence(builder, particles, attributeUses);
			return;
		case XSModelGroup.COMPOSITOR_CHOICE:
			handleCompositorChoice(builder, particles);	
			return;
		default:
			throw new IllegalStateException("handleModelGroup modelGroup.compositor is not COMPOSITOR_SEQUENCE, COMPOSITOR_ALL or COMPOSITOR_CHOICE, but ["+compositor+"]");
		} 
	}

	private void handleCompositorsAllAndSequence(JsonObjectBuilder builder, XSObjectList particles, XSObjectList attributeUses){
		if (DEBUG) log.debug("modelGroup COMPOSITOR_SEQUENCE or COMPOSITOR_ALL");
		if (skipArrayElementContainers && particles.getLength()==1) {
			XSParticle childParticle = (XSParticle)particles.item(0);
			if (childParticle.getMaxOccursUnbounded() || childParticle.getMaxOccurs()>1) {
				if (DEBUG) log.debug("skippable array element childParticle ["+ToStringBuilder.reflectionToString(particles.item(0),ToStringStyle.MULTI_LINE_STYLE)+"]");
				buildSkippableArrayContainer(childParticle, builder);
				return;
			}
		}
		buildProperties(builder, particles, attributeUses);
	}

	private void handleCompositorChoice(JsonObjectBuilder builder, XSObjectList particles){
		if (DEBUG) log.debug("modelGroup COMPOSITOR_CHOICE");
		JsonArrayBuilder oneOfBuilder = Json.createArrayBuilder();
		for (int i=0;i<particles.getLength();i++) {
			XSParticle childParticle = (XSParticle)particles.item(i);
			if (DEBUG) log.debug("childParticle ["+i+"]["+ToStringBuilder.reflectionToString(childParticle,ToStringStyle.MULTI_LINE_STYLE)+"]");
			JsonObjectBuilder typeBuilder = Json.createObjectBuilder();
			handleParticle(typeBuilder,childParticle,null);
			oneOfBuilder.add(typeBuilder.build());
		}
		builder.add("oneOf", oneOfBuilder.build());
	}
	
	private void handleElementDeclaration(JsonObjectBuilder builder, XSElementDeclaration elementDeclaration, boolean multiOccurring, boolean shouldCreateReferences){	
		String elementName=elementDeclaration.getName();
		//if (DEBUG) log.debug("XSElementDeclaration name ["+elementName+"]");
		if (DEBUG) log.debug("XSElementDeclaration element ["+elementName+"]["+ToStringBuilder.reflectionToString(elementDeclaration,ToStringStyle.MULTI_LINE_STYLE)+"]");

		XSTypeDefinition elementTypeDefinition = elementDeclaration.getTypeDefinition();
		JsonStructure definition;
		if (elementTypeDefinition.getAnonymous() || XML_SCHEMA_NS.equals(elementTypeDefinition.getNamespace())) {
			definition =getDefinition(elementTypeDefinition, shouldCreateReferences);
		} else {
			definition = Json.createObjectBuilder().add("$ref",definitionsPath+elementTypeDefinition.getName()).build();
		}
		if (elementDeclaration.getNillable()) {
			definition=nillable(definition);
		}
		if (multiOccurring) {
			JsonObjectBuilder arrayBuilder = Json.createObjectBuilder();
			arrayBuilder.add("type", "array");
			arrayBuilder.add("items", definition);

			builder.add(elementName, arrayBuilder.build());
		} else {
			if (definition!=null) {
				builder.add(elementName, definition);
			}
		}
		
	}

	// Currently commented out because builder param isnt used
	// private void buildWildcard(JsonObjectBuilder builder, XSTerm term){
	private void handleWildcard(XSWildcard wildcard){
		String processContents;
		switch (wildcard.getProcessContents()) {
		case XSWildcard.PC_LAX: processContents="LAX"; break;
		case XSWildcard.PC_SKIP: processContents="SKIP"; break;
		case XSWildcard.PC_STRICT: processContents="STRICT"; break;
		default: 
				throw new IllegalStateException("handleWildcard wildcard.processContents is not PC_LAX, PC_SKIP or PC_STRICT, but ["+wildcard.getProcessContents()+"]");
		}
		String namespaceConstraint;
		switch (wildcard.getConstraintType()) {
		case XSWildcard.NSCONSTRAINT_ANY : namespaceConstraint="ANY"; break;
		case XSWildcard.NSCONSTRAINT_LIST : namespaceConstraint="SKIP "+wildcard.getNsConstraintList(); break;
		case XSWildcard.NSCONSTRAINT_NOT : namespaceConstraint="NOT "+wildcard.getNsConstraintList(); break;
		default: 
				throw new IllegalStateException("handleWildcard wildcard.namespaceConstraint is not ANY, LIST or NOT, but ["+wildcard.getConstraintType()+"]");
		}
	}

	private void buildProperties(JsonObjectBuilder builder, XSObjectList particles, XSObjectList attributeUses){
		builder.add("type", "object");
		builder.add("additionalProperties", false);
		JsonObjectBuilder propertiesBuilder = Json.createObjectBuilder();
		List<String> requiredProperties = new ArrayList<String>();

		if (attributeUses!=null) {
			for (int i=0; i< attributeUses.getLength(); i++) {
				XSAttributeUse attributeUse = (XSAttributeUse)attributeUses.get(i);
				XSAttributeDeclaration attributeDecl = attributeUse.getAttrDeclaration();
				propertiesBuilder.add("@"+attributeDecl.getName(), getDefinition(attributeDecl.getTypeDefinition(), true));
			}
		}
		for (int i=0;i<particles.getLength();i++) {
			XSParticle childParticle = (XSParticle)particles.item(i);
			if (DEBUG) log.debug("childParticle ["+i+"]["+ToStringBuilder.reflectionToString(childParticle,ToStringStyle.MULTI_LINE_STYLE)+"]");
		
			XSTerm childTerm = childParticle.getTerm();
			if (childTerm instanceof XSElementDeclaration) {
				XSElementDeclaration elementDeclaration = (XSElementDeclaration) childTerm;
				String elementName = elementDeclaration.getName();

				if(elementName != null && childParticle.getMinOccurs() != 0){
					requiredProperties.add(elementName);
				}
			}
			
			handleParticle(propertiesBuilder, childParticle, null);
		}
		builder.add("properties", propertiesBuilder.build());
		if(requiredProperties.size() > 0){
			JsonArrayBuilder requiredPropertiesBuilder = Json.createArrayBuilder();
			for (String requiredProperty : requiredProperties) {
				requiredPropertiesBuilder.add(requiredProperty);
			}
			builder.add("required", requiredPropertiesBuilder.build());
		}
	}

	private void buildSkippableArrayContainer(XSParticle childParticle, JsonObjectBuilder builder){
		JsonObjectBuilder refBuilder = Json.createObjectBuilder();
		handleParticle(refBuilder,childParticle,null);

		XSTerm childTerm = childParticle.getTerm();
		if( childTerm instanceof XSElementDeclaration ){
			XSElementDeclaration elementDeclaration=(XSElementDeclaration) childTerm;
			XSTypeDefinition elementTypeDefinition = elementDeclaration.getTypeDefinition();
			JsonStructure definition =getDefinition(elementTypeDefinition, true);
		
			builder.add("type", "array");
			if (elementDeclaration.getNillable()) {
				definition=nillable(definition);
			}
			builder.add("items", definition);
		}
	}

	public JsonStructure nillable(JsonStructure type) {
		JsonObjectBuilder typeBuilder = Json.createObjectBuilder();
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		arrayBuilder.add(type);
		arrayBuilder.add(Json.createObjectBuilder().add("type", "null"));
		
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
							log.warn("Couldn't parse value ["+lexicalFacetValue+"] as Integer... retrying as Long");
	
							try {
								builder.add(key, Long.parseLong(lexicalFacetValue));
							} catch (NumberFormatException nfex) {
								log.warn("Couldn't parse value ["+lexicalFacetValue+"] as Long... retrying as BigInteger");
								
								try {
									builder.add(key, new BigInteger(lexicalFacetValue));
								} catch (NumberFormatException nfexx) {
									log.warn("Couldn't parse value ["+lexicalFacetValue+"] as BigInteger");
								}
							}
						}	
						break;
					default:
						// hmm never reaches this block?
						log.debug("Setting value ["+lexicalFacetValue+"] as String for facet ["+simpleTypeDefinition.getFacet(facet)+"]");
						builder.add(key, lexicalFacetValue);
						break;
				}
			} else if (facet == XSSimpleTypeDefinition.FACET_PATTERN || facet == XSSimpleTypeDefinition.FACET_ENUMERATION) {
				XSObjectList multiValuedFacets = simpleTypeDefinition.getMultiValueFacets();

				for (int i=0; i<multiValuedFacets.getLength(); i++) {
					XSMultiValueFacet multiValuedFacet = (XSMultiValueFacet) multiValuedFacets.item(i);

					if (DEBUG) log.debug("Inspecting single multi valued facet ["+multiValuedFacet+"] which is named ["+multiValuedFacet.getName()+"] which is of type ["+multiValuedFacet.getType()+"]");
					if (DEBUG) log.debug("Inspecting multiValuedFacet.getLexicalFacetValues() for ["+multiValuedFacet.getName()+"] which has value of ["+multiValuedFacet.getLexicalFacetValues()+"]");
					if (DEBUG) log.debug("Inspecting multiValuedFacet.getEnumerationValues() for ["+multiValuedFacet.getName()+"] which has value of ["+multiValuedFacet.getEnumerationValues()+"]");
					if (DEBUG) log.debug("Inspecting multiValuedFacet.getFacetKind() == enum for ["+multiValuedFacet.getName()+"] which has value of ["+(multiValuedFacet.getFacetKind() == XSSimpleTypeDefinition.FACET_ENUMERATION)+"]");
					if (DEBUG) log.debug("Inspecting multiValuedFacet.getFacetKind() == pattern for ["+multiValuedFacet.getName()+"] which has value of ["+(multiValuedFacet.getFacetKind() == XSSimpleTypeDefinition.FACET_PATTERN)+"]");

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

	private String getJsonDataType(short builtInKind){
		String type;
		switch(builtInKind) {
			case XSConstants.BOOLEAN_DT:
				type="boolean";
				break;
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
				type="integer";
				break;
			case XSConstants.UNSIGNEDLONG_DT:
			case XSConstants.LONG_DT:
			case XSConstants.DECIMAL_DT:
			case XSConstants.FLOAT_DT:
			case XSConstants.DOUBLE_DT:
				type="number";
				break;
			case XSConstants.DATE_DT:
				type="date";
				break;
			case XSConstants.DATETIME_DT:
				type="date-time";
				break;
			default:
				type="string";
				break;
		}
		return type;
	}

}