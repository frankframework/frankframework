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
package org.frankframework.management.security;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.nimbusds.jwt.JWTClaimsSet;

import org.frankframework.util.TimeProvider;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

	private static final long serialVersionUID = 1L;

	private final String principal;
	private final String key;
	private final String rawJwt;
	private final Date expiresAt;

	public JwtAuthenticationToken(JWTClaimsSet claimsSet, String rawJwt) throws ParseException {
		super(createAuthorityList(claimsSet));

		this.key = claimsSet.getJWTID();
		this.principal = claimsSet.getSubject();
		this.expiresAt = claimsSet.getExpirationTime();
		this.rawJwt = rawJwt;
	}

	@Override
	public boolean isAuthenticated() {
		return TimeProvider.nowAsDate().before(expiresAt);
	}

	private static List<GrantedAuthority> createAuthorityList(JWTClaimsSet claimsSet) throws ParseException {
		List<String> authorities = claimsSet.getStringListClaim("scope");
		if(authorities != null) {
			return authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
		}
		return AuthorityUtils.NO_AUTHORITIES;
	}

	@Override
	public Object getPrincipal() {
		return this.principal;
	}

	@Override
	public Object getCredentials() {
		return "";
	}

	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj)) {
			return false;
		}
		if (obj instanceof JwtAuthenticationToken other) {
			return this.key == other.key;
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + this.key.hashCode();
		return result;
	}

	public boolean verifyJWT(String jwt) {
		return rawJwt.equals(jwt);
	}
}
