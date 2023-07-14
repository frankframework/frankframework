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
package nl.nn.adapterframework.filesystem.smb;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

import com.hierynomus.smbj.auth.GSSAuthenticationContext;

import nl.nn.adapterframework.filesystem.FileSystemException;
import nl.nn.adapterframework.util.CredentialFactory;

public class SambaFileSystemUtils {

	private static final String SPNEGO_OID = "1.3.6.1.5.5.2";
	private static final String KERBEROS5_OID = "1.2.840.113554.1.2.2";

	public static GSSAuthenticationContext createGSSAuthenticationContext(CredentialFactory credentialFactory) throws FileSystemException {
		try {
			Subject subject = krb5Login(credentialFactory);
			KerberosPrincipal krbPrincipal = subject.getPrincipals(KerberosPrincipal.class).iterator().next();

			final GSSManager manager = GSSManager.getInstance();

			final GSSName name = manager.createName(krbPrincipal.getName(), GSSName.NT_USER_NAME);
			Oid mech = getOidMechanismForName(manager, name);

			GSSCredential creds = Subject.doAs(subject, new PrivilegedExceptionAction<GSSCredential>() {
				@Override
				public GSSCredential run() throws GSSException {
					return manager.createCredential(name, GSSCredential.DEFAULT_LIFETIME, mech, GSSCredential.INITIATE_ONLY);
				}
			});

			return new GSSAuthenticationContext(krbPrincipal.getName(), krbPrincipal.getRealm(), subject, creds);
		} catch (GSSException e) {
			throw new FileSystemException("unable to convert kerberos principal to GSS name using oid ["+GSSName.NT_USER_NAME+"]", e);
		} catch (PrivilegedActionException e) {
			throw new FileSystemException("unable to get GSS credentials", e);
		}
	}

	private static Oid getOidMechanismForName(GSSManager manager, GSSName name) throws FileSystemException {
		try {
			Oid spnego = new Oid(SPNEGO_OID);
			Oid kerberos5 = new Oid(KERBEROS5_OID);

			Set<Oid> mechs = new HashSet<>(Arrays.asList(manager.getMechsForName(name.getStringNameType())));

			if (mechs.contains(kerberos5)) {
				return kerberos5;
			} else if (mechs.contains(spnego)) {
				return spnego;
			} else {
				throw new FileSystemException("no (valid) authentication mechanism found");
			}
		} catch (GSSException e) {
			throw new FileSystemException("invalid Object Identifier", e);
		}
	}

	private static Subject krb5Login(CredentialFactory credentialFactory) throws FileSystemException {
		Map<String, String> loginParams = new HashMap<>();
		loginParams.put("principal", credentialFactory.getUsername());

		try {
			LoginContext lc = new LoginContext(credentialFactory.getUsername(), null,
					new UsernameAndPasswordCallbackHandler(credentialFactory.getUsername(), credentialFactory.getPassword()),
					new KerberosLoginConfiguration(loginParams));
			lc.login();

			return lc.getSubject();
		} catch (LoginException e) {
			throw new FileSystemException("unable to authenticate user", e);
		}
	}
}
