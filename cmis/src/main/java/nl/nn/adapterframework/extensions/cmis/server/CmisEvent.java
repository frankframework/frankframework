/*
   Copyright 2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.cmis.server;

public enum CmisEvent {

	GET_OBJECT("getObject"), 
	GET_PROPERTIES("getProperties"), 
	GET_OBJECT_BY_PATH("getObjectByPath"), 
	UPDATE_PROPERTIES("updateProperties"), 
	DELETE_OBJECT("deleteObject"), 
	CREATE_ITEM("createItem"), 
	CREATE_DOCUMENT("createDocument"), 
	MOVE_OBJECT("moveObject"), 
	CREATE_FOLDER("createFolder"), 
	GET_ALLOWABLE_ACTIONS("getAllowableActions"), 
	GET_CONTENTSTREAM("getContentStream"), 
	GET_TYPE_DEFINITION("getTypeDefinition"), 
	GET_TYPE_DESCENDANTS("getTypeDescendants"), 
	GET_REPOSITORIES("getRepositories"), 
	GET_REPOSITORY_INFO("getRepositoryInfo"), 
	QUERY("query"),
	GET_CHILDREN("getChildren");

	private final String value;

	CmisEvent(String v) {
		value = v;
	}

	public String value() {
		return value;
	}

	public static CmisEvent fromValue(String v) {
		for (CmisEvent c : CmisEvent.values()) {
			if (c.value.equalsIgnoreCase(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}
}
