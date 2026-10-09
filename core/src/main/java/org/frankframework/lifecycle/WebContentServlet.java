/*
   Copyright 2022-2026 WeAreFrank!

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
package org.frankframework.lifecycle;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.tika.io.TikaInputStream;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.IbisContext;
import org.frankframework.configuration.IbisManager;
import org.frankframework.configuration.classloaders.AbstractClassLoader;
import org.frankframework.configuration.classloaders.IConfigurationClassLoader;
import org.frankframework.http.AbstractHttpServlet;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.MessageUtils;

/**
 * This servlet allows the use of WebContent served from {@link Configuration Configurations}.
 * The configuration must have a folder called <code>webcontent</code> for this to work. The Configuration
 * may consist of adapters and webcontent or standalone webcontent. This works for all {@link IConfigurationClassLoader ClassLoaders}.
 *
 * Just like other {@link DynamicRegistration.Servlet servlets} this servlet may be configured through the {@link ServletManager}.
 *
 * @author Niels Meijer
 */
@IbisInitializer
public class WebContentServlet extends AbstractHttpServlet {

	@Serial
	private static final long serialVersionUID = 1L;
	public static final String WEBCONTENT = "webcontent";
	private final transient Logger log = LogUtil.getLogger(this);
	private static final String SERVLET_PATH = "/webcontent/";
	private static final String WELCOME_FILE = "index.html";
	private static final String CONFIGURATION_KEY = WebContentServlet.class.getCanonicalName() + ".configuration";
	private final Map<String, MimeType> supportedMediaTypes = new HashMap<>();
	private final transient Map<String, MimeType> computedMediaTypes = new WeakHashMap<>();
	private final boolean isDtapStageLoc = "LOC".equalsIgnoreCase(AppConstants.getInstance().getProperty("dtap.stage"));

	@Override
	public void init() throws ServletException {
		super.init();

		try {
			loadMediaTypes();
		} catch (IOException e) {
			throw new ServletException(e);
		}
	}

