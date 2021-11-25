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
package nl.nn.adapterframework.jdbc.migration;

import java.io.IOException;
import java.io.InputStream;
import java.util.SortedSet;

import liquibase.resource.InputStreamList;
import liquibase.resource.ResourceAccessor;

/**
 * @author alisihab
 *
 */
public class StreamResourceAccessor implements ResourceAccessor {

	private InputStream stream;
	
	public StreamResourceAccessor(InputStream stream) throws IOException {
		super();
		this.stream = stream;
	}

	@Override
	public InputStreamList openStreams(String relativeTo, String streamPath) throws IOException {
		return null; //Used to find the xsd to validate against.
	}

	@Override
	public InputStream openStream(String relativeTo, String path) throws IOException {
		if(path.endsWith(".xsd")) {
			return null;
		}

		return stream;
	}

	@Override
	public SortedSet<String> list(String relativeTo, String path, boolean recursive, boolean includeFiles, boolean includeDirectories) throws IOException {
		return null;
	}

	@Override
	public SortedSet<String> describeLocations() {
		return null;
	}
}
