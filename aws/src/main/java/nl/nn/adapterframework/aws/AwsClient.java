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
package nl.nn.adapterframework.aws;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;

public class AwsClient {
	protected Logger log = LogUtil.getLogger(this);

	private static final List<String> AVAILABLE_REGIONS = getAvailableRegions();

	private @Getter String accessKey;
	private @Getter String secretKey;
	private @Getter String authAlias;

	private @Getter String clientRegion = Regions.EU_WEST_1.getName();

	private @Getter String proxyHost = null;
	private @Getter Integer proxyPort = null;

	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getAuthAlias()) && (StringUtils.isEmpty(getAccessKey()) || StringUtils.isEmpty(getSecretKey()))) {
			throw new ConfigurationException(" empty credential fields, please provide aws credentials");
		}

		if (StringUtils.isEmpty(getClientRegion()) || !AVAILABLE_REGIONS.contains(getClientRegion())) {
			throw new ConfigurationException(" invalid region [" + getClientRegion() + "] please use one of the following supported regions " + AVAILABLE_REGIONS.toString());
		}
	}

	protected AWSCredentialsProvider getCredentialsProvider() {
		CredentialFactory cf = new CredentialFactory(getAuthAlias(), getAccessKey(), getSecretKey());
		return new AWSStaticCredentialsProvider(new BasicAWSCredentials(cf.getUsername(), cf.getPassword()));
	}

	public static List<String> getAvailableRegions() {
		List<String> availableRegions = new ArrayList<String>(Regions.values().length);
		for (Regions region : Regions.values())
			availableRegions.add(region.getName());

		return availableRegions;
	}

	protected ClientConfiguration getProxyConfig() {
		ClientConfiguration proxyConfig = null;
		if (this.getProxyHost() != null && this.getProxyPort() != null) {
			proxyConfig = new ClientConfiguration();
			proxyConfig.setProtocol(Protocol.HTTPS);
			proxyConfig.setProxyHost(this.getProxyHost());
			proxyConfig.setProxyPort(this.getProxyPort());
		}
		return proxyConfig;
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

	/** AWS Client region
	 * @ff.default eu-west-1
	 */
	public void setClientRegion(String clientRegion) {
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
