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
package org.frankframework.lifecycle.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;

import lombok.extern.log4j.Log4j2;

import org.frankframework.util.StringResolver;

@Log4j2
public class AuthorityMapper implements GrantedAuthoritiesMapper {
	private final Map<GrantedAuthority, String> roleToAuthorityMapping = new HashMap<>();

	public AuthorityMapper(URL roleMappingURL, Set<String> roles, Properties properties) throws IOException {
		Properties roleMappingProperties = new Properties();
		try(InputStream stream = roleMappingURL.openStream()) {
			roleMappingProperties.load(stream);
		} catch (IOException e) {
			throw new IOException("unable to open role-mapping file ["+roleMappingURL+"]", e);
		}

		for(String role : roles) {
			String value = roleMappingProperties.getProperty(role);
			if(StringUtils.isEmpty(value)) {
				log.warn("role [{}] has not been mapped to anything, ignoring this role", role);
				continue;
			}

			String resolvedValue = StringResolver.substVars(value, properties);
			if(StringUtils.isNotEmpty(resolvedValue)) {
				GrantedAuthority grantedAuthority = new SimpleGrantedAuthority("ROLE_"+role);
				roleToAuthorityMapping.put(grantedAuthority, resolvedValue);
				log.info("mapped role [{}] to [{}]", resolvedValue, grantedAuthority);
			}
		}
	}

	@Override
	public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
		List<GrantedAuthority> mappedAuthorities = new ArrayList<>();
		List<String> canonicalRoleNames = authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());

		roleToAuthorityMapping.forEach((authority, key) -> {
			if(canonicalRoleNames.contains(key)) {
				mappedAuthorities.add(authority);
			}
		});

		return mappedAuthorities;
	}
}
