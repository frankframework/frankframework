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

import io.micrometer.common.util.StringUtils;

/**
 * Contains the settings for the SecretServer configuration.
 */
public record DelineaClientSettings(String tenant,
		String apiRootUrl,
		String apiRootUrlTemplate,
		String tokenUrlTemplate,
		String oauthTokenUrl,
		String oauthUsername,
		String oauthPassword,
		String tld) {
	private static final String DEFAULT_TLD = "com";

	private static final String DEFAULT_ROOT_URL_TEMPLATE = "https://%s.secretservercloud.%s/api/v1";

	private static final String DEFAULT_TOKEN_URL_TEMPLATE = "https://%s.secretservercloud.%s/oauth2/token";

	@Override
	public String apiRootUrlTemplate() {
		return StringUtils.isEmpty(apiRootUrlTemplate) ? DEFAULT_ROOT_URL_TEMPLATE : apiRootUrlTemplate;
	}

	@Override
	public String tokenUrlTemplate() {
		return StringUtils.isEmpty(tokenUrlTemplate) ? DEFAULT_TOKEN_URL_TEMPLATE : tokenUrlTemplate;
	}

	@Override
	public String tld() {
		return StringUtils.isEmpty(tld) ? DEFAULT_TLD : tld.replaceAll("^\\.*(.*?)\\.*$", "$1");
	}
}
