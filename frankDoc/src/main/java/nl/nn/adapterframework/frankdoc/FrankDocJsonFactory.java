/* 
Copyright 2021 WeAreFrank! 

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

package nl.nn.adapterframework.frankdoc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.frankdoc.model.AttributeType;
import nl.nn.adapterframework.frankdoc.model.AttributeEnumValue;
import nl.nn.adapterframework.frankdoc.model.AttributeEnum;
import nl.nn.adapterframework.frankdoc.model.ConfigChild;
import nl.nn.adapterframework.frankdoc.model.ElementChild;
import nl.nn.adapterframework.frankdoc.model.ElementType;
import nl.nn.adapterframework.frankdoc.model.FrankAttribute;
import nl.nn.adapterframework.frankdoc.model.FrankDocGroup;
import nl.nn.adapterframework.frankdoc.model.FrankDocModel;
import nl.nn.adapterframework.frankdoc.model.FrankElement;
import nl.nn.adapterframework.frankdoc.model.ObjectConfigChild;
import nl.nn.adapterframework.util.LogUtil;

public class FrankDocJsonFactory {
	private static Logger log = LogUtil.getLogger(FrankDocJsonFactory.class);

	private static final String DESCRIPTION_HEADER = "descriptionHeader";

	private FrankDocModel model;
	private JsonBuilderFactory bf;
	List<FrankElement> elementsOutsideChildren;

	public FrankDocJsonFactory(FrankDocModel model) {
		this.model = model;
		elementsOutsideChildren = new ArrayList<>(model.getElementsOutsideConfigChildren());
		bf = Json.createBuilderFactory(null);
	}

	public JsonObject getJson() {
		try {
			JsonObjectBuilder result = bf.createObjectBuilder();
			result.add("groups", getGroups());
			result.add("types", getTypes());
			result.add("elements", getElements());
			result.add("enums", getEnums());
			return result.build();
		} catch(JsonException e) {
			log.warn("Error producing JSON", e);
			return null;
		}
	}

	private JsonArray getGroups() throws JsonException {
		JsonArrayBuilder result = bf.createArrayBuilder();
		for(FrankDocGroup group: model.getGroups()) {
			result.add(getGroup(group));
		}
		return result.build();
	}

	private JsonObject getGroup(FrankDocGroup group) throws JsonException {
		JsonObjectBuilder result = bf.createObjectBuilder();
		result.add("name", group.getName());
		final JsonArrayBuilder types = bf.createArrayBuilder();
		group.getElementTypes().stream()
				.map(ElementType::getFullName)
				.forEach(types::add);
		if(group.getName().equals(FrankDocGroup.GROUP_NAME_OTHER)) {
			elementsOutsideChildren.forEach(f -> types.add(f.getFullName()));
		}
		result.add("types", types);
		return result.build();
	}

	private JsonArray getTypes() {
		JsonArrayBuilder result = bf.createArrayBuilder();
		List<ElementType> sortedTypes = new ArrayList<>(model.getAllTypes().values());
		Collections.sort(sortedTypes);
		for(ElementType elementType: sortedTypes) {
			result.add(getType(elementType));
		}
		elementsOutsideChildren.forEach(f -> result.add(getNonChildType(f)));
		return result.build();
	}

	private JsonObject getType(ElementType elementType) {
		JsonObjectBuilder result = bf.createObjectBuilder();
		result.add("name", elementType.getFullName());
		final JsonArrayBuilder members = bf.createArrayBuilder();
		elementType.getSyntax2Members().forEach(f -> members.add(f.getFullName()));
		result.add("members", members);
		return result.build();
	}

	private JsonObject getNonChildType(FrankElement frankElement) {
		JsonObjectBuilder result = bf.createObjectBuilder();
		result.add("name", frankElement.getFullName());
		final JsonArrayBuilder members = bf.createArrayBuilder();
		members.add(frankElement.getFullName());
		result.add("members", members);
		return result.build();
	}

	private JsonArray getElements() throws JsonException {
		List<FrankElement> allElements = new ArrayList<>(model.getAllElements().values());
		Collections.sort(allElements);
		JsonArrayBuilder result = bf.createArrayBuilder();
		for(FrankElement frankElement: allElements) {
			result.add(getElement(frankElement));
		}
		return result.build();
	}

	private JsonObject getElement(FrankElement frankElement) throws JsonException {
		JsonObjectBuilder result = bf.createObjectBuilder();
		result.add("name", frankElement.getSimpleName());
		result.add("fullName", frankElement.getFullName());
		if(frankElement.isAbstract()) {
			result.add("abstract", frankElement.isAbstract());
		}
		if(frankElement.isDeprecated()) {
			result.add("deprecated", frankElement.isDeprecated());
		}
		addDescriptionHeader(result, frankElement.getDescriptionHeader());
		addIfNotNull(result, "parent", getParentOrNull(frankElement));
		JsonArrayBuilder xmlElementNames = bf.createArrayBuilder();
		frankElement.getXmlElementNames().forEach(xmlElementNames::add);
		result.add("elementNames", xmlElementNames);
		JsonArray attributes = getAttributes(frankElement, getParentOrNull(frankElement) == null);
		if(! attributes.isEmpty()) {
			result.add("attributes", attributes);
		}
		List<FrankAttribute> nonInheritedAttributes = frankElement.getChildrenOfKind(ElementChild.JSON_NOT_INHERITED, FrankAttribute.class);
		if(! nonInheritedAttributes.isEmpty()) {
			JsonArrayBuilder b = bf.createArrayBuilder();
			nonInheritedAttributes.forEach(nia -> b.add(nia.getName()));
			result.add("nonInheritedAttributes", b.build());
		}
		JsonArray configChildren = getConfigChildren(frankElement);
		if(! configChildren.isEmpty()) {
			result.add("children", configChildren);
		}
		return result.build();
	}

	private static String getParentOrNull(FrankElement frankElement) {
		if(frankElement != null) {
			FrankElement parent = frankElement.getNextAncestorThatHasChildren(
					elem -> elem.getAttributes(ElementChild.ALL_NOT_EXCLUDED).isEmpty() && elem.getConfigChildren(ElementChild.ALL_NOT_EXCLUDED).isEmpty());
			if(parent != null) {
				return parent.getFullName();
			}
		}
		return null;
	}

	private JsonArray getAttributes(FrankElement frankElement, boolean addAttributeActive) throws JsonException {
		JsonArrayBuilder result = bf.createArrayBuilder();
		for(FrankAttribute attribute: frankElement.getAttributes(ElementChild.IN_COMPATIBILITY_XSD)) {
			result.add(getAttribute(attribute));
		}
		if(addAttributeActive) {
			result.add(getAttributeActive());
		}
		return result.build();
	}

	private JsonObject getAttribute(FrankAttribute frankAttribute) throws JsonException {
		JsonObjectBuilder result = bf.createObjectBuilder();
		result.add("name", frankAttribute.getName());
		if(frankAttribute.isDeprecated()) {
			result.add("deprecated", frankAttribute.isDeprecated());
		}
		result.add("describer", frankAttribute.getDescribingElement().getFullName());
		addIfNotNull(result, "description", frankAttribute.getDescription());
		addIfNotNull(result, "default", frankAttribute.getDefaultValue());
		if(! frankAttribute.getAttributeType().equals(AttributeType.STRING)) {
			result.add("type", frankAttribute.getAttributeType().name().toLowerCase());
		}
		if(frankAttribute.getAttributeEnum() != null) {
			result.add("enum", frankAttribute.getAttributeEnum().getFullName());
		}
		return result.build();
	}

	private JsonObject getAttributeActive() {
		JsonObjectBuilder result = bf.createObjectBuilder();
		result.add("name", "active");
		result.add("description", "If defined and empty or false, then this element and all its children are ignored");
		return result.build();
	}

	private void addIfNotNull(JsonObjectBuilder builder, String field, String value) {
		if(value != null) {
			builder.add(field, value);
		}
	}

	private void addDescriptionHeader(JsonObjectBuilder builder, String value) {
		if(! StringUtils.isBlank(value)) {
			builder.add(DESCRIPTION_HEADER, value.replaceAll("\"", "\\\\\\\""));
		}
	}

	private JsonArray getConfigChildren(FrankElement frankElement) throws JsonException {
		JsonArrayBuilder result = bf.createArrayBuilder();
		for(ConfigChild child: frankElement.getConfigChildren(ElementChild.IN_COMPATIBILITY_XSD)) {
			result.add(getConfigChild(child));
		}
		return result.build();
	}

	private JsonObject getConfigChild(ConfigChild child) throws JsonException {
		JsonObjectBuilder result = bf.createObjectBuilder();
		if(child.isDeprecated()) {
			result.add("deprecated", child.isDeprecated());
		}
		if(child.isMandatory()) {
			result.add("mandatory", child.isMandatory());
		}
		result.add("multiple", child.isAllowMultiple());
		result.add("roleName", child.getRoleName());
		addIfNotNull(result, "description", child.getDescription());
		if(child instanceof ObjectConfigChild) {
			result.add("type", ((ObjectConfigChild) child).getElementType().getFullName());
		}
		return result.build();
	}

	private JsonArray getEnums() {
		final JsonArrayBuilder result = bf.createArrayBuilder();
		for(AttributeEnum attributeEnum: model.getAllAttributeEnumInstances()) {
			result.add(getAttributeEnum(attributeEnum));
		}
		return result.build();
	}

	private JsonObject getAttributeEnum(AttributeEnum en) {
		final JsonObjectBuilder result = bf.createObjectBuilder();
		result.add("name", en.getFullName());
		result.add("values", getAttributeEnumValues(en));
		return result.build();
	}

	private JsonArray getAttributeEnumValues(AttributeEnum en) {
		JsonArrayBuilder result = bf.createArrayBuilder();
		for(AttributeEnumValue v: en.getValues()) {
			JsonObjectBuilder valueBuilder = bf.createObjectBuilder();
			valueBuilder.add("label", v.getLabel());
			if(v.getDescription() != null) {
				valueBuilder.add("description", v.getDescription());
			}
			result.add(valueBuilder.build());
		}
		return result.build();	
	}
}
