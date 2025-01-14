/*
   Copyright 2024 WeAreFrank!

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
import java.util.Properties;

import org.springframework.beans.factory.InitializingBean;
import org.yaml.snakeyaml.Yaml;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.util.ClassUtils;
import org.frankframework.util.StreamUtil;

@Log4j2
public class ResourceObjectLocator implements IObjectLocator, InitializingBean {

	private FrankResources resources = null;
	private @Setter String resourceFile = "resources.yml";

	@Override
	public void afterPropertiesSet() throws Exception {
		URL url = ClassUtils.getResourceURL(resourceFile);
		if(url == null) {
			log.info("did not find [{}] skipping resource based object lookups", resourceFile);
			return;
		}

		try(InputStream is = url.openStream(); Reader reader = StreamUtil.getCharsetDetectingInputStreamReader(is)) {
			Yaml yaml = new Yaml();
			resources = yaml.loadAs(reader, FrankResources.class);
		} catch (Exception e) {
			throw new IllegalStateException("unable to parse [" + resourceFile + "]", e);
		}
	}

	@Override
	public <O> O lookup(String name, Properties environment, Class<O> lookupClass) throws Exception {
		if(resources == null) {
			return null;
		}

		return resources.lookup(name, environment, lookupClass);
	}
}
