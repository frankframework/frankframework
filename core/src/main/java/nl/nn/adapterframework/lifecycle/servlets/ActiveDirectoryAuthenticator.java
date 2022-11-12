/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.lifecycle.servlets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Context;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.security.web.SecurityFilterChain;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

public class ActiveDirectoryAuthenticator implements IAuthenticator {
	private String domainName = null;
	/** LDAP server endpoint, eg: ldap://10.1.2.3 */
	private String url = AppConstants.getInstance().getProperty("ldap.auth.url");
	/** Domain root DN, eg: DC=company,DC=org */
	private String rootDn = AppConstants.getInstance().getProperty("ldap.auth.user.base");
	private boolean followReferrals = true;
	/** defaults to (&(objectClass=user)(userPrincipalName={0})) */
	private String searchFilter = null;

	private String roleMappingProperties = "role-mapping.properties";

	//ldap.auth.observer.base
	//ldap.auth.dataadmin.base
	private String tempMappingIbisTester = AppConstants.getInstance().getProperty("ldap.auth.tester.base");

	@Override
	public SecurityFilterChain configure(ServletConfiguration config, HttpSecurity http) throws Exception {
		ActiveDirectoryLdapAuthenticationProvider provider = new ActiveDirectoryLdapAuthenticationProvider(domainName, url, rootDn);
		provider.setConvertSubErrorCodesToExceptions(LogUtil.getLogger(ServletConfiguration.class).isDebugEnabled());

		if(StringUtils.isNotEmpty(searchFilter)) provider.setSearchFilter(searchFilter);
		Map<String, Object> environment = new HashMap<>();
		if(followReferrals) environment.put(Context.REFERRAL, "follow");
		provider.setContextEnvironmentProperties(environment);

		LdapUserDetailsMapper roleMapper = new LdapUserDetailsMapper();
		roleMapper.setRoleAttributes("memberOf".split(" "));
		roleMapper.setConvertToUpperCase(false);
		roleMapper.setRolePrefix("");
		provider.setUserDetailsContextMapper(roleMapper);

		provider.setAuthoritiesMapper(new LdapAuthorityMapper(roleMappingProperties));
		http.authenticationProvider(provider).httpBasic();
		return http.build();
	}

	public class LdapAuthorityMapper implements GrantedAuthoritiesMapper {
		public LdapAuthorityMapper(String roleMappingFile) {
			// TODO Use actual file
		}

		@Override
		public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
			List<GrantedAuthority> mappedAuthorities = new ArrayList<>();
			for(GrantedAuthority grantedAuthority : authorities) {
				String canonicalRoleName = grantedAuthority.getAuthority();
				if(canonicalRoleName.equals(tempMappingIbisTester)) { //If the user contains this role (s)he will get the ROLE_IbisTester authority
					mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_IbisObserver"));
				}
			}
			return mappedAuthorities;
		}
	}
}
