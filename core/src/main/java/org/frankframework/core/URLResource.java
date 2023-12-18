/*
   Copyright 2021 WeAreFrank!

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
package org.frankframework.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Reference to a resource, within the Configuration scope. Can be accessed multiple times.
 *
 * @author Niels Meijer
 *
 */
public class URLResource extends Resource {

	private final URL url;
	private final String systemId;

	protected URLResource(IScopeProvider scopeProvider, URL url, String systemId) {
		super(scopeProvider);

		this.url = url;
		this.systemId = systemId;
	}

	@Override
	public InputStream openStream() throws IOException {
		return url.openStream();
	}

	@Override
	public String getSystemId() {
		return systemId;
	}

	@Override
	public String toString() {
		return "URLResource url ["+url+"] systemId ["+systemId+"] scope ["+scopeProvider+"]";
	}
}
