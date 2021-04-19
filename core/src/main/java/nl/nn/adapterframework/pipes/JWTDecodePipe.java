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
import java.util.Iterator;
import java.util.StringTokenizer;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.InvalidClaimException;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.PrematureJwtException;
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
import nl.nn.adapterframework.jwt.DecoderSigningKeyResolver;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * Pipe that performs JWT decoding, output is a JSON containing the JWT claims.
 * 
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default when the JWT has been decoded succesfully</td></tr>
 * <tr><td>"failure"</td><td>one of the required claims did not match the required value</td></tr>
 * <tr><td>"expired"</td><td>token has the <code>Expiration Time</code> claim set to a time before the current time including allowed clock skew. If "expired" is not specified, "failure" is used in such a case</td></tr>
 * <tr><td>"premature"</td><td>token has the <code>Not Before</code> claim set to a time after the current time including allowed clock skew. If "premature" is not specified, "failure" is used in such a case</td></tr>
 * </table>
 * </p>
 * @since   7.7
 * @author  Ricardo van Holst
 */
public class JWTDecodePipe extends FixedForwardPipe {

	private final static String PARAM_ISSUER = "issuer";
	private final static String PARAM_SUBJECT = "subject";
	private final static String PARAM_AUDIENCE = "audience";
	private final static String PARAM_JTI = "jti";

	private @Getter String issuer;
	private @Getter String subject;
	private @Getter String audience;
	private @Getter String jti;
	private @Getter String customClaimsParams;
	
	private @Getter int allowedClockSkew = 60;
	
	private @Getter @Setter DecoderSigningKeyResolver decoderSigningKeyResolver;
	
	private PipeForward failureForward; // forward used when verification fails
	private PipeForward expiredForward; // forward used when the token is expired
	private PipeForward prematureForward; // forward used when the token is not yet valid
	
	public JWTDecodePipe() {
		super();
		setDecoderSigningKeyResolver(new DecoderSigningKeyResolver());
	}
	
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		
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
	
	@Override
	public void start() throws PipeStartException {
		super.start();
		
		getDecoderSigningKeyResolver().start();
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
		
		JwtParserBuilder jwtParserBuilder = Jwts.parserBuilder().setAllowedClockSkewSeconds(getAllowedClockSkew()).setSigningKeyResolver(getDecoderSigningKeyResolver());
		
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

	@IbisDoc({"1", "If set, the token must contain the <code>Issuer</code> claim with this value, override with parameter '" + PARAM_ISSUER + "'", ""})
	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	@IbisDoc({"2", "If set, the token must contain the <code>Subject</code> claim with this value, override with parameter '" + PARAM_SUBJECT + "'", ""})
	public void setSubject(String subject) {
		this.subject = subject;
	}

	@IbisDoc({"3", "If set, the token must contain the <code>Audience</code> claim with this value, override with parameter '" + PARAM_AUDIENCE + "'", ""})
	public void setAudience(String audience) {
		this.audience = audience;
	}

	@IbisDoc({"4", "If set, the token must contain the <code>JWT ID</code> claim with this value, override with parameter '" + PARAM_JTI + "'", ""})
	public void setJTI(String jti) {
		this.jti = jti;
	}

	@IbisDoc({"5", "Allowed clock skew for <code>Expiration Time</code> and <code>Not Before</code> claims in seconds", "60"})
	public void setAllowedClockSkew(int allowedClockSkew) {
		this.allowedClockSkew = allowedClockSkew;
	}

	@IbisDoc({"6", "Comma separated list of parameter names that contain claims that should be present in the token with the parameter value", ""})
	public void setCustomClaimsParams(String customClaimsParams) {
		this.customClaimsParams = customClaimsParams;
	}

	@IbisDoc({"7", "Secret for signature verification", "" })
	public void setSecret(String secret) {
		getDecoderSigningKeyResolver().setSecret(secret);
	}

	@IbisDoc({"8", "Alias that contains the secret for signature verification", "" })
	public void setAuthAlias(String authAlias) {
		getDecoderSigningKeyResolver().setAuthAlias(authAlias);
	}

	@IbisDoc({"9", "Truststore containing the certificate for signature verification", "" })
	public void setTruststore(String truststore) {
		getDecoderSigningKeyResolver().setTruststoreUrl(ClassUtils.getResourceURL(this, truststore));
	}

	@IbisDoc({"10", "Type of the truststore", "pkcs12" })
	public void setTruststoreType(String truststoreType) {
		getDecoderSigningKeyResolver().setTruststoreType(truststoreType);
	}

	@IbisDoc({"11", "Alias of the certificate to use", "" })
	public void setTruststoreAlias(String truststoreAlias) {
		getDecoderSigningKeyResolver().setTruststoreAlias(truststoreAlias);
	}

	@IbisDoc({"12", "Password of the truststore", "" })
	public void setTruststorePassword(String truststorePassword) {
		getDecoderSigningKeyResolver().setTruststorePassword(truststorePassword);
	}

	@IbisDoc({"13", "Alias containing the password of the truststore", "" })
	public void setTruststoreAuthAlias(String truststoreAuthAlias) {
		getDecoderSigningKeyResolver().setTruststoreAuthAlias(truststoreAuthAlias);
	}

	@IbisDoc({"14", "", "" })
	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		getDecoderSigningKeyResolver().setTrustManagerAlgorithm(trustManagerAlgorithm);
	}
}
