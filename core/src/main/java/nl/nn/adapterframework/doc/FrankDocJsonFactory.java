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

package nl.nn.adapterframework.doc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nl.nn.adapterframework.doc.model.ConfigChild;
import nl.nn.adapterframework.doc.model.ElementChild;
import nl.nn.adapterframework.doc.model.FrankAttribute;
import nl.nn.adapterframework.doc.model.FrankDocGroup;
import nl.nn.adapterframework.doc.model.FrankDocModel;
import nl.nn.adapterframework.doc.model.FrankElement;
import nl.nn.adapterframework.util.LogUtil;

public class FrankDocJsonFactory {
	private static Logger log = LogUtil.getLogger(FrankDocJsonFactory.class);

	private FrankDocModel model;

	FrankDocJsonFactory(FrankDocModel model) {
		this.model = model;
	}

	public JSONObject getJson() {
		try {
			JSONObject result = new JSONObject();
			result.put("groups", getGroups());
			result.put("elements", getElements());
			return result;
		} catch(JSONException e) {
			log.warn("Error producing JSON", e);
			return null;
		}
	}

	private JSONArray getGroups() throws JSONException {
		JSONArray result = new JSONArray();
		for(FrankDocGroup group: model.getGroups().values()) {
			result.put(getGroup(group));
		}
		return result;
	}

	private JSONObject getGroup(FrankDocGroup group) throws JSONException {
		JSONObject result = new JSONObject();
		result.put("name", group.getName());
		result.put("category", group.getCategory());
		List<String> memberReferences = group.getElements().stream()
				.map(FrankElement::getFullName)
				.collect(Collectors.toList());
		result.put("members", memberReferences);
		return result;
	}

	private JSONArray getElements() throws JSONException {
		List<FrankElement> allElements = new ArrayList<>(model.getAllElements().values());
		Collections.sort(allElements);
		JSONArray result = new JSONArray();
		for(FrankElement frankElement: allElements) {
			result.put(getElement(frankElement));
		}
		return result;
	}

	private JSONObject getElement(FrankElement frankElement) throws JSONException {
		JSONObject result = new JSONObject();
		result.put("name", frankElement.getSimpleName());
		result.put("fullName", frankElement.getFullName());
		result.put("isAbstract", frankElement.isAbstract());
		result.put("isDeprecated", frankElement.isDeprecated());
		result.put("parent", getParentOrNull(frankElement));
		result.put("elementNames", new JSONArray(frankElement.getXmlElementNames()));
		result.put("attributes", getAttributes(frankElement));
		result.put("children", getConfigChildren(frankElement));
		return result;
	}

	private static String getParentOrNull(FrankElement frankElement) {
		return frankElement.getParent() == null? null: frankElement.getParent().getFullName();
	}

	private JSONArray getAttributes(FrankElement frankElement) throws JSONException {
		JSONArray result = new JSONArray();
		for(FrankAttribute attribute: frankElement.getAttributes(ElementChild.IN_COMPATIBILITY_XSD)) {
			result.put(getAttribute(attribute));
		}
		return result;
	}

	private JSONObject getAttribute(FrankAttribute frankAttribute) throws JSONException {
		JSONObject result = new JSONObject();
		result.put("name", frankAttribute.getName());
		result.put("isDeprecated", frankAttribute.isDeprecated());
		result.put("describer", frankAttribute.getDescribingElement().getFullName());
		result.put("description", frankAttribute.getDescription());
		result.put("default", frankAttribute.getDefaultValue());
		return result;
	}

	private JSONArray getConfigChildren(FrankElement frankElement) throws JSONException {
		JSONArray result = new JSONArray();
		for(ConfigChild child: frankElement.getConfigChildren(ElementChild.IN_COMPATIBILITY_XSD)) {
			result.put(getConfigChild(child));
		}
		return result;
	}

	private JSONObject getConfigChild(ConfigChild child) throws JSONException {
		JSONObject result = new JSONObject();
		result.put("isDeprecated", child.isDeprecated());
		result.put("isMandatory", child.isMandatory());
		result.put("isMultiple", child.isAllowMultiple());
		result.put("roleName", child.getElementRole().getSyntax1Name());
		result.put("group", child.getElementRole().getElementType().getFrankDocGroup().getName());
		result.put("description", child.getDescription());
		return result;
	}
}
