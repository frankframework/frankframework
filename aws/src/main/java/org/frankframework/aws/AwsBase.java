/*
   Copyright 2023-2024 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import org.frankframework.util.CredentialFactory;

public class AwsBase {

	private @Getter String accessKey;
	private @Getter String secretKey;
	private @Getter String authAlias;

	private @Getter Region clientRegion = Region.EU_WEST_1;

	private @Getter String proxyHost = null;
	private @Getter Integer proxyPort = null;

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

	/** AWS accessKey */
	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	/** AWS secretKey */
	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	/** AuthAlias to provide accessKey and secretKey */
	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}

	/**
	 * AWS Client region
	 * @ff.default eu-west-1
	 */
	public void setClientRegion(Region clientRegion) {
		this.clientRegion = clientRegion;
	}

	/** Proxy host to use to connect to AWS service */
	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	/** Proxy port to use to connect to AWS service */
	public void setProxyPort(Integer proxyPort) {
		this.proxyPort = proxyPort;
	}

}
