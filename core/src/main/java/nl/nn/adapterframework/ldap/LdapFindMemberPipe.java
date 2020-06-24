/*
   Copyright 2016, 2019, 2020 Nationale-Nederlanden

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

import java.util.Hashtable;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.ldap.InitialLdapContext;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.webcontrol.DummySSLSocketFactory;

/**
 * Pipe that checks if a specified dn exists as 'member' in another specified dn
 * in LDAP.
 * 
 * @author Peter Leeuwenburgh
 */
public class LdapFindMemberPipe extends LdapQueryPipeBase {
	private String dnSearchIn;
	private String dnFind;
	private boolean recursiveSearch = true;


	@Override
	public PipeRunResult doPipeWithException(Message message, IPipeLineSession session) throws PipeRunException {
		String dnSearchIn_work;
		String dnFind_work;
		ParameterValueList pvl = null;
		if (getParameterList() != null) {
			try {
				pvl = getParameterList().getValues(message, session);
			} catch (ParameterException e) {
				throw new PipeRunException(this, getLogPrefix(session) + "exception on extracting parameters", e);
			}
		}
		dnSearchIn_work = getParameterValue(pvl, "dnSearchIn");
		if (dnSearchIn_work == null) {
			dnSearchIn_work = getDnSearchIn();
		}
		dnFind_work = getParameterValue(pvl, "dnFind");
		if (dnFind_work == null) {
			dnFind_work = getDnFind();
		}

		boolean found = false;
		if (StringUtils.isNotEmpty(dnSearchIn_work)
				&& StringUtils.isNotEmpty(dnFind_work)) {
			try {
				found = findMember(getHost(), getPort(), dnSearchIn_work, isUseSsl(), dnFind_work, isRecursiveSearch());
			} catch (NamingException e) {
				throw new PipeRunException(this, getLogPrefix(session) + "exception on ldap lookup", e);
			}
		}

		if (!found) {
			String msg = getLogPrefix(session) + "dn [" + dnFind_work + "] not found as member in url [" + retrieveUrl(getHost(), getPort(), dnSearchIn_work, isUseSsl()) + "]";
			if (notFoundForward == null) {
				throw new PipeRunException(this, msg);
			} else {
				log.info(msg);
				return new PipeRunResult(notFoundForward, message);
			}
		}
		return new PipeRunResult(getForward(), message);
	}

	private boolean findMember(String host, int port, String dnSearchIn, boolean useSsl, String dnFind, boolean recursiveSearch) throws NamingException {
		Hashtable<String,Object> env = new Hashtable<String,Object>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		String provUrl = retrieveUrl(host, port, dnSearchIn, useSsl);
		env.put(Context.PROVIDER_URL, provUrl);
		if (StringUtils.isNotEmpty(cf.getUsername())) {
			env.put(Context.SECURITY_AUTHENTICATION, "simple");
			env.put(Context.SECURITY_PRINCIPAL, cf.getUsername());
			env.put(Context.SECURITY_CREDENTIALS, cf.getPassword());
		} else {
			env.put(Context.SECURITY_AUTHENTICATION, "none");
		}
		DirContext ctx = null;
		try {
			try {
				ctx = new InitialDirContext(env);
			} catch (CommunicationException e) {
				log.info("Cannot create constructor for DirContext ["+ e.getMessage() + "], will try again with dummy SocketFactory",e);
				env.put("java.naming.ldap.factory.socket", DummySSLSocketFactory.class.getName());
				ctx = new InitialLdapContext(env, null);
			}
			Attribute attrs = ctx.getAttributes("").get("member");
			if (attrs != null) {
				boolean found = false;
				for (int i = 0; i < attrs.size() && !found; i++) {
					String dnFound = (String) attrs.get(i);
					if (dnFound.equalsIgnoreCase(dnFind)) {
						found = true;
					} else {
						if (recursiveSearch) {
							found = findMember(host, port, dnFound, useSsl,
									dnFind, recursiveSearch);
						}
					}
				}
				return found;
			}
		} finally {
			if (ctx != null) {
				try {
					ctx.close();
				} catch (NamingException e) {
					log.warn("Exception closing DirContext", e);
				}
			}
		}
		return false;
	}

	@IbisDoc({"1", "The dn of the group to search in when the parameter dnSearchIn is not set", ""})
	public void setDnSearchIn(String string) {
		dnSearchIn = string;
	}
	public String getDnSearchIn() {
		return dnSearchIn;
	}

	@IbisDoc({"2", "The dn of the member to search for when the parameter dnFind is not set", ""})
	public void setDnFind(String string) {
		dnFind = string;
	}
	public String getDnFind() {
		return dnFind;
	}

	@IbisDoc({"3", "when <code>true</code>, the member attribute is also searched in all the found members", "true"})
	public void setRecursiveSearch(boolean b) {
		recursiveSearch = b;
	}
	public boolean isRecursiveSearch() {
		return recursiveSearch;
	}

}
