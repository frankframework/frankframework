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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate implementation for the Delinea Secret Server REST API.
 */
public class DelineaClient extends RestTemplate {
	static final String SECRETS_URI = "/secrets";

	static final String SECRET_ID_URI = SECRETS_URI + "/{id}";

	/**
	 * Fetch and return a {@link Secret} from Delinea Secret Server, including {@code fileAttachments}
	 *
	 * @param id - the integer ID of the secret to be fetched
	 * @param autoCommentValue - the auto comment value to be used if not empty
	 * @return the {@link Secret} object
	 */
	public Secret getSecret(String id, String autoCommentValue) {
		final Map<String, String> params = new HashMap<>();
		params.put("id", id);

		// it is possible to provide an auto comment value, which is shown in the audit trail on the server
		// see: https://docs.delinea.com/online-help/integrations/uipath/config/enable-auto-comment.htm
		if (StringUtils.isNotBlank(autoCommentValue)) {
			params.put("autoComment", autoCommentValue);
		}

		return getForObject(SECRET_ID_URI, Secret.class, params);
	}

	public Secret getSecret(String id) {
		return getSecret(id, null);
	}

	/**
	 * @return a list of all Secrets
	 */
	public List<String> getSecrets() {
		List<SecretsList> resultPages = new ArrayList<>();
		boolean getNextPage = true;
		int skip = 0;

		while (getNextPage) {
			SecretsList page = getSecretsPage(skip);
			if (page != null) {
				resultPages.add(page);
				getNextPage = page.hasNext();
				skip = page.nextSkip();
			} else {
				getNextPage = false;
			}
		}

		return resultPages.stream()
				.flatMap(page -> page.records().stream())
				.map(SecretsList.CategorizedListSummary::id)
				.map(Objects::toString)
				.toList();
	}

	/**
	 * This is a paginated api. Called with a 'skip' parameter to skip a page of results.
	 */
	private SecretsList getSecretsPage(int skip) {
		final Map<String, String> params = new HashMap<>();
		params.put("skip", String.valueOf(skip));

		// see: https://updates.thycotic.net/secretserver/restapiguide/TokenAuth/#tag/Secrets/operation/SecretsService_SearchV2
		return getForObject(SECRETS_URI, SecretsList.class, params);
	}
}
