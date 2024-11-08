/*
   Copyright 2019-2021 WeAreFrank!

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
package org.frankframework.ldap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingException;

import lombok.Getter;

import org.frankframework.cache.ICache;
import org.frankframework.cache.ICacheEnabled;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.PipeStartException;
import org.frankframework.stream.Message;
import org.frankframework.util.XmlBuilder;

/**
 * Pipe that returns the memberships of a userDN.
 * The input is a fullDn, of a user or a group.
 * <br/>
 * Sample result:<br/><code><pre>
 *	&lt;ldap&gt;
 *	 &lt;entry name="CN=xxyyzz,OU=Users,DC=domain,DC=ext"&gt;
 *	   &lt;attributes&gt;
 *	    &lt;attribute&gt;
 *	    &lt;attribute name="memberOf" value="Extern"/&gt;
 *	    &lt;attribute name="departmentCode" value="358000"/&gt;
 *	    &lt;attribute name="organizationalHierarchy"&gt;
 *	        &lt;item value="ou=zzyyxx"/&gt;
 *	        &lt;item value="ou=OPS&amp;IT,ou=Group,ou=domain,o=ext"/&gt;
 *	    &lt;/attribute>
 *	    &lt;attribute name="givenName" value="Gerrit"/>
 *	   &lt;/attributes&gt;
 *	  &lt;/entry&gt;
 *   &lt;entry&gt; .... &lt;/entry&gt;
 *   .....
 *	&lt;/ldap&gt;
 * </pre></code> <br/>
 *
 * @author Gerrit van Brakel
 */
public class LdapFindGroupMembershipsPipe extends AbstractLdapQueryPipe implements ICacheEnabled<String,Set<String>> {

	private @Getter boolean recursiveSearch = true;

	private LdapClient ldapClient;
	private ICache<String, Set<String>> cache;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		Map<String,Object> options=new HashMap<>();
		options.put("java.naming.provider.url",retrieveUrl(getHost(), getPort(), getBaseDN(), isUseSsl()));
		options.put(Context.SECURITY_AUTHENTICATION, "simple");
		options.put(Context.SECURITY_PRINCIPAL, cf.getUsername());
		options.put(Context.SECURITY_CREDENTIALS, cf.getPassword());
		ldapClient= new LdapClient(options);
		ldapClient.setCache(cache);
		ldapClient.configure();
	}

	@Override
	public void start() throws PipeStartException {
		super.start();
		ldapClient.open();
	}

	@Override
	public void stop() {
		try {
			ldapClient.close();
		} finally {
			super.stop();
		}
	}


	@Override
	public PipeRunResult doPipeWithException(Message message, PipeLineSession session) throws PipeRunException {
		if (message==null) {
			throw new PipeRunException(this, "input is null");
		}

		String searchedDN;
		try {
			searchedDN = message.asString();
		} catch (IOException e) {
			throw new PipeRunException(this, "Failure converting input to string", e);
		}

		Set<String> memberships;
		try {
			if (isRecursiveSearch()) {
				memberships = searchRecursivelyViaAttributes(searchedDN);
			} else {
				memberships = searchObjectForMultiValuedAttribute(searchedDN);
			}
			XmlBuilder result = new XmlBuilder("ldap");
			result.addSubElement("entryName", searchedDN);
			XmlBuilder attributes = new XmlBuilder("attributes");
			result.addSubElement(attributes);
			for (String membership:memberships) {
				XmlBuilder attribute = new XmlBuilder("attribute");
				attribute.addAttribute("attrID", "memberOf");
				attribute.setValue(membership,true);
				attributes.addSubElement(attribute);
			}
			return new PipeRunResult(getSuccessForward(), result.asMessage());
		} catch (NamingException e) {
			throw new PipeRunException(this, "exception on ldap lookup", e);
		}
	}

	public Set<String> searchRecursivelyViaAttributes(String searchedDN) throws NamingException {
		return ldapClient.searchRecursivelyViaAttributes(searchedDN, getBaseDN(), "memberOf");
	}

	public Set<String> searchObjectForMultiValuedAttribute(String searchedDN) throws NamingException {
		return ldapClient.searchObjectForMultiValuedAttribute(searchedDN, getBaseDN(), "memberOf");
	}


	@Override
	public void setCache(ICache<String, Set<String>> cache) {
		this.cache=cache;
	}
	@Override
	public ICache<String, Set<String>> getCache() {
		return cache;
	}

	/**
	 * when <code>true</code>, the memberOf attribute is also searched in all the found members
	 * @ff.default true
	 */
	public void setRecursiveSearch(boolean b) {
		recursiveSearch = b;
	}

}
