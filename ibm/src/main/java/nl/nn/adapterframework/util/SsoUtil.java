/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.util;

import java.util.Iterator;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialExpiredException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.URI;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.CredentialDestroyedException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.ws.security.auth.WSCredentialImpl;

/**
 * Start of some generic utility class to generate SSO tokens.
 * 
 * @author  Gerrit van Brakel
 * @since   4.8
 * @version $Id$
 */
public class SsoUtil {
	private static Logger log = LogUtil.getLogger(SsoUtil.class);
	
	public static String getSsoTokenName() {
		return "LtpaToken";
	}
	
	public static String getSsoToken() throws WSSecurityException, CredentialDestroyedException, CredentialExpiredException {
		String result=null;
	
		Subject subj=WSSubject.getCallerSubject();

		if (subj==null) {
			throw new WSSecurityException("could not find Subject");
		}
		Set pubs=subj.getPublicCredentials();
		if (pubs==null) {
			throw new WSSecurityException("could not find PublicCredentials");
		}
		for (Iterator it=pubs.iterator();result==null && it.hasNext();) {
			Object pc = it.next();
			if (pc instanceof WSCredentialImpl) {
				WSCredentialImpl wsci = (WSCredentialImpl)pc;
				byte token[] = wsci.getCredentialToken();
				if (token!=null && token.length>0) {
					result=Base64.encodeBase64String(token);
				}
			}
		}
		return result;
	}

	public static void addSsoCredential(HttpMethod method, HttpState state, String defaultForwardHost) {
		try {
			String name=SsoUtil.getSsoTokenName();
			String value=SsoUtil.getSsoToken();
			if (StringUtils.isEmpty(value)) {
				if (log.isDebugEnabled()) log.debug("no value for SsoCredential ["+name+"]");
			} else {
				if (log.isDebugEnabled()) log.debug("constructing SsoCredentialCookie ["+name+"]");
				Cookie ssoCookie = new Cookie();
				ssoCookie.setName(name);
			
				ssoCookie.setValue(value);
				String forwardHost;
				try {
					URI uri = method.getURI();
					forwardHost = uri.getHost();
					if (StringUtils.isEmpty(forwardHost)) {
						if (log.isDebugEnabled()) log.debug("did not find host from URI ["+uri.getURI()+"], will use default ["+defaultForwardHost+"] for SSO credential cookie");
						forwardHost=defaultForwardHost;
					}
				} catch (Throwable t) {
					log.warn("could not extract host from URI", t);
					forwardHost = defaultForwardHost;					
				}
				ssoCookie.setDomain(forwardHost);
				// path must have a value, otherwise cookie is not appended to request
				ssoCookie.setPath("/");
				if (log.isDebugEnabled()) log.debug("set SSOcookie attributes: domain ["+ssoCookie.getDomain()+"] path ["+ssoCookie.getPath()+"]");
				state.addCookie(ssoCookie);
			}
			
		} catch (Exception e) {
			log.warn("could not obtain SsoToken: "+e.getMessage());
		}
	}

}
