/*
   Copyright 2023 WeAreFrank!

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
package org.frankframework.aws;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.util.CredentialFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;

public class AwsBase {

	private @Setter @Getter String accessKey;
	private @Setter @Getter String secretKey;
	private @Getter String authAlias;

	private @Getter Region clientRegion = Region.EU_WEST_1;

	private @Setter @Getter String proxyHost = null;
	private @Setter @Getter Integer proxyPort = null;

	public AwsCredentialsProvider getAwsCredentialsProvider() {
		if ((StringUtils.isNotEmpty(getAccessKey()) && StringUtils.isEmpty(getSecretKey())) || (StringUtils.isEmpty(getAccessKey()) && StringUtils.isNotEmpty(getSecretKey()))) {
			throw new IllegalStateException("invalid credential fields, please provide AWS credentials (accessKey and secretKey)");
		}

		CredentialFactory cf = null;
		if (StringUtils.isNotEmpty(getAuthAlias()) || (StringUtils.isNotEmpty(getAccessKey()) && StringUtils.isNotEmpty(getSecretKey()))) {
			cf = new CredentialFactory(getAuthAlias(), getAccessKey(), getSecretKey());
		}
		return AwsUtil.createCredentialProviderChain(cf);
	}

	/** AuthAlias to provide accessKey and secretKey */
	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}

	/**
	 * AWS Client region
	 *
	 * @ff.default eu-west-1
	 */
	public void setClientRegion(Region clientRegion) {
		this.clientRegion = clientRegion;
	}
}
