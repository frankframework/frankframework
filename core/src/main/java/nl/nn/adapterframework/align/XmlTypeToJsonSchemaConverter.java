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
import org.apache.log4j.Logger;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSMultiValueFacet;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import org.apache.xerces.xs.XSWildcard;

import nl.nn.adapterframework.util.LogUtil;

public class XmlTypeToJsonSchemaConverter  {
	protected Logger log = LogUtil.getLogger(this.getClass());

	private List<XSModel> models;
	private boolean skipArrayElementContainers;
	private boolean skipRootElement;

	protected final boolean DEBUG=false; 

	private ArrayList<String> namedJsonObjects = new ArrayList<String>();
	private JsonObjectBuilder definitionsBuilder;

	public XmlTypeToJsonSchemaConverter(List<XSModel> models, boolean skipArrayElementContainers, boolean skipRootElement) {
		this.models=models;
		this.skipArrayElementContainers=skipArrayElementContainers;
		this.skipRootElement=skipRootElement;
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
		definitionsBuilder = Json.createObjectBuilder();
		if (skipRootElement) {
			JsonObject result = (JsonObject)getDefinition(elementDecl.getTypeDefinition(), false);
			result.entrySet().
					forEach(e -> builder.add(e.getKey(), e.getValue()));
		} else {
			buildTerm(builder,elementDecl,null, false, false);
		}
		JsonObject definitionsBuilderResult = definitionsBuilder.build();
		if(!definitionsBuilderResult.isEmpty()){
			builder.add("definitions", definitionsBuilderResult);
		}
		return builder.build();
	}
	
	private void buildReference(XSTypeDefinition typeDefinition, String complexTypeDefinitionName){
		if (DEBUG) log.debug("handleElementContents building ref for ["+complexTypeDefinitionName+"]!");
		namedJsonObjects.add(complexTypeDefinitionName);
		definitionsBuilder.add(complexTypeDefinitionName, getDefinition(typeDefinition, false));
	}

	public JsonStructure getDefinition(XSTypeDefinition typeDefinition) {
		return getDefinition(typeDefinition, true);
	}

	public JsonStructure getDefinition(XSTypeDefinition typeDefinition, 
	Boolean shouldCreateReferences) {
		JsonObjectBuilder builder = Json.createObjectBuilder();

		if (typeDefinition instanceof XSComplexTypeDefinition) {
			XSComplexTypeDefinition complexTypeDefinition = (XSComplexTypeDefinition)typeDefinition;
			switch (complexTypeDefinition.getContentType()) {
			case XSComplexTypeDefinition.CONTENTTYPE_EMPTY:
				if (DEBUG) log.debug("handleElementContents complexTypeDefinition.contentType is Empty, no child elements");
				break;
			case XSComplexTypeDefinition.CONTENTTYPE_SIMPLE:
				if (DEBUG) log.debug("handleElementContents complexTypeDefinition.contentType is Simple, no child elements (only characters)");
				break;
			case XSComplexTypeDefinition.CONTENTTYPE_ELEMENT:
				if (DEBUG) log.debug("handleElementContents complexTypeDefinition.contentType is Element, complexTypeDefinition ["+ToStringBuilder.reflectionToString(complexTypeDefinition,ToStringStyle.MULTI_LINE_STYLE)+"]");
				buildComplexTypeDefinition(complexTypeDefinition, shouldCreateReferences, builder);
				break;
			case XSComplexTypeDefinition.CONTENTTYPE_MIXED:
				if (DEBUG) log.debug("handleElementContents complexTypeDefinition.contentType is Mixed");
				break;
			default:
				throw new IllegalStateException("handleElementContents complexTypeDefinition.contentType is not Empty,Simple,Element or Mixed, but ["+complexTypeDefinition.getContentType()+"]");
			}
			if (DEBUG) log.debug(ToStringBuilder.reflectionToString(complexTypeDefinition,ToStringStyle.MULTI_LINE_STYLE));
		} else {
			buildSimpleTyeDefinition(typeDefinition, builder);
		}
		return builder.build();
	}

