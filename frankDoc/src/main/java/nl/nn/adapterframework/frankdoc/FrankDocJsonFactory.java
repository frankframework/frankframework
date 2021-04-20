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

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.frankdoc.model.AttributeType;
import nl.nn.adapterframework.frankdoc.model.AttributeValues;
import nl.nn.adapterframework.frankdoc.model.ConfigChild;
import nl.nn.adapterframework.frankdoc.model.ElementChild;
import nl.nn.adapterframework.frankdoc.model.FrankAttribute;
import nl.nn.adapterframework.frankdoc.model.FrankDocGroup;
import nl.nn.adapterframework.frankdoc.model.FrankDocModel;
import nl.nn.adapterframework.frankdoc.model.FrankElement;
import nl.nn.adapterframework.util.LogUtil;

public class FrankDocJsonFactory {
	private static Logger log = LogUtil.getLogger(FrankDocJsonFactory.class);

	private FrankDocModel model;
	private JsonBuilderFactory bf;

	public FrankDocJsonFactory(FrankDocModel model) {
		this.model = model;
		bf = Json.createBuilderFactory(null);
	}

	public JsonObject getJson() {
		try {
			JsonObjectBuilder result = bf.createObjectBuilder();
			result.add("groups", getGroups());
			result.add("elements", getElements());
			return result.build();
		} catch(JsonException e) {
			log.warn("Error producing JSON", e);
			return null;
		}
	}

	private JsonArray getGroups() throws JsonException {
		JsonArrayBuilder result = bf.createArrayBuilder();
		for(FrankDocGroup group: model.getGroups().values()) {
			result.add(getGroup(group));
		}
		return result.build();
	}

	private JsonObject getGroup(FrankDocGroup group) throws JsonException {
		JsonObjectBuilder result = bf.createObjectBuilder();
		result.add("name", group.getName());
		result.add("category", group.getCategory());
		final JsonArrayBuilder members = bf.createArrayBuilder();
		group.getElements().stream()
				.map(FrankElement::getFullName)
				.forEach(members::add);
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
		addIfNotNull(result, "parent", getParentOrNull(frankElement));
		JsonArrayBuilder xmlElementNames = bf.createArrayBuilder();
		frankElement.getXmlElementNames().forEach(xmlElementNames::add);
		result.add("elementNames", xmlElementNames);
		result.add("attributes", getAttributes(frankElement));
		result.add("children", getConfigChildren(frankElement));
		return result.build();
	}

	private static String getParentOrNull(FrankElement frankElement) {
		if(frankElement != null) {
			FrankElement parent = frankElement.getNextAncestorThatHasChildren(
					elem -> elem.getAttributes(ElementChild.ALL).isEmpty() && elem.getConfigChildren(ElementChild.ALL).isEmpty());
			if(parent != null) {
				return parent.getFullName();
			}
		}
		return null;
	}

	private JsonArray getAttributes(FrankElement frankElement) throws JsonException {
		JsonArrayBuilder result = bf.createArrayBuilder();
		for(FrankAttribute attribute: frankElement.getAttributes(ElementChild.IN_COMPATIBILITY_XSD)) {
			result.add(getAttribute(attribute));
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
		if(frankAttribute.getAttributeValues() != null) {
			result.add("values", getValues(frankAttribute.getAttributeValues()));
		}
		return result.build();
	}

	private void addIfNotNull(JsonObjectBuilder builder, String field, String value) {
		if(value != null) {
			builder.add(field, value);
		}
	}

	private JsonArray getValues(AttributeValues vl) {
		final JsonArrayBuilder result = bf.createArrayBuilder();
		vl.getValues().forEach(value -> result.add(value));
		return result.build();
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
		result.add("roleName", child.getElementRole().getRoleName());
		result.add("group", child.getElementRole().getElementType().getFrankDocGroup().getName());
		addIfNotNull(result, "description", child.getDescription());
		return result.build();
	}
}
