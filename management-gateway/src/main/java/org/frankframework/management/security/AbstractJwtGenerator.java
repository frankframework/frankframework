/*
 * Copyright 2025-2026 WeAreFrank!
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.frankframework.management.security;

import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.util.Environment;
import org.frankframework.util.TimeProvider;
import org.frankframework.util.UUIDUtil;


@Log4j2
public abstract class AbstractJwtGenerator<T extends JWK> implements InitializingBean, ApplicationContextAware {

	public static final JWSAlgorithm JWT_DEFAULT_SIGNING_ALGORITHM = JWSAlgorithm.RS512;
	private final JWSAlgorithm algorithm;
	private final String generatorVersion;
	private @Setter ApplicationContext applicationContext;

	protected JWSHeader jwtHeader;

	private JWSSigner signer;

	@Getter
	protected String publicJwkSet;

	protected AbstractJwtGenerator(JWSAlgorithm algorithm) {
		this.algorithm = algorithm;
		String version = Environment.getModuleVersion("iaf-management-gateway");
		generatorVersion = StringUtils.isBlank(version) ? "unknown" : version;
		log.info("Initializing GeneratedJwtKeyGenerator version [{}]", generatorVersion);
	}

	@Override
	public void afterPropertiesSet() {
		final T jwk;
		try {
			jwk = getJwk();
			signer = getSigner(jwk);
		} catch (GeneralSecurityException | JOSEException e) {
			throw new AuthenticationServiceException("unable to create JWT signer", e);
		}

		try {
			jwtHeader = createJwsHeader(jwk.getKeyID());

			publicJwkSet = createJwkSet(jwk);
		} catch (Exception e) {
			throw new IllegalStateException("unable to generate JWT key", e);
		}
	}

	protected String createJwkSet(T jwk) {
		return new JWKSet(jwk.toPublicJWK()).toString();
	}

	protected abstract T getJwk() throws GeneralSecurityException, JOSEException;

	protected abstract JWSSigner getSigner(T jwkKey) throws JOSEException;

	private JWSHeader createJwsHeader(String keyId) {
		JWSHeader.Builder header = new JWSHeader.Builder(algorithm)
				.type(JOSEObjectType.JWT)
				.keyID(keyId);

		header.customParam("version", generatorVersion);
		header.customParam("iss", applicationContext.getDisplayName());

		return header.build();
	}

	/**
	 * Uses the Spring Authentication object to create a signed JWT.
	 */
	public final @NonNull String createJWT() {
		return createJWT(null);
	}

	/**
	 * Uses the Spring Authentication object to create a signed JWT.
	 * Allows users to provide additional JWT claims.
	 */
	public final @NonNull String createJWT(Consumer<JWTClaimsSet.Builder> additionalClaims) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			throw new AuthenticationServiceException("no Authentication object found in SecurityContext");
		}

		JWTClaimsSet claims = createClaimsSet(authentication, additionalClaims);
		return createJwtToken(claims);
	}

	private @NonNull JWTClaimsSet createClaimsSet(Authentication authentication, Consumer<JWTClaimsSet.Builder> claims) {
		try {
			JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
					.subject(getPrincipalName(authentication))
					.expirationTime(Date.from(TimeProvider.now().plusSeconds(120)))
					.issueTime(Date.from(TimeProvider.now()))
					.jwtID(UUIDUtil.createRandomUUID())
					.claim("scope", mapAuthorities(authentication));

			if (claims != null) {
				claims.accept(builder);
			}

			return builder.build();
		} catch (Exception e) {
			throw new AuthenticationServiceException("Unable to generate JWT ClaimsSet", e);
		}
	}

	protected String getPrincipalName(Authentication authentication) {
		return authentication.getName();
	}

	private List<String> mapAuthorities(Authentication authentication) {
		return authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.toList();
	}

	private @NonNull String createJwtToken(@NonNull JWTClaimsSet claims) {
		SignedJWT signedJWT = new SignedJWT(jwtHeader, claims);
		try {
			signedJWT.sign(signer);
			String jwt = signedJWT.serialize();
			log.trace("Generated JWT token [{}]", jwt);
			return jwt;
		} catch (Exception e) {
			throw new AuthenticationServiceException("Unable to sign JWT", e);
		}
	}
}
