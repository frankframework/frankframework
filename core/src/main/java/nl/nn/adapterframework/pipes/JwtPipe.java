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
package nl.nn.adapterframework.pipes;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTClaimsSet.Builder;
import com.nimbusds.jwt.SignedJWT;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.Category;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.CredentialFactory;

/**
 * Creates a JWT
 *
 * @author Niels Meijer
 * @since 7.9
 */
@Category("Basic")
public class JwtPipe extends FixedForwardPipe {
	private static final String SHARED_SECRET_PARAMETER_NAME = "sharedSecret";

	private JWSHeader jwtHeader;
	private JWSSigner globalSigner;

	private String sharedSecret;
	private String sharedSecretAlias;
	private int expirationTime = 600;

	@Override
	public void configure() throws ConfigurationException {
		parameterNamesMustBeUnique = true;
		super.configure();

		jwtHeader = new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build();

		if(StringUtils.isNotEmpty(sharedSecret) || StringUtils.isNotEmpty(sharedSecretAlias)) {
			try {
				CredentialFactory credentialFactory = new CredentialFactory(sharedSecretAlias, null, () -> sharedSecret);
				globalSigner = new MACSigner(credentialFactory.getPassword());
			} catch (KeyLengthException e) {
				throw new ConfigurationException("invalid shared key", e);
			}
		}

		if(globalSigner == null && getParameterList().findParameter(SHARED_SECRET_PARAMETER_NAME) == null) {
			throw new ConfigurationException("must either provide a [sharedSecret] (alias) or parameter");
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		final JWSSigner signer;
		Builder claimsSetBuilder = new JWTClaimsSet.Builder();

		Map<String, Object> parameterMap = getParameterValueMap(message, session);
		if(parameterMap != null) {
			Object sharedKey = parameterMap.remove(SHARED_SECRET_PARAMETER_NAME);
			parameterMap.forEach(claimsSetBuilder::claim);

			signer = getSigner(sharedKey);
		} else {
			signer = globalSigner;
		}

		if(expirationTime > 0) {
			Date expirationDate = Date.from(Instant.now().plusSeconds(expirationTime));
			claimsSetBuilder.expirationTime(expirationDate);
		}
		claimsSetBuilder.issueTime(Date.from(Instant.now()));

		String jwtToken = createAndSignJwtToken(signer, claimsSetBuilder.build());
		return new PipeRunResult(getSuccessForward(), Message.asMessage(jwtToken));
	}

	private JWSSigner getSigner(Object sharedKey) throws PipeRunException {
		if(Objects.nonNull(sharedKey)) {
			try {
				return new MACSigner(Message.asString(sharedKey));
			} catch (KeyLengthException | IOException e) {
				throw new PipeRunException(this, "invalid shared key", e);
			}
		}
		return globalSigner;
	}

	private Map<String, Object> getParameterValueMap(Message message, PipeLineSession session) throws PipeRunException {
		ParameterList parameterList = getParameterList();
		if(parameterList != null) {
			ParameterValueList pvl;
			try {
				pvl = parameterList.getValues(message, session);
			} catch (ParameterException e) {
				throw new PipeRunException(this, "unable to resolve parameters", e);
			}
			return pvl.getValueMap();
		}
		return null;
	}

	private @Nonnull String createAndSignJwtToken(@Nonnull JWSSigner signer, @Nonnull JWTClaimsSet claims) throws PipeRunException {
		SignedJWT signedJWT = new SignedJWT(jwtHeader, claims);

		try {
			signedJWT.sign(signer);
		} catch (JOSEException e) {
			throw new PipeRunException(this, "unable to sing JWT using [" + signer + "]", e);
		}

		String jwt = signedJWT.serialize();
		log.debug("generated JWT token [{}]", jwt);
		return jwt;
	}

	/** Authentication alias used for the SharedSecret */
	public void setAuthAlias(String alias) {
		this.sharedSecretAlias = alias;
	}
	/** Authentication alias used for authentication to the host */
	public void setSharedSecret(String sharedSecret) {
		this.sharedSecret = sharedSecret;
	}

	/**
	 * ExpirationTime in seconds, 0 to disable
	 * @ff.default 600
	 */
	public void setExpirationTime(int expirationTime) {
		this.expirationTime = expirationTime;
	}
}
