/*
   Copyright 2020 WeAreFrank!
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

package nl.nn.adapterframework.ldap;

import java.io.IOException;
import java.net.URL;

import org.junit.After;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.ldap.LdapQueryPipeBase;
import nl.nn.adapterframework.pipes.PipeTestBase;
import nl.nn.adapterframework.util.ClassUtils;

public abstract class LdapQueryPipeTestBase<P extends LdapQueryPipeBase> extends PipeTestBase<P> {
	protected InMemoryDirectoryServer inMemoryDirectoryServer = null;
	protected String baseDNs = "dc=ibissource,dc=org";

	@Override
	public void setup() throws ConfigurationException {
		super.setup();
		session = new PipeLineSessionBase();

		pipe.setBaseDN(baseDNs);

		try {
			startLdapServer();
		} catch (LDAPException e) {
			throw new ConfigurationException(e);
		} catch (IOException e) {
			throw new ConfigurationException(e);
		}

		LDAPConnection connection;
		try {
			connection = inMemoryDirectoryServer.getConnection();
		} catch (LDAPException e) {
			throw new ConfigurationException(e);
		}
		pipe.setUseSsl(false);
		pipe.setHost(connection.getConnectedAddress());
		pipe.setPort(connection.getConnectedPort());
		pipe.setUserName("");
		pipe.setPassword("");
	}

	public void startLdapServer() throws LDAPException, IOException {
		InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(baseDNs);
		config.setSchema(null);
		inMemoryDirectoryServer = new InMemoryDirectoryServer(config);

		String ldifDataFile = "Ldap/memberPipes.ldif";
		URL ldifDataUrl = ClassUtils.getResourceURL(ldifDataFile);
		if (ldifDataUrl == null) {
			throw new IOException("cannot find resource [" + ldifDataFile + "]");
		}
		inMemoryDirectoryServer.importFromLDIF(true, ldifDataUrl.getPath());
		inMemoryDirectoryServer.startListening();
	}

	@After
	public void stopLdapServer() {
		if (inMemoryDirectoryServer != null) {
			inMemoryDirectoryServer.shutDown(true);
		}
	}
}