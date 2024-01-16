/*
   Copyright 2019 Nationale-Nederlanden, 2021 WeAreFrank!

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
package org.frankframework.extensions.cmis.server;

import org.frankframework.doc.DocumentedEnum;
import org.frankframework.doc.EnumLabel;

public enum CmisEvent implements DocumentedEnum {
	@EnumLabel("getObject") GET_OBJECT,
	@EnumLabel("getProperties") GET_PROPERTIES,
	@EnumLabel("getObjectByPath") GET_OBJECT_BY_PATH,
	@EnumLabel("updateProperties") UPDATE_PROPERTIES,
	@EnumLabel("deleteObject") DELETE_OBJECT,
	@EnumLabel("createItem") CREATE_ITEM,
	@EnumLabel("createDocument") CREATE_DOCUMENT,
	@EnumLabel("moveObject") MOVE_OBJECT,
	@EnumLabel("createFolder") CREATE_FOLDER,
	@EnumLabel("getAllowableActions") GET_ALLOWABLE_ACTIONS,
	@EnumLabel("getContentStream") GET_CONTENTSTREAM,
	@EnumLabel("getTypeDefinition") GET_TYPE_DEFINITION,
	@EnumLabel("getTypeDescendants") GET_TYPE_DESCENDANTS,
	@EnumLabel("getRepositories") GET_REPOSITORIES,
	@EnumLabel("getRepositoryInfo") GET_REPOSITORY_INFO,
	@EnumLabel("query") QUERY,
	@EnumLabel("getChildren") GET_CHILDREN;
}
