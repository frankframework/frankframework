/*
   Copyright 2025-2026 WeAreFrank!

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
package org.frankframework.ladybug.runner;

import java.net.URL;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import org.frankframework.components.Module;
import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;

public class LadybugDebugModule implements Module {
	private static final String DEFAULT_ADVICE_FILE = "springIbisDebuggerAdvice.xml";
	private static final Logger APPLICATION_LOG = LogUtil.getLogger("APPLICATION");

	@Override
	public List<String> getSpringConfigurationFiles() {
		AppConstants properties = AppConstants.getInstance();
		String customization = properties.getProperty("ibistesttool.custom", "");
		if (StringUtils.isNotBlank(customization)) {
			APPLICATION_LOG.info("Found ladybug customization [{}]", customization);

			String debuggerAdvice = "springIbisDebuggerAdvice%s.xml".formatted(customization);

			String file = findFile(debuggerAdvice);
			if (file != null) {
				return List.of(file);
			}

			APPLICATION_LOG.info("Unable to locate Ladybug debugger customization file [{}] fallback to default file [{}]", debuggerAdvice, DEFAULT_ADVICE_FILE);
		}

		String defaultAdviceFile = findFile(DEFAULT_ADVICE_FILE);
		if (StringUtils.isBlank(defaultAdviceFile)) {
			throw new IllegalStateException("unable to start Ladybug, configFile ["+DEFAULT_ADVICE_FILE+"] not found");
		}
		return List.of(DEFAULT_ADVICE_FILE);
	}

	@Nullable
	static String findFile(String file) {
		URL fileURL = LadybugDebugModule.class.getClassLoader().getResource(file);
		if(fileURL == null) {
			return null;
		}

		APPLICATION_LOG.info("Loading TestTool configuration [{}]", file);
		return file;
	}
}