	private void loadMediaTypes() throws IOException {
		URL mappingFile = ClassLoaderUtils.getResourceURL("/MediaTypeMapping.properties");
		if(mappingFile == null) {
			throw new IOException("unable to find mappingFile");
		}

		try(InputStream stream = mappingFile.openStream()) {
			Properties properties = new Properties();
			properties.load(stream);
			for(String key : properties.stringPropertyNames()) {
				String value = properties.getProperty(key);
				supportedMediaTypes.put(key, MediaType.valueOf(value));
			}
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String path = req.getPathInfo();
		if (path == null) {
			resp.sendRedirect(req.getContextPath() + SERVLET_PATH);
			return;
		} else if("/".equals(path)) {
			if(isDtapStageLoc) {
				listDirectory(resp);
				resp.flushBuffer();
			} else {
				resp.sendError(HttpStatus.NOT_FOUND.value(), "resource not found");
			}
			return;
		}

		URL resource = findResource(req);

		if (resource == null) {
			if (!path.endsWith("/") && findResource(req, path + "/") != null) {
				// Folder with a welcome file, redirect so relative links in the welcome file resolve against the folder.
				resp.sendRedirect(getFolderRedirectUrl(req));
				return;
			}
			resp.sendError(HttpStatus.NOT_FOUND.value(), "resource not found");
			return;
		}

		MimeType mimeType = determineMimeType(resource);
		if (mimeType != null) {
			log.debug("found MimeType [{}] for resource [{}]", mimeType, resource);
			resp.setContentType(mimeType.toString());
		}

		try (InputStream in = resource.openStream()) {
			IOUtils.copy(in, resp.getOutputStream());
		} catch (IOException e) {
			log.warn("error reading or writing resource to servlet", e);
			resp.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
			return;
		}

		resp.flushBuffer();
	}

	@Override
	protected long getLastModified(HttpServletRequest req) {
		String path = req.getPathInfo();
		if (StringUtils.isNotEmpty(path) && !"/".equals(path) && findResource(req) != null) {
			String configurationName = (String) req.getAttribute(CONFIGURATION_KEY);
			return findConfiguration(configurationName).getStartupDate();
		}

		return -1;
	}

	private MimeType determineMimeType(URL resource) {
		String extension = FilenameUtils.getExtension(resource.getPath());
		log.debug("trying to lookup MimeType for extension [{}]", extension);
		MimeType type = supportedMediaTypes.get(extension);
		if (type == null) {
			log.info("no default MimeType mapping found for extension [{}]", extension);
			return computedMediaTypes.computeIfAbsent(resource.toExternalForm(), this::computeMimeType);
		}
		return type;
	}

	/**
	 * Tries to determine the MimeType by reading the file's magic (first 16k bytes)
	 * @return the computed MimeType or APPLICATION/OCTET_STREAM
	 */
	private MimeType computeMimeType(String resourcePath) {
		URL resource;
		try {
			resource = URI.create(resourcePath).toURL();
		} catch (MalformedURLException e) {
			log.warn("Could not create URL from path [{}]", resourcePath);
			return MediaType.APPLICATION_OCTET_STREAM;
		}
		log.debug("computing MimeType for resource [{}]", resource);
		String name = FilenameUtils.getExtension(resource.toString());
		try (InputStream in = resource.openStream(); TikaInputStream tis = TikaInputStream.get(in)) {
			MimeType type = MimeType.valueOf(MessageUtils.TIKA.detect(tis, name));
			if (!type.getSubtype().contains("x-tika")) {
				return type;
			}
		} catch (IOException e) {
			log.warn("unable to compute MimeType from URL [{}]", resource, e);
		}
		return MediaType.APPLICATION_OCTET_STREAM;
	}

	/**
	 * Relative to the current URL, so the redirect can only ever point at the folder itself:
	 * the raw request URI may start with a double slash, which a browser would read as a host.
	 * The leading {@code ./} keeps a folder name containing a colon from being read as a scheme.
	 */
	private String getFolderRedirectUrl(HttpServletRequest req) {
		String requestUri = req.getRequestURI();
		String location = "./" + requestUri.substring(requestUri.lastIndexOf('/') + 1) + "/";
		String queryString = req.getQueryString();
		if (StringUtils.isNotEmpty(queryString)) {
			location += "?" + queryString;
		}
		return location;
	}

	/**
	 * Should fail fast, always return null / HTTP 404.
	 */
	private @Nullable URL findResource(HttpServletRequest req) {
		return findResource(req, req.getPathInfo());
	}

	/**
	 * Finds the resource for the given path, which starts with the configuration name.
	 * A folder resolves to its welcome file. A folder itself is never returned, its contents must not be listed.
	 */
	private @Nullable URL findResource(HttpServletRequest req, String path) {
		String normalizedPath = FilenameUtils.normalize(path, true);
		if (normalizedPath == null) {
			return null;
		}
		if (normalizedPath.startsWith("/")) {
			normalizedPath = normalizedPath.substring(1);
		}
		String[] split = normalizedPath.split("/");
		String configurationName = split[0];
		Configuration configuration = findConfiguration(configurationName);
		if(configuration == null) {
			log.debug("unable to find configuration [{}] derived from path [{}]", configurationName, normalizedPath);
			return null;
		}
		req.setAttribute(CONFIGURATION_KEY, configurationName);

		String resource = normalizedPath.substring(configurationName.length());
		if(StringUtils.isEmpty(resource) || "/".equals(resource)) {
			log.debug("unable to determine resource from path [{}] returning welcome file [{}]", normalizedPath, WELCOME_FILE);
			resource = WELCOME_FILE;
		} else if (resource.endsWith("/")) {
			log.debug("resource [{}] is a folder, returning welcome file [{}]", resource, WELCOME_FILE);
			resource = resource + WELCOME_FILE;
		}

		AbstractClassLoader classLoader = (AbstractClassLoader) configuration.getClassLoader();
		if(classLoader == null) {
			log.warn("configuration [{}] has no ClassLoader", configuration);
			return null;
		}
		URL url = classLoader.getResource(WEBCONTENT + "/" + resource, false);
		if (url != null && isDirectory(url)) {
			log.debug("resource [{}] is a directory, not serving it", url);
			return null;
		}
		return url;
	}

	private boolean isDirectory(URL url) {
		if (url.getPath().endsWith("/")) {
			return true;
		}

		try {
			if ("file".equals(url.getProtocol())) {
				return Files.isDirectory(Path.of(url.toURI()));
			}
		} catch (URISyntaxException | IllegalArgumentException e) {
			log.warn("unable to determine if resource [{}] is a directory, not serving it", url, e);
			return true;
		}
		return false;
	}

	private Configuration findConfiguration(String configurationName) {
		return getIbisManager().getConfiguration(configurationName);
	}

	private void listDirectory(HttpServletResponse response) throws IOException {
		// Make sure we serve text/html
		response.setContentType("text/html");

		// Wrap the output with correct tags
		response.getWriter().append("<html><body>");

		for(Configuration configuration : getIbisManager().getConfigurations()) {
			getWebContentForConfiguration(response, configuration);
		}

		// And close the tags again.
		response.getWriter().append("</body></html>");
	}

	void getWebContentForConfiguration(HttpServletResponse response, Configuration configuration) throws IOException {
		AbstractClassLoader classLoader = (AbstractClassLoader) configuration.getClassLoader();
		boolean isWebContentFolderPresent = classLoader != null && classLoader.getResource(WEBCONTENT, false) != null;
		if(isWebContentFolderPresent) {
			log.info("found configuration [{}] with [{}}] folder", configuration, WEBCONTENT);
			response.getWriter().append("<a href=\""+ configuration.getName()+"\">"+ configuration.getName()+"</a>");
		}
	}

	/**
	 * Should be fetched runtime, the IbisContext is not available until after the FrankApplicationInitializer has initialized
	 */
	IbisManager getIbisManager() {
		IbisContext ibisContext = FrankApplicationInitializer.getIbisContext(getServletContext());
		return ibisContext.getIbisManager();
	}

	@Override
	public String getUrlMapping() {
		return SERVLET_PATH + "*";
	}

	@Override
	public String[] getAccessGrantingRoles() {
		return ALL_IBIS_ROLES;
	}
}
