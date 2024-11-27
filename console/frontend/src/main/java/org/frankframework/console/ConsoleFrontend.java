/*
   Copyright 2023 - 2024 WeAreFrank!

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
package org.frankframework.console;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.ResourceUtils;

import org.frankframework.util.ClassUtils;

/**
 * Contains the component annotation so it will be picked up by the core (SpringEnvironmentContext.xml).
 * When using Spring boot (and the FrankConsoleContext.xml), where classpath scanning is enabled,
 * it will directly try to configure this servlet without any configuration.
 * Hence, it should use the RegisterServletEndpoints which uses a ServletConfiguration to initialize the Servlet.
 *
 * @author Niels Meijer
 */
@Log4j2
public class ConsoleFrontend extends HttpServlet implements EnvironmentAware, InitializingBean {

	private static final long serialVersionUID = 1L;
	private static final String WELCOME_FILE = "index.html";
	private static final String DEFAULT_CONSOLE_PATH = "console"; //WebSphere doesn't like the classpath: protocol and resources should not start with a slash?

	private String frontendPath = null;
	@Setter private transient Environment environment;

	@Override
	public void afterPropertiesSet() {
		if(environment != null && Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
			String devFrontendLocation = environment.getProperty("frontend.resources.location");
			if(devFrontendLocation == null) {
				Path rootPath = Paths.get("").toAbsolutePath(); // get default location based on current working directory, in IntelliJ this is the project root.
				devFrontendLocation = rootPath.resolve("console/frontend/target/frontend/").toString(); //Navigate to the target of the frontend module
			}

			frontendPath = ResourceUtils.FILE_URL_PREFIX + FilenameUtils.getFullPath(devFrontendLocation);
			log.info("found frontend path [{}]", frontendPath);
		}

		frontendPath = DEFAULT_CONSOLE_PATH;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		try {
			doGetSafely(req, resp);
		} catch (IOException e) {
			log.error("unable to process request", e);
		}
	}

	/**
	 * @throws IOException only when sendError or sendRedirect cannot process the request.
	 */
	private void doGetSafely(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String path = req.getPathInfo();
		if(StringUtils.isBlank(path)) { //getPathInfo may return null, redirect to {base}+'/' when that happens.
			String fullPath = req.getRequestURI();
			if(!fullPath.endsWith("/")) {
				resp.sendRedirect(req.getContextPath() + req.getServletPath() + "/");
				return;
			} else {
				//WebSphere likes to add a slash to the requestURI but leaves it out of the pathInfo
				if(path == null) {
					path = "/";
				} else {
					resp.sendError(404);
					return;
				}
			}
		}
		if("/".equals(path)) {
			path += WELCOME_FILE;
		}

		URL resource = findResource(path);
		if(resource == null) {
			resp.sendError(404);
			return;
		}

		String mimeType = getServletContext().getMimeType(path);
		resp.setContentType(mimeType != null ? mimeType : "application/octet-stream");

		try(InputStream in = resource.openStream()) {
			IOUtils.copy(in, resp.getOutputStream());

			resp.flushBuffer();
		} catch (IOException e) {
			// Either something has gone wrong, or the request has been cancelled
			log.debug("error reading resource [{}]", resource, e);
		}
	}

	private @Nullable URL findResource(String path) {
		try {
			String normalized = FilenameUtils.normalize(frontendPath+path, true);
			log.trace("trying to find resource [{}]", normalized);
			URL resource = ClassUtils.getResourceURL(normalized);
			if(resource == null) {
				log.debug("unable to locate resource [{}]", path);
			}
			return resource;
		} catch (IOException e) {
			log.warn("exception while locating file [{}]", path, e);
			return null;
		}
	}
}
