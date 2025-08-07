/*
   Copyright 2024-2025 WeAreFrank!

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
package org.frankframework.jdbc.datasource;

import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentSkipListMap;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.util.ClassUtils;
import org.frankframework.util.StreamUtil;

/**
 * Class that look up a given resource in the resource file. By default `resources.yml`.
 */
@Log4j2
public class ResourceObjectLocator implements IObjectLocator, InitializingBean {

	private final ObjectCreator objectCreator = new ObjectCreator();
	private final ConcurrentSkipListMap<String, FrankResources> parsedFrankResources = new ConcurrentSkipListMap<>();
	private @Setter String resourceFile = "resources.yml";
	private URL resourceUrl;

	@Override
	public void afterPropertiesSet() throws Exception {
		resourceUrl = ClassUtils.getResourceURL(resourceFile);
		if(resourceUrl == null) {
			log.info("did not find [{}] skipping resource based object lookups", resourceFile);
			return;
		}

		// pre-fill cache with known entries such as JDBC and JMS connections
		getResources("jdbc");
		getResources("jms");
	}

	@Nonnull
	private FrankResources getResources(String prefix) {
		return parsedFrankResources.computeIfAbsent(prefix, this::parseResourcesForPrefix);
	}

	/**
	 * Reads the resource file, skips all root elements except for the given prefix.
	 */
	@Nonnull
	private FrankResources parseResourcesForPrefix(String prefix) {
		try(InputStream is = resourceUrl.openStream(); Reader reader = StreamUtil.getCharsetDetectingInputStreamReader(is)) {
			Representer represent = new Representer(new DumperOptions());
			represent.getPropertyUtils().setSkipMissingProperties(true);
			Yaml yaml = new Yaml(represent, new DumperOptions());
			TypeDescription typeDescription = new TypeDescription(FrankResources.class);

			// Bind the prefix to the resource list.
			typeDescription.substituteProperty(prefix, List.class, null, "setResources", FrankResource.class);
			yaml.addTypeDescription(typeDescription);
			return yaml.loadAs(reader, FrankResources.class);
		} catch (Exception e) {
			throw new IllegalStateException("unable to parse [" + resourceFile + "]", e);
		}
	}

	@Nullable
	private FrankResource findFrankResource(String name) {
		if (StringUtils.isBlank(name)) {
			throw new IllegalStateException("invalid resource defined");
		}
		int slashPos = name.indexOf('/');
		if (slashPos == -1) {
			throw new IllegalStateException("no resource prefix found");
		}
		String prefix = name.substring(0, slashPos);
		String resourceName = name.substring(slashPos + 1);

		return getResources(prefix).findResource(resourceName);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <O> O lookup(String name, Properties environment, Class<O> lookupClass) throws Exception {
		if (resourceUrl == null) {
			log.debug("resource locator is not configured, skip lookup");
			return null;
		}

		FrankResource resource = findFrankResource(name);
		if(resource == null) {
			log.debug("no resource found for name [{}]", name);
			return null; // If the lookup returns null, fail-fast to allow other ResourceFactories to locate the object.
		}

		if (lookupClass == null) {
			return (O) resource;
		}

		return objectCreator.instantiateResource(resource, environment, lookupClass);
	}
}
