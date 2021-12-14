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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTClaimsSet;

import lombok.Getter;
import nl.nn.adapterframework.http.HttpSecurityHandler;

public class JwtSecurityHandler extends HttpSecurityHandler {

	private @Getter Map<String, Object> claimsSet;

	public JwtSecurityHandler(HttpServletRequest request, JwtWrapper jwtWrapper) throws Exception {
		super(request);
		validateAndParseToken(jwtWrapper);
	}

	private void validateAndParseToken(JwtWrapper jwtWrapper) throws Exception {
		JwtValidator<SecurityContext> validator = new JwtValidator<SecurityContext>();
		validator.init(jwtWrapper);
		JWTClaimsSet claims = validator.validateJWT(jwtWrapper.getToken());
		claimsSet = claims.toJSONObject();

	}

	public String getClaimsJson() {
		return JSONObjectUtils.toJSONString(claimsSet);
	}

}
