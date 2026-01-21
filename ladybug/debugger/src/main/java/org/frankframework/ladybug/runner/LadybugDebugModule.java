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

import java.util.List;

import lombok.extern.log4j.Log4j2;

import org.frankframework.components.Module;
import org.frankframework.util.AppConstants;

@Log4j2
public class LadybugDebugModule implements Module {

	@Override
	public List<String> getSpringConfigurationFiles() {
		AppConstants properties = AppConstants.getInstance();
		String customXmlfile = properties.getProperty("ibistesttool.custom", "");
		String debuggerAdvice = "springIbisDebuggerAdvice%s.xml".formatted(customXmlfile);
		log.debug("using Ladybug debugger file [{}]", debuggerAdvice);
		return List.of(debuggerAdvice);
	}
}
