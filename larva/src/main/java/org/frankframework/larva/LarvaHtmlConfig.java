/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.larva;

import jakarta.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;

/**
 * Larva extra configuration options for HTML output and will parse all the LarvaConfig options from the HTTP Servlet Request.
 */
public class LarvaHtmlConfig extends LarvaConfig {
	public static final String REQUEST_PARAM_LOG_LEVEL = "loglevel";
	public static final String REQUEST_PARAM_AUTO_SCROLL = "autoscroll";
	public static final String REQUEST_PARAM_MULTI_THREADED = "multithreaded";
	public static final String REQUEST_PARAM_SCENARIOS_ROOT_DIR = "scenariosrootdirectory";
	public static final String REQUEST_PARAM_EXECUTE = "execute";
	public static final String REQUEST_PARAM_TIMEOUT = "timeout";
	public static final String REQUEST_PARAM_WAIT_BEFORE_CLEANUP = "waitbeforecleanup";


	private @Getter @Setter boolean autoScroll = true;
	private @Getter @Setter boolean useHtmlBuffer = false;
	private @Getter @Setter boolean useLogBuffer = true;
	private @Getter @Setter String execute;

	public LarvaHtmlConfig() {
		// No-op default constructor
	}

	public LarvaHtmlConfig(HttpServletRequest request) {
		String paramLogLevel = request.getParameter(REQUEST_PARAM_LOG_LEVEL);
		String paramAutoScroll = request.getParameter(REQUEST_PARAM_AUTO_SCROLL);
		String paramMultiThreaded = request.getParameter(REQUEST_PARAM_MULTI_THREADED);
		String paramWaitBeforeCleanUp = request.getParameter(REQUEST_PARAM_WAIT_BEFORE_CLEANUP);
		String paramTimeout = request.getParameter(REQUEST_PARAM_TIMEOUT);

		String paramExecute = request.getParameter(REQUEST_PARAM_EXECUTE);
		String paramScenariosRootDirectory = request.getParameter(REQUEST_PARAM_SCENARIOS_ROOT_DIR);

		setLogLevel(LarvaLogLevel.parse(paramLogLevel, LarvaLogLevel.WRONG_PIPELINE_MESSAGES));
		setAutoScroll(paramAutoScroll != null || paramLogLevel == null);
		setMultiThreaded(paramMultiThreaded != null && paramLogLevel != null);
		if (paramWaitBeforeCleanUp != null) {
			setWaitBeforeCleanup(Integer.parseInt(paramWaitBeforeCleanUp));
		}
		if (paramTimeout != null) {
			setTimeout(Integer.parseInt(paramTimeout));
		}
		setExecute(paramExecute);
		setActiveScenariosDirectory(paramScenariosRootDirectory);
	}
}
