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

import java.util.Date;
import java.util.StringTokenizer;

import io.jsonwebtoken.ExpiredJwtException;
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
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.jwt.EncoderSigningKeyResolver;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * Pipe that performs JWT encoding, output is the encoded JWT using the selected algorithm.
 *
 *
 * @since   7.7
 * @author  Ricardo van Holst
 */
public class JWTEncodePipe extends FixedForwardPipe {

	private final static String PARAM_ISSUER = "issuer";
	private final static String PARAM_SUBJECT = "subject";
	private final static String PARAM_AUDIENCE = "audience";
	private final static String PARAM_JTI = "jti";

	private @Getter String issuer;
	private @Getter String subject;
	private @Getter String audience;
	private @Getter String jti;
	private @Getter String customClaimsParams;
	
	private @Getter boolean verifyExpiration = true;
	private @Getter boolean verifyNotBefore = true;
	private @Getter int allowedClockSkew = 60;
	
	private @Getter @Setter EncoderSigningKeyResolver encoderSigningKeyResolver;
	
	public JWTEncodePipe() {
		super();
		setEncoderSigningKeyResolver(new EncoderSigningKeyResolver());
	}
	
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		
		if (getAllowedClockSkew() < 0) {
			throw new ConfigurationException(getLogPrefix(null)+"allowedClockSkew is set to ["+getAllowedClockSkew()+"], which is not enough for adequate operation");
		}
				
		getEncoderSigningKeyResolver().configure();
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
		
		JwtParserBuilder jwtParserBuilder = Jwts.parserBuilder().setAllowedClockSkewSeconds(getAllowedClockSkew()).setSigningKeyResolver(getEncoderSigningKeyResolver());
		Date now = new Date();
		
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
		if (isVerifyExpiration()) {
			jwtParserBuilder.requireExpiration(now);
		}
		if (isVerifyNotBefore()) {
			jwtParserBuilder.requireNotBefore(now);
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
			throw new PipeRunException(this, getLogPrefix(session) + "error validating JWT, invalid signature", e);
		} catch (ExpiredJwtException e) {
			throw new PipeRunException(this, getLogPrefix(session) + "error validating JWT, expired token", e);
		} catch (MalformedJwtException e) {
			throw new PipeRunException(this, getLogPrefix(session) + "error validating JWT, malformed token", e);
		} catch (PrematureJwtException e) {
			throw new PipeRunException(this, getLogPrefix(session) + "error validating JWT, premature token", e);
		} catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session) + "error validating JWT", e);
		}
		
		return new PipeRunResult(getForward(), jwt.getBody());
	}

	@IbisDoc({"1", "If set, the token must contain the 'issuer' claim with this value, override with parameter '" + PARAM_ISSUER + "'", ""})
	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	@IbisDoc({"2", "If set, the token must contain the 'subject' claim with this value, override with parameter '" + PARAM_SUBJECT + "'", ""})
	public void setSubject(String subject) {
		this.subject = subject;
	}

	@IbisDoc({"3", "If set, the token must contain the 'audience' claim with this value, override with parameter '" + PARAM_AUDIENCE + "'", ""})
	public void setAudience(String audience) {
		this.audience = audience;
	}

	@IbisDoc({"4", "If set, the token must contain the JWT ID claim with this value, override with parameter '" + PARAM_JTI + "'", ""})
	public void setJTI(String jti) {
		this.jti = jti;
	}

	@IbisDoc({"5", "If set, the token must contain the 'expiration time' claim with a timestamp in the future", "true"})
	public void setVerifyExpiration(boolean verifyExpiration) {
		this.verifyExpiration = verifyExpiration;
	}	

	@IbisDoc({"6", "If set, the token must contain the not before claim with a timestamp in the past", "true"})
	public void setVerifyNotBefore(boolean verifyNotBefore) {
		this.verifyNotBefore = verifyNotBefore;
	}

	@IbisDoc({"7", "Allowed clock skew for verifyExpiration and verifyNotBefore in seconds", "60"})
	public void setAllowedClockSkew(int allowedClockSkew) {
		this.allowedClockSkew = allowedClockSkew;
	}

	@IbisDoc({"8", "Comma separated list of parameter names that contain claims that should be present in the token with the parameter value", ""})
	public void setCustomClaimsParams(String customClaimsParams) {
		this.customClaimsParams = customClaimsParams;
	}

	@IbisDoc({"9", "Secret for signature verification", "" })
	public void setSecret(String secret) {
		getEncoderSigningKeyResolver().setSecret(secret);
	}

	@IbisDoc({"10", "Alias that contains the secret for signature verification", "" })
	public void setAuthAlias(String authAlias) {
		getEncoderSigningKeyResolver().setAuthAlias(authAlias);
	}

	@IbisDoc({"11", "Truststore containing the certificate for signature verification", "" })
	public void setTruststore(String truststore) {
		getEncoderSigningKeyResolver().setTruststoreUrl(ClassUtils.getResourceURL(this, truststore));
	}

	@IbisDoc({"12", "Type of the truststore", "pkcs12" })
	public void setTruststoreType(String truststoreType) {
		getEncoderSigningKeyResolver().setTruststoreType(truststoreType);
	}

	@IbisDoc({"13", "Alias of the certificate to use", "" })
	public void setTruststoreAlias(String truststoreAlias) {
		getEncoderSigningKeyResolver().setTruststoreAlias(truststoreAlias);
	}

	@IbisDoc({"14", "Password of the truststore", "" })
	public void setTruststorePassword(String truststorePassword) {
		getEncoderSigningKeyResolver().setTruststorePassword(truststorePassword);
	}

	@IbisDoc({"15", "Alias containing the password of the truststore", "" })
	public void setTruststoreAuthAlias(String truststoreAuthAlias) {
		getEncoderSigningKeyResolver().setTruststoreAuthAlias(truststoreAuthAlias);
	}

	@IbisDoc({"16", "", "" })
	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		getEncoderSigningKeyResolver().setTrustManagerAlgorithm(trustManagerAlgorithm);
	}
}
