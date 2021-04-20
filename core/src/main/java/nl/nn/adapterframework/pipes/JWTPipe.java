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
package nl.nn.adapterframework.pipes;

import java.io.IOException;
import java.security.Key;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.StringTokenizer;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.InvalidClaimException;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.PrematureJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.InvalidKeyException;
import io.jsonwebtoken.security.SignatureException;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.jwt.JWTKeyResolver;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.Misc;

/**
 * Pipe that performs JWT encoding and decoding. Output is the encoded JWT for encoding and a JSON containing the JWT claims for decoding.
 * 
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default when the JWT has been encoded/decoded succesfully</td></tr>
 * <tr><td>"failure"</td><td>only for decoding: one of the required claims did not match the required value</td></tr>
 * <tr><td>"expired"</td><td>only for decoding: token has the <code>Expiration Time</code> claim set to a time before the current time including allowed clock skew. If "expired" is not specified, "failure" is used in such a case</td></tr>
 * <tr><td>"premature"</td><td>only for decoding: token has the <code>Not Before</code> claim set to a time after the current time including allowed clock skew. If "premature" is not specified, "failure" is used in such a case</td></tr>
 * </table>
 * </p>
 * @since   7.7
 * @author  Ricardo van Holst
 */
public class JWTPipe extends FixedForwardPipe {
	private @Getter Direction direction = Direction.ENCODE;
	
	private final static String PARAM_ISSUER = "issuer";
	private final static String PARAM_SUBJECT = "subject";
	private final static String PARAM_AUDIENCE = "audience";
	private final static String PARAM_JTI = "jti";
	private final static String PARAM_ISSUEDAT = "issuedAt";
	private final static String PARAM_NOTBEFORE = "notBefore";
	private final static String PARAM_EXPIRATION = "expiration";

	private @Getter String issuer;
	private @Getter String subject;
	private @Getter String audience;
	private @Getter String jti;
	private @Getter String customClaimsParams;
	
	// Encode properties
	private @Getter SignatureAlgorithm algorithm;
	private @Getter boolean issuedAtNow = true;
	private @Getter Integer notBeforeOffset;
	private @Getter Integer expirationOffset;
		
	// Decode properties
	private @Getter int allowedClockSkew = 60;
	
	private @Getter @Setter JWTKeyResolver jWTKeyResolver;
	
	private PipeForward failureForward; // forward used when verification fails
	private PipeForward expiredForward; // forward used when the token is expired
	private PipeForward prematureForward; // forward used when the token is not yet valid
	
	public enum Direction {
		ENCODE,
		DECODE;
	}
	
	public JWTPipe() {
		super();
		setJWTKeyResolver(new JWTKeyResolver());
	}
	
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		
		if (getDirection()==Direction.DECODE) {
			if (getAllowedClockSkew() < 0) {
				throw new ConfigurationException(getLogPrefix(null)+"allowedClockSkew is set to ["+getAllowedClockSkew()+"], which is not enough for adequate operation");
			}
			
			failureForward = findForward("failure");
			if (failureForward==null)  {
				throw new ConfigurationException("Forward [failure] must be specfied");
			}
			expiredForward = findForward("expired");
			if (expiredForward==null)  {
				expiredForward = failureForward;
			}
			prematureForward = findForward("premature");
			if (prematureForward==null)  {
				prematureForward = failureForward;
			}
		}
		
