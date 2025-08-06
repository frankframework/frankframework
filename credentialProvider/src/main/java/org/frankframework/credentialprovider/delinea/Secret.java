/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.credentialprovider.delinea;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Java representation of a <i>Secret</i> retrieved from Delinea Secret Server.
 * <br/>
 *
 * For example:
 * <pre>{@code
 *   {
 *     "id": 1,
 *     "name": "Example account",
 *     "secretTemplateId": 9,
 *     "folderId": 13,
 *     "active": true,
 *     "items": []
 *     ...
 *     "checkedOut": false,
 *     "checkOutEnabled": false,
 *     "siteId": 1,
 *     ...
 *     "enableInheritSecretPolicy": true,
 *     "secretPolicyId": -1,
 *     "lastHeartBeatStatus": "Pending",
 *     "lastHeartBeatCheck": null,
 *     "failedPasswordChangeAttempts": 0,
 *     "lastPasswordChangeAttempt": null,
 *     "secretTemplateName": "Web Password",
 *     "responseCodes": [],
 *     "webLauncherRequiresIncognitoMode": false
 *   }
 * }</pre>
 *
 * @see <a href="https://docs.delinea.com/online-help/platform-api/secret-server-apis-from-platform.htm#UsingtheAccessTokentoCalltheSecretServerAPI">example response</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Secret(
	int id,
	int folderId,
	String name,
	boolean active,

	// List of Field objects
	@JsonProperty("items")
	List<Field> fields) {

		@Override
		public List<Field> fields() {
			return Collections.unmodifiableList(fields); // Return an unmodifiable view
		}

		@Override
		public String toString() {
			return String.format("Secret { id: %d, folderId: %d, name: %s}",
					this.id, this.folderId, this.name);
		}

	/**
	 * Java representation of an <i>Item</i> of a <i>Secret</i>.
	 * <br/>
	 * For example:
	 *
	 * <pre>{@code
	 *   {
	 *     "itemId": 4,
	 *     "fileAttachmentId": null,
	 *     "filename": null,
	 *     "itemValue": "",
	 *     "fieldId": 41,
	 *     "fieldName": "Notes",
	 *     "slug": "notes",
	 *     "fieldDescription": "Any comments or additional information for the secret.",
	 *     "isFile": false,
	 *     "isNotes": true,
	 *     "isPassword": false,
	 *     "isList": false,
	 *     "listType": "None"
	 *   }
	 * }</pre>
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Field(
		@JsonProperty("itemId")
		int id,

		@JsonProperty("itemValue")
		String value,

		String slug) {
	}
}
