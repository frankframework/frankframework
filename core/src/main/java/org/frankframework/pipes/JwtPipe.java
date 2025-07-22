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
package org.frankframework.pipes;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import jakarta.annotation.Nonnull;

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

import lombok.AccessLevel;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.Category;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.TimeProvider;

/**
 * Creates a JWT with a shared secret using the HmacSHA256 algorithm.
 *
 * @ff.parameter {@value #SHARED_SECRET_PARAMETER_NAME} overrides attribute <code>sharedSecret</code>. This parameter has worse performance, compared to this pipes attribute.
 *
 * @author Niels Meijer
 * @since 7.9
 */
@Category(Category.Type.BASIC)
public class JwtPipe extends FixedForwardPipe {
	static final String SHARED_SECRET_PARAMETER_NAME = "sharedSecret";

	private @Setter(AccessLevel.PACKAGE) boolean jwtAllowWeakSecrets = AppConstants.getInstance().getBoolean("application.security.jwt.allowWeakSecrets", false);

	private JWSHeader jwtHeader;
	private JWSSigner globalSigner;

	private String sharedSecret;
	private String authAlias;
	private int expirationTime = 600;

	@Override
	public void configure() throws ConfigurationException {
		parameterNamesMustBeUnique = true;
		super.configure();

		jwtHeader = new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build();

		if (StringUtils.isNotEmpty(sharedSecret) || StringUtils.isNotEmpty(authAlias)) {
			try {
				CredentialFactory credentialFactory = new CredentialFactory(authAlias, null, () -> sharedSecret);
				String factoryPassword = credentialFactory.getPassword();
				if (jwtAllowWeakSecrets && StringUtils.isNotEmpty(factoryPassword)) {
					factoryPassword = StringUtils.rightPad(factoryPassword, 32, "\0");
				}
				globalSigner = new MACSigner(factoryPassword);
			} catch (KeyLengthException e) {
				throw new ConfigurationException("invalid shared key", e);
			}
		}

		if (globalSigner == null && !getParameterList().hasParameter(SHARED_SECRET_PARAMETER_NAME)) {
			throw new ConfigurationException("must either provide a [sharedSecret] (alias) or parameter");
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		Builder claimsSetBuilder = new JWTClaimsSet.Builder();

		Map<String, Object> parameterMap = getParameterValueMap(message, session);
		Object sharedSecretParam = parameterMap.remove(SHARED_SECRET_PARAMETER_NAME); //Remove the SharedKey, else it will be added as a JWT Claim
		parameterMap.forEach(claimsSetBuilder::claim);

		if (expirationTime > 0) {
			Date expirationDate = Date.from(TimeProvider.now().plusSeconds(expirationTime));
			claimsSetBuilder.expirationTime(expirationDate);
		}
		claimsSetBuilder.issueTime(Date.from(TimeProvider.now()));

		final JWSSigner signer = getSigner(sharedSecretParam);
		String jwtToken = createAndSignJwtToken(signer, claimsSetBuilder.build());
		return new PipeRunResult(getSuccessForward(), new Message(jwtToken));
	}

	/**
	 * Get Signer based on the SharedSecretKey parameter if it exists, else return the Global signer.
	 */
	private JWSSigner getSigner(Object sharedSecretParam) throws PipeRunException {
		if (Objects.nonNull(sharedSecretParam)) {
			try {
				String sharedSecretKey = MessageUtils.asString(sharedSecretParam);
				if (jwtAllowWeakSecrets && StringUtils.isNotEmpty(sharedSecretKey)) {
					sharedSecretKey = StringUtils.rightPad(sharedSecretKey, 32, "\0");
				}
				return new MACSigner(sharedSecretKey);
			} catch (KeyLengthException | IOException e) {
				throw new PipeRunException(this, "invalid shared key", e);
			}
		}
		return globalSigner;
	}

	private @Nonnull Map<String, Object> getParameterValueMap(Message message, PipeLineSession session) throws PipeRunException {
		ParameterList parameterList = getParameterList();
		ParameterValueList pvl;
		try {
			pvl = parameterList.getValues(message, session);
		} catch (ParameterException e) {
			throw new PipeRunException(this, "unable to resolve parameters", e);
		}
		return pvl.getValueMap();
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

	/** Auth Alias for the SharedSecret to be used when signing the JWT (using the HmacSHA256 algorithm) */
	public void setAuthAlias(String alias) {
		this.authAlias = alias;
	}

	/** Shared secret to be used when signing the JWT (using the HmacSHA256 algorithm) */
	public void setSharedSecret(String sharedSecret) {
		this.sharedSecret = sharedSecret;
	}

	/**
	 * JWT expirationTime in seconds, 0 to disable
	 *
	 * @ff.default 600
	 */
	public void setExpirationTime(int expirationTime) {
		this.expirationTime = expirationTime;
	}
}
