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
package org.frankframework.http.authentication;

import org.apache.http.impl.auth.AuthSchemeBase;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.logging.log4j.Logger;

import lombok.Getter;

import org.frankframework.util.ClassUtils;
import org.frankframework.util.LogUtil;

public enum AuthenticationScheme {
	OAUTH(OAuthAuthenticationScheme.class),
	BASIC(BasicScheme.class);

	protected Logger log = LogUtil.getLogger(this);

	private @Getter String schemeName;
	private final Class<? extends AuthSchemeBase> schemeClass;

	AuthenticationScheme(Class<? extends AuthSchemeBase> schemeClass) {
		this.schemeClass = schemeClass;
		schemeName = createScheme().getSchemeName();
	}

	public AuthSchemeBase createScheme() {
		try {
			return ClassUtils.newInstance(schemeClass);
		} catch (ReflectiveOperationException | SecurityException e) {
			log.warn("Cannot Instantiate object from class {}", schemeClass.getName(), e);
			return null;
		}
	}
}
