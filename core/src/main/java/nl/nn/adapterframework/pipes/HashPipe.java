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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.CredentialFactory;

/**
 * Pipe that hashes the imput
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAlgorithm(String) algorithm}</td><td>name of the Pipe</td><td>HmacSHA256</td></tr>
 * <tr><td>{@link #setEncoding(String) encoding}</td><td>name of the Pipe</td><td>ISO8859_1</td></tr>
 * <tr><td>{@link #setSecret(String) secret}</td><td>the secret to hash with</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAuthAlias(String) authAlias}</td><td>AuthAlias to retrieve the secret from (password field).</td><td>&nbsp;</td></tr>
 * </table>
 * <p><b>NOTE:</b> You can also retrieve the secret or authAlias from a parameter.</p>
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

	public PipeRunResult doPipe (Object input, IPipeLineSession session) throws PipeRunException {
		String message = (String) input;

		String authAlias = getAuthAlias();
		String secret = getSecret();
		try {
			ParameterList parameterList = getParameterList();
			ParameterResolutionContext prc = new ParameterResolutionContext(message, session);
			ParameterValueList pvl = prc.getValues(parameterList);
			if(pvl != null) {
				Parameter authAliasParam = parameterList.findParameter("authAlias");
				if(authAliasParam != null)
					authAlias = (String) authAliasParam.getValue(pvl, prc);

				Parameter secretParam = parameterList.findParameter("secret");
				if(secretParam != null)
					secret = (String) secretParam.getValue(pvl, prc);
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

			String hash = Base64.encodeBase64String(mac.doFinal(message.getBytes()));
			return new PipeRunResult(getForward(), hash);
		}
		catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session) + "error creating hash", e);
		}
	}

	public String getAlgorithm() {
		return algorithm;
	}
	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	public String getEncoding() {
		return encoding;
	}
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public String getSecret() {
		return secret;
	}
	public void setSecret(String secret) {
		this.secret = secret;
	}

	public String getAuthAlias() {
		return authAlias;
	}
	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}
}