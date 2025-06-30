package org.frankframework.management.security;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;

import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.util.UUIDUtil;


@Log4j2
public abstract class AbstractJwtKeyGenerator {

	protected JWSSigner signer;
	protected JWSHeader jwtHeader;

	@Getter
	protected String publicJwkSet;

	public @Nonnull String create() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			throw new AuthenticationServiceException("No Authentication object found in SecurityContext");
		}

		JWTClaimsSet claims = createClaimsSet(authentication);
		return createJwtToken(claims);
	}

	protected @Nonnull JWTClaimsSet createClaimsSet(Authentication authentication) {
		try {
			JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
					.subject(getPrincipalName(authentication))
					.expirationTime(Date.from(Instant.now().plusSeconds(120)))
					.issueTime(Date.from(Instant.now()))
					.jwtID(UUIDUtil.createRandomUUID())
					.claim("scope", mapAuthorities(authentication));

			addCustomClaims(builder, authentication);
			return builder.build();
		} catch (Exception e) {
			throw new AuthenticationServiceException("Unable to generate JWT ClaimsSet", e);
		}
	}

	protected String getPrincipalName(Authentication authentication) {
		return authentication.getName();
	}

	protected List<String> mapAuthorities(Authentication authentication) {
		return authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.collect(Collectors.toList());
	}

	protected void addCustomClaims(JWTClaimsSet.Builder builder, Authentication authentication) {
		// Optional: overridden in subclasses
	}

	protected @Nonnull String createJwtToken(@Nonnull JWTClaimsSet claims) {
		SignedJWT signedJWT = new SignedJWT(jwtHeader, claims);
		try {
			signedJWT.sign(signer);
		} catch (Exception e) {
			throw new AuthenticationServiceException("Unable to sign JWT", e);
		}
		String jwt = signedJWT.serialize();
		log.trace("Generated JWT token [{}]", jwt);
		return jwt;
	}
}
