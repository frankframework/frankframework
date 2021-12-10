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
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.ResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import lombok.Getter;

public class JwtValidator<C extends SecurityContext> {

	private @Getter int connectTimeout=2000;
	private @Getter int readTimeout=2000;

	private @Getter ConfigurableJWTProcessor<C> jwtProcessor = null;

	public JwtValidator() {
		jwtProcessor = new DefaultJWTProcessor<C>();
	}
	
	public JwtValidator(URL jwksURL, String requiredIssuer) throws IOException, ParseException {
		this();
		init(jwksURL, requiredIssuer);
	}
	
	public void init(URL jwksURL, String requiredIssuer) throws IOException, ParseException {
		JWKSource<C> keySource = getKeySource(jwksURL);

		
		Set<JWSAlgorithm> algorithmSet = new LinkedHashSet<JWSAlgorithm>();
		algorithmSet.addAll(JWSAlgorithm.Family.HMAC_SHA);
		algorithmSet.addAll(JWSAlgorithm.Family.RSA);

		// Configure the JWT processor with a key selector to feed matching public
		// RSA keys sourced from the JWK set URL
		JWSKeySelector<C> keySelector = new JWSVerificationKeySelector<C>(algorithmSet, keySource);

		// Set up a JWT processor to parse the tokens and then check their signature
		// and validity time window (bounded by the "iat", "nbf" and "exp" claims)

		if (StringUtils.isNotEmpty(requiredIssuer)) {
			DefaultJWTClaimsVerifier<C> verifier=new DefaultJWTClaimsVerifier<C>() { // TODO: it is possible to provide required claims and exact value match for the claims in this constructor

				@Override
				public void verify(JWTClaimsSet claimsSet, C context) throws BadJWTException {
					super.verify(claimsSet, context);
					String issuer=claimsSet.getIssuer();
					if (!requiredIssuer.equals(issuer)) {
						throw new BadJWTException("illegal issuer ["+issuer+"], must be ["+requiredIssuer+"]");
					}
				}
			};
			getJwtProcessor().setJWTClaimsSetVerifier(verifier);
		}

		getJwtProcessor().setJWSKeySelector(keySelector);
	}

	protected JWKSource<C> getKeySource(URL jwksURL) throws IOException, ParseException {
		JWKSource<C> keySource = null;
		if(jwksURL.getProtocol().equals("file") || jwksURL.getProtocol().equals("jar")) {
			JWKSet set = JWKSet.load(jwksURL.openStream());
			keySource = new ImmutableJWKSet<C>(set);
			return keySource;
		} else {
			ResourceRetriever retriever = new DefaultResourceRetriever(getConnectTimeout(), getReadTimeout());
			//JWKSource<C> keySource = new RemoteJWKSet<C>(new URL(jwksURL),retriever);
			// Implemented Seam for Dependency Injection of JWKSource for unit testing
			keySource = new RemoteJWKSet<C>(jwksURL, retriever);
		}

		return keySource;
	}

	protected C createSecurityContext(String idToken) {
		return null;  // optional context parameter, not required here
	}

	public JWTClaimsSet validateJWT(String idToken) throws ParseException, BadJOSEException, JOSEException {
		// Process the token
		C ctx = createSecurityContext(idToken);
		JWTClaimsSet claimsSet = getJwtProcessor().process(idToken, ctx);
		return claimsSet;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

}
