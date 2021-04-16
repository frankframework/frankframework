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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;

import javax.net.ssl.KeyManager;
import javax.net.ssl.X509KeyManager;

import org.apache.commons.lang3.StringUtils;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.InvalidKeyException;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.PkiUtil;

/**
 * Pipe that performs JWT encoding, output is a string containing the encoded JWT.
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
	private final static String PARAM_ISSUEDAT = "issuedAt";
	private final static String PARAM_NOTBEFORE = "notBefore";
	private final static String PARAM_EXPIRATION = "expiration";

	private @Getter SignatureAlgorithm algorithm;
	private @Getter String issuer;
	private @Getter String subject;
	private @Getter String audience;
	private @Getter String jti;
	private @Getter boolean issuedAtNow = true;
	private @Getter int notBeforeOffset = 0;
	private @Getter int expirationOffset = 0;
	private @Getter String customClaimsParams;

	private @Getter String secret;
	private @Getter String authAlias;
	private @Getter String keystore;
	private @Getter String keystoreType="pkcs12";
	private @Getter String keystoreAlias;
	private @Getter String keystorePassword;
	private @Getter String keystoreAuthAlias;
	private @Getter String keyManagerAlgorithm;
	
	private @Getter @Setter Key key;
	private @Getter URL keystoreUrl = null;
	
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		
		if (StringUtils.isNotEmpty(getKeystore())) {
			keystoreUrl = ClassUtils.getResourceURL(this, getKeystore());
			if (keystoreUrl == null) {
				throw new ConfigurationException("cannot find URL for keystore resource ["+getKeystore()+"]");
			}
		}
	}
	
	@Override
	public void start() throws PipeStartException {
		super.start();
		
		// Set the key to sign tokens
		Key key = null;
		if (getKeystoreUrl()!=null) {
			try {
				if ("pem".equals(getKeystoreType())) {
					key = PkiUtil.getPrivateKeyFromPem(getKeystoreUrl());
				} else {
					CredentialFactory cf = new CredentialFactory(getKeystoreAuthAlias(), "", getKeystorePassword());
					KeyStore keystore = PkiUtil.createKeyStore(getKeystoreUrl(), cf.getPassword(), getKeystoreType(), "Keys for signing tokens");
					KeyManager[] keymanagers = PkiUtil.createKeyManagers(keystore, cf.getPassword(), getKeyManagerAlgorithm());
					if (keymanagers==null || keymanagers.length==0) {
						throw new PipeStartException("No keymanager found for keystore ["+getKeystoreUrl()+"]");
					}
					X509KeyManager keyManager = (X509KeyManager)keymanagers[0];
					key = keyManager.getPrivateKey(getKeystoreAlias());
				}
			} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | UnrecoverableKeyException | InvalidKeySpecException e) {
				throw new PipeStartException("cannot get Secret Key for verification in keystore ["+keystoreUrl+"]", e);
			}
		} else if (getSecret()!=null || getAuthAlias()!=null) {
			// set the secret key used for HMAC requests only if the secret is set;
			CredentialFactory cf = new CredentialFactory(getAuthAlias(), "", getSecret());
			String cfSecret = cf.getPassword();
			key = Keys.hmacShaKeyFor(cfSecret.getBytes(StandardCharsets.UTF_8));
		}
		setKey(key);
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
		} else if (getExpirationOffset()>0) {
			Calendar expiration = now;
			expiration.add(Calendar.SECOND, getExpirationOffset());
			
			jwtBuilder.setExpiration(expiration.getTime());
		}
		if (pvl.getParameterValue(PARAM_NOTBEFORE)!=null) {
			jwtBuilder.setNotBefore((Date)pvl.getValue(PARAM_NOTBEFORE));
		} else if (getNotBeforeOffset()>0) {
			Calendar notBefore = now;
			notBefore.add(Calendar.SECOND, -getNotBeforeOffset());
			
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
	
	@IbisDoc({"1", "Algorithm to use for signing the token, default the application attempts to determine the algorithm. Must be one of HS256, HS384, HS512, ES256, ES384, RS512, RS256, RS384, RS512, PS256, PS384, PS512'", ""})
	public void setAlgorithm(SignatureAlgorithm algorithm) {
		this.algorithm = algorithm;
	}
	
	@IbisDoc({"2", "If set, sets the 'issuer' claim with this value, override with parameter '" + PARAM_ISSUER + "'", ""})
	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	@IbisDoc({"3", "If set, sets the 'subject' claim with this value, override with parameter '" + PARAM_SUBJECT + "'", ""})
	public void setSubject(String subject) {
		this.subject = subject;
	}

	@IbisDoc({"4", "If set, set the 'audience' claim with this value, override with parameter '" + PARAM_AUDIENCE + "'", ""})
	public void setAudience(String audience) {
		this.audience = audience;
	}

	@IbisDoc({"5", "If set, set the JWT ID claim with this value, override with parameter '" + PARAM_JTI + "'", ""})
	public void setJTI(String jti) {
		this.jti = jti;
	}

	@IbisDoc({"6", "If <code>true</code>, set the Issued At claim with the current time, override set time with '" + PARAM_ISSUEDAT + "'", "true"})
	public void setIssuedAtNow(boolean issuedAtNow) {
		this.issuedAtNow = issuedAtNow;
	}

	@IbisDoc({"7", "If <code>notBeforeOffset &gt; 0</code>, set the 'Not Before' claim with the <code>current time - notBeforeOffset</code> in seconds, override set time with parameter '" + PARAM_NOTBEFORE + "'", "0"})
	public void setNotBeforeOffset(int notBeforeOffset) {
		this.notBeforeOffset = notBeforeOffset;
	}

	@IbisDoc({"8", "If <code>expirationOffset &gt; 0</code>, set the 'Expiration' claim with the <code>current time + expirationOffset</code> in seconds, override set time with parameter '" + PARAM_EXPIRATION + "'", "0"})
	public void setExpirationOffset(int expirationOffest) {
		this.expirationOffset = expirationOffest;
	}

	@IbisDoc({"9", "Comma separated list of parameter names that contain claims to add to the token with the parameter value", ""})
	public void setCustomClaimsParams(String customClaimsParams) {
		this.customClaimsParams = customClaimsParams;
	}

	@IbisDoc({"10", "Secret for signing", "" })
	public void setSecret(String secret) {
		this.secret = secret;
	}

	@IbisDoc({"11", "Alias that contains the secret for signing", "" })
	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}

	@IbisDoc({"12", "Keystore containing the private key for signing", "" })
	public void setKeystore(String keystore) {
		this.keystore = keystore;
	}

	@IbisDoc({"13", "Type of the keystore", "pkcs12" })
	public void setKeystoreType(String keystoreType) {
		this.keystoreType = keystoreType;
	}

	@IbisDoc({"14", "Alias of the private key to use", "" })
	public void setKeystoreAlias(String keystoreAlias) {
		this.keystoreAlias = keystoreAlias;
	}

	@IbisDoc({"15", "Password of the keystore", "" })
	public void setKeystorePassword(String keystorePassword) {
		this.keystorePassword = keystorePassword;
	}

	@IbisDoc({"16", "Alias containing the password of the keystore", "" })
	public void setKeystoreAuthAlias(String keystoreAuthAlias) {
		this.keystoreAuthAlias = keystoreAuthAlias;
	}

	@IbisDoc({"17", "", "" })
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		this.keyManagerAlgorithm = keyManagerAlgorithm;
	}
}
