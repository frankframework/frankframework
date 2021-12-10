/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.jwt;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTClaimsSet;

import lombok.Getter;
import nl.nn.adapterframework.http.HttpSecurityHandler;

public class JwtSecurityHandler extends HttpSecurityHandler {

	private URL jwksUrl;
	private @Getter Map<String, Object> claimsSet;

	public JwtSecurityHandler(HttpServletRequest request, String authorizationHeader, URL jwksUrl, String remoteUser) throws IOException {
		super(request);
		this.jwksUrl = jwksUrl;
		validateAndParseToken(authorizationHeader, remoteUser);
	}

	private void validateAndParseToken(String header, String requiredIssuer) throws IOException {
		JwtValidator<SecurityContext> validator = new JwtValidator<SecurityContext>();
		String token = header;
		try {
			if(token.contains("Bearer ")) {
				token = token.substring(7);
			}
			validator.init(jwksUrl, requiredIssuer);
			JWTClaimsSet claims = validator.validateJWT(token);
			claimsSet = claims.toJSONObject();

		} catch (ParseException | BadJOSEException | JOSEException | IOException e) {
			throw new IOException("unable to validate token", e);
		}
	}
	
	public String getClaimsJson() {
		return JSONObjectUtils.toJSONString(claimsSet);
	}

	public boolean isClaimPresent(String claimName) {
		return claimsSet.containsKey(claimName);
	}

	public boolean isClaimHasExpectedValue(String claimName, String expectedClaimValue) {
		return expectedClaimValue.equals(claimsSet.get(claimName));
	}
}
