/*
   Copyright 2018 Nationale-Nederlanden

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
package nl.nn.adapterframework.pipes;

import java.util.Arrays;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.CredentialFactory;

/**
 * Pipe that hashes the imput
 * 
 * 
 * @author	Niels Meijer
 */
public class HashPipe extends FixedForwardPipe {

	private String algorithm = "HmacSHA256";
	private String encoding = "ISO8859_1";
	private String secret = null;
	private String authAlias = null;

	List<String> algorithms = Arrays.asList("HmacMD5", "HmacSHA1", "HmacSHA256", "HmacSHA384", "HmacSHA512");

	public void configure() throws ConfigurationException {
		super.configure();

		if (!algorithms.contains(getAlgorithm())) {
			throw new ConfigurationException("illegal value for algorithm [" + getAlgorithm() + "], must be " + algorithms.toString());
		}
	}

	@Override
	public PipeRunResult doPipe (Object input, IPipeLineSession session) throws PipeRunException {
		Message message = new Message(input);

		String authAlias = getAuthAlias();
		String secret = getSecret();
		try {
			ParameterList parameterList = getParameterList();
			ParameterValueList pvl = parameterList==null ? null : parameterList.getValues(message, session, isNamespaceAware());
			if(pvl != null) {
				String authAliasParamValue = (String)pvl.getValue("authAlias");
				if (StringUtils.isNotEmpty(authAliasParamValue)) {
					authAlias = authAliasParamValue;
				}
				String secretParamValue = (String)pvl.getValue("secret");
				if (StringUtils.isNotEmpty(secretParamValue)) {
					secret = secretParamValue;
				}
			}
		}
		catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session) + "exception extracting authAlias", e);
		}

		CredentialFactory accessTokenCf = new CredentialFactory(authAlias, "", secret);
		String cfSecret = accessTokenCf.getPassword();

		if(cfSecret == null || cfSecret.isEmpty())
			throw new PipeRunException(this, getLogPrefix(session) + "empty secret, unable to hash");

		try {
			Mac mac = Mac.getInstance(getAlgorithm());

			SecretKeySpec secretkey = new SecretKeySpec(cfSecret.getBytes(getEncoding()), "algorithm");
			mac.init(secretkey);

			String hash = Base64.encodeBase64String(mac.doFinal(message.asByteArray()));
			return new PipeRunResult(getForward(), hash);
		}
		catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session) + "error creating hash", e);
		}
	}

	public String getAlgorithm() {
		return algorithm;
	}

	@IbisDoc({"name of the pipe", "hmacsha256"})
	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	public String getEncoding() {
		return encoding;
	}

	@IbisDoc({"name of the pipe", "iso8859_1"})
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public String getSecret() {
		return secret;
	}

	@IbisDoc({"the secret to hash with", ""})
	public void setSecret(String secret) {
		this.secret = secret;
	}

	public String getAuthAlias() {
		return authAlias;
	}

	@IbisDoc({"authalias to retrieve the secret from (password field).", ""})
	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}
}