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
package nl.nn.adapterframework.management.security;

import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.Getter;
import nl.nn.adapterframework.util.Environment;
import nl.nn.adapterframework.util.UUIDUtil;

public class JwtKeyGenerator implements InitializingBean {
	private final Logger log = LogManager.getLogger(JwtKeyGenerator.class);
	private JWSSigner signer;
	private @Getter String publicJwkSet;
	private JWSHeader jwtHeader;

	@Override
	public void afterPropertiesSet() {
		try {
			ECKey key = new ECKeyGenerator(Curve.SECP256K1).keyIDFromThumbprint(true).generate();

			String version = Environment.getModuleVersion("iaf-management-gateway");
			log.info("initializing foo bar version [{}]", version);
			generateJWSHeader(key, version);

			// Store the public key
			ECPrivateKey privateKey = key.toECPrivateKey();
			signer = new ECDSASigner(privateKey, Curve.SECP256K1);
			JWKSet set = new JWKSet(key.toPublicJWK());
			publicJwkSet = set.toString();
		} catch (JOSEException e) {
			throw new IllegalStateException("unable to generate JWT header", e);
		}
	}

	private void generateJWSHeader(ECKey key, String version) {
		jwtHeader = new JWSHeader.Builder(JWSAlgorithm.ES256K)
				.type(JOSEObjectType.JWT)
				.customParam("version", version)
				.keyID(key.getKeyID()).build();
	}

	/**
	 * Create a new JWT based on the currently logged in user.
	 */
	public @Nonnull String create() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if(authentication == null) {
			throw new AuthenticationServiceException("no Authentication object found in SecurityContext"); //This should technically not be possible but...
		}

		//TODO can we save and reuse the JWT?
		JWTClaimsSet claims = createClaimsSet(authentication);
		return createJwtToken(claims);
	}

	private @Nonnull JWTClaimsSet createClaimsSet(Authentication authentication) {
		try {
			return new JWTClaimsSet.Builder()
					.subject(authentication.getName())
					.expirationTime(Date.from(Instant.now().plusSeconds(120)))
					.issueTime(Date.from(Instant.now()))
					.jwtID(UUIDUtil.createRandomUUID())
					.claim("scope", mapAuthorities(authentication))
					.build();
		} catch (Exception e) {
			throw new AuthenticationServiceException("unable to generate JWT ClaimsSet", e);
		}
	}

	//Should contain AuthorityAuthorizationManager#ROLE_PREFIX
	private List<String> mapAuthorities(Authentication authentication) {
		return authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.collect(Collectors.toList());
	}

	private @Nonnull String createJwtToken(@Nonnull JWTClaimsSet claims) {
		SignedJWT signedJWT = new SignedJWT(jwtHeader, claims);

		try {
			signedJWT.sign(signer);
		} catch (JOSEException e) {
			throw new AuthenticationServiceException("unable to sing JWT using ["+signer+"]", e);
		}

		String jwt = signedJWT.serialize();
		log.trace("generated JWT token [{}]", jwt);
		return jwt;
	}
}
