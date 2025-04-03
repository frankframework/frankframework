/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.lifecycle;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaMetadataKeys;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.configuration.classloaders.ClassLoaderBase;
import nl.nn.adapterframework.configuration.classloaders.IConfigurationClassLoader;
import nl.nn.adapterframework.http.HttpServletBase;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassLoaderUtils;
import nl.nn.adapterframework.util.LogUtil;

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
public class WebContentServlet extends HttpServletBase {

	private static final long serialVersionUID = 1L;

	public static final String WEBCONTENT = "webcontent";

	private final transient Logger log = LogUtil.getLogger(this);
	private static final String SERVLET_PATH = "/webcontent/";
	private static final String WELCOME_FILE = "index.html";
	private static final String CONFIGURATION_KEY = WebContentServlet.class.getCanonicalName() + ".configuration";
	private final Map<String, MimeType> supportedMediaTypes = new HashMap<>();
	private final Map<URL, MimeType> computedMediaTypes = new WeakHashMap<>();
	private final boolean isDtapStageLoc = "LOC".equalsIgnoreCase(AppConstants.getInstance().getProperty("dtap.stage"));
	private Detector detector = null;

	@Override
	public void init() throws ServletException {
		super.init();

		try {
			loadMediaTypes();
		} catch (IOException e) {
			throw new ServletException(e);
		}

		try {
			TikaConfig tika = new TikaConfig();
			detector = tika.getDetector();
		} catch (TikaException | IOException e) {
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
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path = req.getPathInfo();
		if(path == null) {
			resp.sendRedirect(req.getContextPath() + SERVLET_PATH);
			return;
		} else if(path.equals("/")) {
			if(isDtapStageLoc) {
				listDirectory(resp);
				resp.flushBuffer();
			} else {
				resp.sendError(404, "resource not found");
			}
			return;
		}

		URL resource = findResource(req);

		if(resource == null) {
			resp.sendError(404, "resource not found");
			return;
		}

		MimeType mimeType = determineMimeType(resource);
		if(mimeType != null) {
			log.debug("found MimeType [{}] for resource [{}]", mimeType, resource);
			resp.setContentType(mimeType.toString());
		}

		try(InputStream in = resource.openStream()) {
			IOUtils.copy(in, resp.getOutputStream());
		} catch (IOException e) {
			log.warn("error reading or writing resource to servlet", e);
			resp.sendError(500, e.getMessage());
			return;
		}

		resp.flushBuffer();
	}

	@Override
	protected long getLastModified(HttpServletRequest req) {
		String path = req.getPathInfo();
		if(StringUtils.isNotEmpty(path) && !path.equals("/") && findResource(req) != null) {
			String configurationName = (String) req.getAttribute(CONFIGURATION_KEY);
			return findConfiguration(configurationName).getStartupDate();
		}

		return -1;
	}

	private MimeType determineMimeType(URL resource) {
		String extension = FilenameUtils.getExtension(resource.toString());
		log.debug("trying to lookup MimeType for extension [{}]", extension);
		MimeType type = supportedMediaTypes.get(extension);
		if(type == null) {
			log.info("no default MimeType mapping found for extension [{}]", extension);
			return computedMediaTypes.computeIfAbsent(resource, this::computeMimeType);
		}
		return type;
	}

	/**
	 * Tries to determine the MimeType by reading the file's magic (first 16k bytes)
	 * @return the computed MimeType or APPLICATION/OCTET_STREAM
	 */
	private MimeType computeMimeType(URL resource) {
		log.debug("computing MimeType for resource [{}]", resource);
		Metadata metadata = new Metadata();
		String name = FilenameUtils.getExtension(resource.toString());
		metadata.set(TikaMetadataKeys.RESOURCE_NAME_KEY, name);
		try(InputStream in = resource.openStream()) {
			MimeType type = MimeType.valueOf(detector.detect(TikaInputStream.get(in), metadata).toString());
			if(!type.getSubtype().contains("x-tika")) {
				return type;
			}
		} catch (IOException e) {
			log.warn("unable to compute MimeType from URL [{}]", resource, e);
		}
		return MediaType.APPLICATION_OCTET_STREAM;
	}

	/**
	 * Should fail fast, always return null / HTTP 404.
	 */
	private URL findResource(HttpServletRequest req) {
		String normalizedPath = FilenameUtils.normalize(req.getPathInfo(), true);
		if(normalizedPath.startsWith("/")) {
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
		if(StringUtils.isEmpty(resource) || resource.equals("/")) {
			log.debug("unable to determine resource from path [{}] returning welcome file [{}]", normalizedPath, WELCOME_FILE);
			resource = WELCOME_FILE;
		}

		ClassLoaderBase classLoader = (ClassLoaderBase) configuration.getClassLoader();
		if(classLoader == null) {
			log.warn("configuration [{}] has no ClassLoader", configuration);
			return null;
		}
		return classLoader.getResource(WEBCONTENT + "/" + resource, false);
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
		ClassLoaderBase classLoader = (ClassLoaderBase) configuration.getClassLoader();
		boolean isWebContentFolderPresent = classLoader != null && classLoader.getLocalResource(WEBCONTENT) != null;
		if (isWebContentFolderPresent) {
			log.info("found configuration [{}] with [{}}] folder", configuration, WEBCONTENT);
			response.getWriter().append("<a href=\"" + configuration.getName() + "\">" + configuration.getName() + "</a>");
		}
	}

	/**
	 * Should be fetched runtime, the IbisContext is not available until after the IbisApplicationServlet has initialized
	 */
	private IbisManager getIbisManager() {
		IbisContext ibisContext = IbisApplicationServlet.getIbisContext(getServletContext());
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