		getJWTKeyResolver().configure(this);
	}
	
	@Override
	public void start() throws PipeStartException {
		super.start();
		
		getJWTKeyResolver().start();
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		ParameterValueList pvl = null;
		if (getParameterList() != null) {
			try {
				pvl = getParameterList().getValues(message, session);
			} catch (ParameterException e) {
				throw new PipeRunException(this, getLogPrefix(session) + "exception on extracting parameters", e);
			}
		}
		
		String issuer_work = getParameterValue(pvl, PARAM_ISSUER);
		if (issuer_work==null) {
			issuer_work = getIssuer();
		}
		String subject_work = getParameterValue(pvl, PARAM_SUBJECT);
		if (subject_work==null) {
			subject_work = getSubject();
		}
		String audience_work = getParameterValue(pvl, PARAM_AUDIENCE);
		if (audience_work==null) {
			audience_work = getAudience();
		}
		String jti_work = getParameterValue(pvl, PARAM_JTI);
		if (jti_work==null) {
			jti_work = getJti();
		}
		
		if (getDirection()==Direction.DECODE) {
			JwtParserBuilder jwtParserBuilder = Jwts.parserBuilder().setAllowedClockSkewSeconds(getAllowedClockSkew()).setSigningKeyResolver(getJWTKeyResolver());
			
			if (issuer_work!=null) {
				jwtParserBuilder.requireIssuer(issuer_work);
			}
			if (subject_work!=null) {
				jwtParserBuilder.requireSubject(subject_work);
			}
			if (audience_work!=null) {
				jwtParserBuilder.requireAudience(audience_work);
			}
			if (jti_work!=null) {
				jwtParserBuilder.requireId(jti_work);
			}
			if (getCustomClaimsParams() != null) {
				StringTokenizer st = new StringTokenizer(getCustomClaimsParams(), ",");
				while (st.hasMoreElements()) {
					String paramName = st.nextToken();
					ParameterValue paramValue = pvl.getParameterValue(paramName);
					if(paramValue != null)
						jwtParserBuilder.require(paramName, paramValue.asStringValue(null));
				}
			}
			
			Jwt<?, ?> jwt;
			
			try {
				jwt = jwtParserBuilder.build().parse(message.asString());
			} catch (SignatureException e) {
				throw new PipeRunException(this, getLogPrefix(session) + "invalid signature", e);
			} catch (MalformedJwtException e) {
				throw new PipeRunException(this, getLogPrefix(session) + "malformed JWT", e);
			} catch (InvalidClaimException e) {
				return new PipeRunResult(failureForward, claimsToString(e.getClaims()));
			} catch (ExpiredJwtException e) {
				return new PipeRunResult(expiredForward, claimsToString(e.getClaims()));
			} catch (PrematureJwtException e) {
				return new PipeRunResult(prematureForward, claimsToString(e.getClaims()));
			} catch (IOException e) {
				throw new PipeRunException(this, getLogPrefix(session) + "error validating JWT", e);
			}
			
			return new PipeRunResult(getForward(), jwt.getBody());
		} else {
			JwtBuilder jwtBuilder = Jwts.builder();
			
			if (issuer_work!=null) {
				jwtBuilder.setIssuer(issuer_work);
			}
			if (subject_work!=null) {
				jwtBuilder.setSubject(subject_work);
			}
			if (audience_work!=null) {
				jwtBuilder.setAudience(audience_work);
			}
			if (jti_work!=null) {
				jwtBuilder.setId(jti_work);
			}

			// Set times
			Calendar now = Calendar.getInstance();
			
			if (pvl.getParameterValue(PARAM_ISSUEDAT)!=null) {
				jwtBuilder.setIssuedAt((Date)pvl.getValue(PARAM_ISSUEDAT));
			} else if (isIssuedAtNow()) {
				jwtBuilder.setIssuedAt(now.getTime());
			}
			if (pvl.getParameterValue(PARAM_EXPIRATION)!=null) {
				jwtBuilder.setExpiration((Date)pvl.getValue(PARAM_EXPIRATION));
			} else if (getExpirationOffset()!=null) {
				Calendar expiration = now;
				expiration.add(Calendar.SECOND, getExpirationOffset());
				
				jwtBuilder.setExpiration(expiration.getTime());
			}
			if (pvl.getParameterValue(PARAM_NOTBEFORE)!=null) {
				jwtBuilder.setNotBefore((Date)pvl.getValue(PARAM_NOTBEFORE));
			} else if (getNotBeforeOffset()!=null) {
				Calendar notBefore = now;
				notBefore.add(Calendar.SECOND, getNotBeforeOffset());
				
				jwtBuilder.setNotBefore(notBefore.getTime());
			}
			if (getCustomClaimsParams() != null) {
				StringTokenizer st = new StringTokenizer(getCustomClaimsParams(), ",");
				while (st.hasMoreElements()) {
					String paramName = st.nextToken();
					ParameterValue paramValue = pvl.getParameterValue(paramName);
					if(paramValue != null)
						jwtBuilder.claim(paramName, paramValue.asStringValue(null));
				}
			}
			
			String result;
			try {
				Key key = getJWTKeyResolver().getSigningKey();
				if (getAlgorithm() != null) {
					jwtBuilder.signWith(key, getAlgorithm());
				} else {
					jwtBuilder.signWith(key);
				}
				result = jwtBuilder.compact();
			} catch (InvalidKeyException e) {
				throw new PipeRunException(this, getLogPrefix(session) + "invalid key", e);
			}
			
			return new PipeRunResult(getForward(), result);
		}
	}
	
	private String claimsToString(Claims claims) {
		StringBuilder mapAsString = new StringBuilder("{");
		for (Iterator<String> it = claims.keySet().iterator(); it.hasNext();) {
			String claim = it.next();
			mapAsString.append(claim + "=" + claims.get(claim) + ", ");
		}
		mapAsString.delete(mapAsString.length()-2, mapAsString.length()).append("}");
		return mapAsString.toString();
	}

	@IbisDoc({"1", "Either <code>encode</code> or <code>decode</code>", "encode"})
	public void setDirection(String direction) {
		this.direction = Misc.parse(Direction.class, direction);
		getJWTKeyResolver().setDirection(direction);
	}
	
	@IbisDoc({"2", "(Only used when direction=encode) Algorithm to use for signing the token, default the application attempts to determine the algorithm. Must be one of HS256, HS384, HS512, ES256, ES384, RS512, RS256, RS384, RS512, PS256, PS384, PS512", ""})
	public void setAlgorithm(String algorithm) {
		this.algorithm = Misc.parse(SignatureAlgorithm.class, algorithm);
		getJWTKeyResolver().setAlgorithm(algorithm);
	}

	@IbisDoc({"3", "If direction=encode: If set, sets the <code>Issuer</code> claim with this value"+
					"<br>If direction=decode: If set, the token must contain the <code>Issuer</code> claim with this value"+
					"<br>Override with parameter '" + PARAM_ISSUER + "'", ""})
	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	@IbisDoc({"4", "If direction=encode: If set, sets the <code>Subject</code> claim with this value"+
			"<br>If direction=decode: If set, the token must contain the <code>Subject</code> claim with this value"+
			"<br>Override with parameter '" + PARAM_SUBJECT + "'", ""})
	public void setSubject(String subject) {
		this.subject = subject;
	}

	@IbisDoc({"5", "If direction=encode: If set, sets the <code>Audience</code> claim with this value"+
			"<br>If direction=decode: If set, the token must contain the <code>Audience</code> claim with this value"+
			"<br>Override with parameter '" + PARAM_AUDIENCE + "'", ""})
	public void setAudience(String audience) {
		this.audience = audience;
	}

	@IbisDoc({"6", "If direction=encode: If set, sets the <code>JWT ID</code> claim with this value"+
			"<br>If direction=decode: If set, the token must contain the <code>JWT ID</code> claim with this value"+
			"<br>Override with parameter '" + PARAM_JTI + "'", ""})
	public void setJTI(String jti) {
		this.jti = jti;
	}

	@IbisDoc({"7", "(Only used when direction=decode) Allowed clock skew for <code>Expiration Time</code> and <code>Not Before</code> claims in seconds", "60"})
	public void setAllowedClockSkew(int allowedClockSkew) {
		this.allowedClockSkew = allowedClockSkew;
	}
	
	@IbisDoc({"8", "(Only used when direction=encode) If <code>true</code>, set the <code>Issued At</code> claim with the current time, override set time with '" + PARAM_ISSUEDAT + "'", "true"})
	public void setIssuedAtNow(boolean issuedAtNow) {
		this.issuedAtNow = issuedAtNow;
	}

	@IbisDoc({"9", "(Only used when direction=encode) If set, set the <code>Not Before</code> claim with the <code>current time + notBeforeOffset</code> in seconds, override set time with parameter '" + PARAM_NOTBEFORE + "'", ""})
	public void setNotBeforeOffset(int notBeforeOffset) {
		this.notBeforeOffset = notBeforeOffset;
	}

	@IbisDoc({"10", "(Only used when direction=encode) If set, set the <code>Expiration</code> claim with the <code>current time + expirationOffset</code> in seconds, override set time with parameter '" + PARAM_EXPIRATION + "'", ""})
	public void setExpirationOffset(int expirationOffest) {
		this.expirationOffset = expirationOffest;
	}

	@IbisDoc({"11", "If direction=encode: Comma separated list of parameter names that contain claims to add to the token with the parameter value"+
			"<br>If direction=decode: Comma separated list of parameter names that contain claims that should be present in the token with the parameter value", ""})
	public void setCustomClaimsParams(String customClaimsParams) {
		this.customClaimsParams = customClaimsParams;
	}

	@IbisDoc({"12", "Secret for the signature", "" })
	public void setSecret(String secret) {
		getJWTKeyResolver().setSecret(secret);
	}

	@IbisDoc({"13", "Alias that contains the secret for the signature", "" })
	public void setAuthAlias(String authAlias) {
		getJWTKeyResolver().setAuthAlias(authAlias);
	}

	@IbisDoc({"14", "If direction=encode, the keystore containing the private key to sign the token with, else the truststore containing the certificate for signature verification", "" })
	public void setKeystore(String keystore) {
		getJWTKeyResolver().setKeystore(keystore);
	}

	@IbisDoc({"15", "Type of the keystore", "pkcs12" })
	public void setKeystoreType(String keystoreType) {
		getJWTKeyResolver().setKeystoreType(keystoreType);
	}

	@IbisDoc({"16", "Alias of the key/certificate to use", "" })
	public void setTrustkeystoreAlias(String keystoreAlias) {
		getJWTKeyResolver().setKeystoreAlias(keystoreAlias);
	}

	@IbisDoc({"17", "Password of the keystore", "" })
	public void setKeystorePassword(String keystorePassword) {
		getJWTKeyResolver().setKeystorePassword(keystorePassword);
	}

	@IbisDoc({"18", "Alias containing the password of the keystore", "" })
	public void setKeystoreAuthAlias(String keystoreAuthAlias) {
		getJWTKeyResolver().setKeystoreAuthAlias(keystoreAuthAlias);
	}

	@IbisDoc({"19", "", "" })
	public void setKeyManagerAlgorithm(String managerAlgorithm) {
		getJWTKeyResolver().setKeyManagerAlgorithm(managerAlgorithm);
	}
}
