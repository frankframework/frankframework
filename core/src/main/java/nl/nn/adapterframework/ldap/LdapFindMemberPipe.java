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

import java.util.Set;

import javax.naming.NamingException;
import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;

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
				Set<String> members;
				
				if (isRecursiveSearch()) {
					members = ldapClient.searchRecursivelyViaAttributes(dnSearchIn, getBaseDN(), "member", dnFind);
				} else {
					members = ldapClient.searchObjectForMultiValuedAttribute(dnSearchIn, getBaseDN(), "member");
				}
				
				found = members.contains(dnFind_work);
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

	public void setDnSearchIn(String string) {
		dnSearchIn = string;
	}
	public String getDnSearchIn() {
		return dnSearchIn;
	}

	public void setDnFind(String string) {
		dnFind = string;
	}
	public String getDnFind() {
		return dnFind;
	}

	public void setRecursiveSearch(boolean b) {
		recursiveSearch = b;
	}
	public boolean isRecursiveSearch() {
		return recursiveSearch;
	}

}
