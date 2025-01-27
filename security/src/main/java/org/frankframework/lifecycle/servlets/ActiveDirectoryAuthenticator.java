/*
   Copyright 2022-2023 WeAreFrank!

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
package org.frankframework.lifecycle.servlets;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.security.web.SecurityFilterChain;

import lombok.Setter;

import org.frankframework.util.ClassUtils;

public class ActiveDirectoryAuthenticator extends AbstractServletAuthenticator {

	private @Setter String domainName = null;

	/** LDAP server endpoint, eg: ldap://10.1.2.3 */
	private @Setter String url;

	/** Domain root DN, eg: DC=company,DC=org */
	private @Setter String baseDn;
	private @Setter boolean followReferrals = true;

	/** defaults to (&(objectClass=user)(userPrincipalName={0})) */
	private @Setter String searchFilter = null;

	private @Setter String roleMappingFile = "ldap-role-mapping.properties";
	private URL roleMappingURL = null;

	private void configure() throws FileNotFoundException {
		setDefaultValues();

		if(StringUtils.isEmpty(url)) {
			throw new IllegalArgumentException("url may not be empty");
		}

		roleMappingURL = ClassUtils.getResourceURL(roleMappingFile);
		if(roleMappingURL == null) {
			throw new FileNotFoundException("unable to find LDAP role-mapping file ["+roleMappingFile+"]");
		}
		log.info("found rolemapping file [{}]", roleMappingURL);
	}

	/** Set default values for legacy properties to ease application upgrades, these values can be overwritten when the appropriate setter is called. */
	private void setDefaultValues() {
		Environment env = getApplicationContext().getEnvironment();
		String legacyURL = env.getProperty("ldap.auth.url");
		if(StringUtils.isEmpty(url) && StringUtils.isNotBlank(legacyURL)) {
			this.url = legacyURL;
		}
		String legacyBaseDn = env.getProperty("ldap.auth.user.base");
		if(StringUtils.isEmpty(baseDn) && StringUtils.isNotBlank(legacyBaseDn)) {
			this.baseDn = legacyBaseDn;
		}
	}

	@Override
	public SecurityFilterChain configure(HttpSecurity http) throws Exception {
		configure();

		ActiveDirectoryLdapAuthenticationProvider provider = new ActiveDirectoryLdapAuthenticationProvider(domainName, url, baseDn);
		provider.setConvertSubErrorCodesToExceptions(log.isDebugEnabled());

		if(StringUtils.isNotEmpty(searchFilter)) provider.setSearchFilter(searchFilter);
		Map<String, Object> environment = new HashMap<>();
		if(followReferrals) environment.put(Context.REFERRAL, "follow");
		provider.setContextEnvironmentProperties(environment);

		LdapUserDetailsMapper roleMapper = new LdapUserDetailsMapper();
		roleMapper.setRoleAttributes("memberOf".split(" "));
		roleMapper.setConvertToUpperCase(false);
		roleMapper.setRolePrefix("");
		provider.setUserDetailsContextMapper(roleMapper);

		provider.setAuthoritiesMapper(new AuthorityMapper(roleMappingURL, getSecurityRoles(), getEnvironmentProperties()));

		http.authenticationProvider(provider);
		String realmName = StringUtils.isNotEmpty(domainName) ? domainName : url;
		http.httpBasic(basic -> basic.realmName(realmName));
		return http.build();
	}
}