	private void buildComplexTypeDefinition(XSComplexTypeDefinition complexTypeDefinition,
	Boolean shouldCreateReferences, JsonObjectBuilder builder){
		if(shouldCreateReferences){
			String complexTypeDefinitionName = complexTypeDefinition.getName();

			if(complexTypeDefinitionName == null && complexTypeDefinition.getContext() != null  && complexTypeDefinition.getContext().getNamespaceItem() != null){
				complexTypeDefinitionName = complexTypeDefinition.getContext().getName();
			}

			if(complexTypeDefinitionName != null){
				if (DEBUG) log.debug("handleElementContents creating ref!");

				builder.add("$ref", "#/definitions/"+complexTypeDefinitionName);
				if(!namedJsonObjects.contains(complexTypeDefinitionName)){
					buildReference((XSTypeDefinition) complexTypeDefinition, complexTypeDefinitionName);
				}
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
		buildParticle(builder, particle, attributeUses);
	}
	
	private void buildSimpleTyeDefinition(XSTypeDefinition typeDefinition, 
	JsonObjectBuilder builder){
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

	public void buildParticle(JsonObjectBuilder builder, XSParticle particle, XSObjectList attributeUses) {
		if (particle==null) {
			throw new NullPointerException("particle is null");
		} 
		XSTerm term = particle.getTerm();
		if (term==null) {
			throw new NullPointerException("particle.term is null");
		}
		buildTerm(builder,term,attributeUses, particle.getMaxOccursUnbounded() || particle.getMaxOccurs()>1);
	}

	public void buildTerm(JsonObjectBuilder builder, XSTerm term, XSObjectList attributeUses, boolean multiOccurring) {
		buildTerm(builder, term, attributeUses, multiOccurring, true);
	}

	private void buildProperties(JsonObjectBuilder builder, XSObjectList particles, 
	XSObjectList attributeUses, boolean shouldCreateReferences){
		builder.add("type", "object");
		builder.add("additionalProperties", false);
		JsonObjectBuilder propertiesBuilder = Json.createObjectBuilder();
		List<String> requiredProperties = new ArrayList<String>();

		if (attributeUses!=null) {
			for (int i=0; i< attributeUses.getLength(); i++) {
				XSAttributeUse attributeUse = (XSAttributeUse)attributeUses.get(i);
				XSAttributeDeclaration attributeDecl = attributeUse.getAttrDeclaration();
				propertiesBuilder.add("@"+attributeDecl.getName(), getDefinition(attributeDecl.getTypeDefinition(), shouldCreateReferences));
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
			
			buildParticle(propertiesBuilder, childParticle, null);
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

	private void buildSkippableArrayContainer(XSParticle childParticle, boolean shouldCreateReferences,
	JsonObjectBuilder builder){
		JsonObjectBuilder refBuilder = Json.createObjectBuilder();
		buildParticle(refBuilder,childParticle,null);

		XSTerm childTerm = childParticle.getTerm();
		if( childTerm instanceof XSElementDeclaration ){
			XSElementDeclaration elementDeclaration=(XSElementDeclaration) childTerm;
			XSTypeDefinition elementTypeDefinition = elementDeclaration.getTypeDefinition();
			JsonStructure definition =getDefinition(elementTypeDefinition, shouldCreateReferences);
		
			builder.add("type", "array");
			if (elementDeclaration.getNillable()) {
				definition=nillable(definition);
			}
			builder.add("items", definition);
		}
	}

	private void buildElementDecleration(JsonObjectBuilder builder, XSTerm term,
	boolean multiOccurring, boolean shouldCreateReferences){	
		XSElementDeclaration elementDeclaration=(XSElementDeclaration)term;
		String elementName=elementDeclaration.getName();
		//if (DEBUG) log.debug("XSElementDeclaration name ["+elementName+"]");
		if (DEBUG) log.debug("XSElementDeclaration element ["+elementName+"]["+ToStringBuilder.reflectionToString(elementDeclaration,ToStringStyle.MULTI_LINE_STYLE)+"]");

		XSTypeDefinition elementTypeDefinition = elementDeclaration.getTypeDefinition();
		JsonStructure definition =getDefinition(elementTypeDefinition, shouldCreateReferences);
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
	private void buildWildcard(XSTerm term){
		XSWildcard wildcard=(XSWildcard)term;
		String processContents;
		switch (wildcard.getProcessContents()) {
		case XSWildcard.PC_LAX: processContents="LAX"; break;
		case XSWildcard.PC_SKIP: processContents="SKIP"; break;
		case XSWildcard.PC_STRICT: processContents="STRICT"; break;
		default: 
				throw new IllegalStateException("getBestMatchingElementPath wildcard.processContents is not PC_LAX, PC_SKIP or PC_STRICT, but ["+wildcard.getProcessContents()+"]");
		}
		String namespaceConstraint;
		switch (wildcard.getConstraintType()) {
		case XSWildcard.NSCONSTRAINT_ANY : namespaceConstraint="ANY"; break;
		case XSWildcard.NSCONSTRAINT_LIST : namespaceConstraint="SKIP "+wildcard.getNsConstraintList(); break;
		case XSWildcard.NSCONSTRAINT_NOT : namespaceConstraint="NOT "+wildcard.getNsConstraintList(); break;
		default: 
				throw new IllegalStateException("getBestMatchingElementPath wildcard.namespaceConstraint is not ANY, LIST or NOT, but ["+wildcard.getConstraintType()+"]");
		}
//			String msg="term for element ["+baseElementDeclaration.getName()+"] is WILDCARD; namespaceConstraint ["+namespaceConstraint+"] processContents ["+processContents+"]. Please check if the element typed properly in the schema";
//			if (isFailOnWildcards()) {
//				throw new IllegalStateException(msg+", or set failOnWildcards=\"false\"");
//			}
//			log.warn(msg);
	}

	private void buildCompositorAllSequence(JsonObjectBuilder builder, XSObjectList particles, 
	XSObjectList attributeUses, boolean shouldCreateReferences){
		if (DEBUG) log.debug("modelGroup COMPOSITOR_SEQUENCE or COMPOSITOR_ALL");
		if (skipArrayElementContainers && particles.getLength()==1) {
			XSParticle childParticle = (XSParticle)particles.item(0);
			if (childParticle.getMaxOccursUnbounded() || childParticle.getMaxOccurs()>1) {
				if (DEBUG) log.debug("skippable array element childParticle ["+ToStringBuilder.reflectionToString(particles.item(0),ToStringStyle.MULTI_LINE_STYLE)+"]");
				buildSkippableArrayContainer(childParticle, shouldCreateReferences, builder);
				return;
			}
		}
		buildProperties(builder, particles, attributeUses, shouldCreateReferences);
	}

	private void buildCompositorChoice(JsonObjectBuilder builder, XSObjectList particles){
		if (DEBUG) log.debug("modelGroup COMPOSITOR_CHOICE");
		JsonArrayBuilder oneOfBuilder = Json.createArrayBuilder();
		for (int i=0;i<particles.getLength();i++) {
			XSParticle childParticle = (XSParticle)particles.item(i);
			if (DEBUG) log.debug("childParticle ["+i+"]["+ToStringBuilder.reflectionToString(childParticle,ToStringStyle.MULTI_LINE_STYLE)+"]");
			JsonObjectBuilder typeBuilder = Json.createObjectBuilder();
			buildParticle(typeBuilder,childParticle,null);
			oneOfBuilder.add(typeBuilder.build());
		}
		builder.add("oneOf", oneOfBuilder.build());
	}
	
	private void buildModelGroup(JsonObjectBuilder builder, XSTerm term, XSObjectList attributeUses, 
	boolean shouldCreateReferences){
		XSModelGroup modelGroup = (XSModelGroup)term;
		short compositor = modelGroup.getCompositor();			
		XSObjectList particles = modelGroup.getParticles();
		if (DEBUG) log.debug("modelGroup ["+ToStringBuilder.reflectionToString(modelGroup,ToStringStyle.MULTI_LINE_STYLE)+"]");
		if (DEBUG) log.debug("modelGroup particles ["+ToStringBuilder.reflectionToString(particles,ToStringStyle.MULTI_LINE_STYLE)+"]");
		switch (compositor) {
		case XSModelGroup.COMPOSITOR_SEQUENCE:
		case XSModelGroup.COMPOSITOR_ALL:
			buildCompositorAllSequence(builder, particles, attributeUses, shouldCreateReferences);
			return;
		case XSModelGroup.COMPOSITOR_CHOICE:
			buildCompositorChoice(builder, particles);	
			return;
		default:
			throw new IllegalStateException("getTerm modelGroup.compositor is not COMPOSITOR_SEQUENCE, COMPOSITOR_ALL or COMPOSITOR_CHOICE, but ["+compositor+"]");
		} 
	}

	public void buildTerm(JsonObjectBuilder builder, XSTerm term, XSObjectList attributeUses, 
	boolean multiOccurring, boolean shouldCreateReferences) {
		if (term instanceof XSModelGroup) {
			buildModelGroup(builder, term, attributeUses, shouldCreateReferences);
			return;
		} 
		if (term instanceof XSElementDeclaration) {
			buildElementDecleration(builder, term, multiOccurring, shouldCreateReferences);
			return;
		}
		if (term instanceof XSWildcard) {
			buildWildcard(term);
			return;
		} 
		throw new IllegalStateException("getBestMatchingElementPath unknown Term type ["+term.getClass().getName()+"]");
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
						JsonArrayBuilder enumBuilder = Json.createArrayBuilder();

						/* 
							Isn't this strange?
							This assumes that an enumeration/pattern value is always a string, 
							
							don't we need to try and parse?
						*/

						for (int x=0; x<lexicalFacetValues.getLength(); x++) {
							lexicalFacetValue = lexicalFacetValues.item(x); 
							enumBuilder.add(lexicalFacetValue);
						}

						builder.add(key, enumBuilder.build());
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