/*
   Copyright 2022, 2023 WeAreFrank!

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
package org.frankframework.documentbuilder;

import java.util.Map.Entry;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import org.apache.commons.lang3.NotImplementedException;
import org.xml.sax.SAXException;

public class DocumentUtils {

	public static String DEFAULT_ARRAY_ELEMENT_NAME = "item";

	public static void jsonValue2Document(JsonValue jValue, IDocumentBuilder documentBuilder) throws SAXException {
		switch (jValue.getValueType()) {
		case ARRAY:
			try (ArrayBuilder arrayBuilder = documentBuilder.asArrayBuilder(DEFAULT_ARRAY_ELEMENT_NAME)) {
				jsonArray2Builder((JsonArray)jValue, arrayBuilder);
			}
			break;
		case OBJECT:
			try (ObjectBuilder objectBuilder = documentBuilder.asObjectBuilder()) {
				jsonObject2Builder((JsonObject)jValue, objectBuilder);
			}
			break;
		case NUMBER:
			documentBuilder.setValue(jValue.toString()); // works for XML, but will be quoted in JSON
			break;
		case STRING:
			documentBuilder.setValue(((JsonString) jValue).getString());
			break;
		case FALSE:
			documentBuilder.setValue(false);
			break;
		case TRUE:
			documentBuilder.setValue(true);
			break;
		case NULL:
			documentBuilder.setValue((String)null);
			break;
		default:
			throw new NotImplementedException("not implemented ["+jValue.getValueType()+"]");
		}
	}

	private static void jsonObject2Builder(JsonObject jObj, ObjectBuilder objectBuilder) throws SAXException {
		for (Entry<String,JsonValue> entry:jObj.entrySet()) {
			String n=entry.getKey();
			JsonValue v=entry.getValue();
			switch (v.getValueType()) {
			case ARRAY:
				JsonArray array = v.asJsonArray();
				try (ArrayBuilder arrayBuilder=objectBuilder.addRepeatedField(n)) {
					jsonArray2Builder(array, arrayBuilder);
				}
				break;
			case OBJECT:
				try (ObjectBuilder fieldObjectBuilder=objectBuilder.addObjectField(n)) {
					jsonObject2Builder((JsonObject)v, fieldObjectBuilder);
				}
				break;
			case STRING:
				objectBuilder.add(n, ((JsonString) v).getString());
				break;
			case NUMBER:
				objectBuilder.add(n, ((JsonNumber) v).toString()); // works for XML, but will be quoted in JSON
				break;
			case FALSE:
				objectBuilder.add(n, false);
				break;
			case TRUE:
				objectBuilder.add(n, true);
				break;
			case NULL:
				objectBuilder.add(n, (String)null);
				break;
			default:
				throw new NotImplementedException("not implemented ["+v.getValueType()+"]");
			}
		}
	}

	public static void jsonArray2Builder(JsonArray jArr, ArrayBuilder arrayBuilder) throws SAXException {
		for (JsonValue jValue:jArr) {
			switch (jValue.getValueType()) {
			case ARRAY:
				JsonArray array = jValue.asJsonArray();
				try (ArrayBuilder nestedArrayBuilder=arrayBuilder.addArrayElement(DEFAULT_ARRAY_ELEMENT_NAME)) {
					jsonArray2Builder(array, nestedArrayBuilder);
				}
				break;
			case OBJECT:
				try (ObjectBuilder objectBuilder=arrayBuilder.addObjectElement()) {
					jsonObject2Builder(jValue.asJsonObject(), objectBuilder);
				}
				break;
			case STRING:
				arrayBuilder.addElement(((JsonString) jValue).getString());
				break;
			case NUMBER:
				arrayBuilder.addElement(((JsonNumber) jValue).toString()); // works for XML, but will be quoted in JSON
				break;
			case FALSE:
				arrayBuilder.addElement(false);
				break;
			case TRUE:
				arrayBuilder.addElement(true);
				break;
			case NULL:
				arrayBuilder.addElement((String)null);
				break;
			default:
				throw new NotImplementedException("not implemented ["+jValue.getValueType()+"]");
			}
		}
	}
}
