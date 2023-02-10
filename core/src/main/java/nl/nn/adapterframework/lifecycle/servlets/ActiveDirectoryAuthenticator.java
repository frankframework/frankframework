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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.security.web.SecurityFilterChain;

import lombok.Setter;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.StringResolver;

public class ActiveDirectoryAuthenticator extends ServletAuthenticatorBase implements InitializingBean {

	private @Setter String domainName = null;
	/** LDAP server endpoint, eg: ldap://10.1.2.3 */
	private @Setter String url = AppConstants.getInstance().getProperty("ldap.auth.url");
	/** Domain root DN, eg: DC=company,DC=org */
	private @Setter String baseDn = AppConstants.getInstance().getProperty("ldap.auth.user.base");
	private @Setter boolean followReferrals = true;
	/** defaults to (&(objectClass=user)(userPrincipalName={0})) */
	private @Setter String searchFilter = null;

	private @Setter String roleMappingFile = "ldap-role-mapping.properties";
	private URL roleMappingURL = null;

	@Override
	public SecurityFilterChain configure(HttpSecurity http) throws Exception {
		if(StringUtils.isEmpty(url)) {
			throw new IllegalArgumentException("url may not be empty");
		}

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

		provider.setAuthoritiesMapper(new LdapAuthorityMapper(roleMappingURL));

		http.authenticationProvider(provider);
		String realmName = StringUtils.isNotEmpty(domainName) ? domainName : url;
		http.httpBasic().realmName(realmName);
		return http.build();
	}

	public class LdapAuthorityMapper implements GrantedAuthoritiesMapper {
		Map<String, SimpleGrantedAuthority> roleToAuthorityMapping = new HashMap<>();

		public LdapAuthorityMapper(URL roleMappingURL) throws IOException {
			Properties roleMappingProperties = new Properties();
			try(InputStream stream = roleMappingURL.openStream()) {
				roleMappingProperties.load(stream);
			} catch (IOException e) {
				throw new IOException("unable to open LDAP role-mapping file ["+roleMappingFile+"]", e);
			}

			for(String role : getSecurityRoles()) {
				String value = roleMappingProperties.getProperty(role);
				if(StringUtils.isEmpty(value)) {
					log.warn("LDAP role [{}] has not been mapped to anything, ignoring this role", role);
					continue;
				}

				String resolvedValue = StringResolver.substVars(value, AppConstants.getInstance());
				if(StringUtils.isNotEmpty(role) && StringUtils.isNotEmpty(resolvedValue)) {
					roleToAuthorityMapping.put(resolvedValue, new SimpleGrantedAuthority("ROLE_"+role));
				}
			}
		}

		@Override
		public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
			List<GrantedAuthority> mappedAuthorities = new ArrayList<>();
			for(GrantedAuthority grantedAuthority : authorities) {
				String canonicalRoleName = grantedAuthority.getAuthority();
				SimpleGrantedAuthority authority = roleToAuthorityMapping.get(canonicalRoleName);
				if(authority != null) {
					mappedAuthorities.add(authority);
				}
			}
			return mappedAuthorities;
		}
	}

	@Override
	public void afterPropertiesSet() throws FileNotFoundException {
		roleMappingURL = ClassUtils.getResourceURL(roleMappingFile);
		if(roleMappingURL == null) {
			throw new FileNotFoundException("unable to find LDAP role-mapping file ["+roleMappingFile+"]");
		}
		log.info("found rolemapping file [{}]", roleMappingURL);
	}
}
