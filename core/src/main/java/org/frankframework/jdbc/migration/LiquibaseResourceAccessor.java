/*
   Copyright 2021-2023 WeAreFrank!

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
package org.frankframework.jdbc.migration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.resource.AbstractResource;
import liquibase.resource.ResourceAccessor;
import org.frankframework.core.Resource;
import org.frankframework.functional.ThrowingSupplier;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.LogUtil;

/**
 * Liquibase ResourceAccessor that can return a resource supplied at construction time.
 * This allows to generate scripts from an uploaded changelog file.
 *
 * @author Niels Meijer
 */
public class LiquibaseResourceAccessor implements ResourceAccessor {

	private final Resource resource;

	public LiquibaseResourceAccessor(Resource resource) {
		this.resource = resource;
	}

	/**
	 * This method is primarily used by Liquibase to get the xsd
	 * (/www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.3.xsd) to validate against. (the path literally contains www.liquibase.org !)
	 * Since the XSD is in the jar file and we do not want to override it, simply return null.
	 * Then the default XSD in the Liquibase jar will be used.
	 */
	@Override
	public List<liquibase.resource.Resource> search(String path, boolean recursive) throws IOException {
		if (path.equals(resource.getSystemId())) {
			return asResourceList(path, null, resource::openStream);
		}
		URL url = ClassLoaderUtils.getResourceURL(resource, path);
		if (url==null) {
			return Collections.emptyList();
		}
		try {
			return asResourceList(path, url.toURI(), url::openStream);
		} catch (URISyntaxException e) {
			LogUtil.getLogger(this).warn("unable to convert resource url [{}]", url, e);
		}
		return Collections.emptyList();
	}

	protected List<liquibase.resource.Resource> asResourceList(String path, URI uri, ThrowingSupplier<InputStream,IOException> inputStreamSupplier) {
		return Collections.singletonList(new AbstractResource(path, uri) {

			@Override
			public InputStream openInputStream() throws IOException {
				return inputStreamSupplier.get();
			}

			@Override
			public boolean exists() {
				return true;
			}

			@Override
			public liquibase.resource.Resource resolve(String other) {
				try {
					return get(resolvePath(other));
				} catch (IOException e) {
					throw new UnexpectedLiquibaseException(e);
				}
			}

			@Override
			public liquibase.resource.Resource resolveSibling(String other) {
				try {
					return get(resolveSiblingPath(other));
				} catch (IOException e) {
					throw new UnexpectedLiquibaseException(e);
				}
			}

		});
	}


	@Override
	public List<liquibase.resource.Resource> getAll(String path) throws IOException {
		return search(path, false);
	}

	@Override
	public List<String> describeLocations() {
		return Collections.singletonList(resource.toString());
	}

	@Override
	public void close() throws Exception {
		// not necessary to close anything
	}

}
