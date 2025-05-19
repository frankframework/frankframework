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
package org.frankframework.larva;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

import lombok.Getter;

import org.frankframework.core.SenderException;
import org.frankframework.http.AbstractHttpServlet;
import org.frankframework.lifecycle.IbisInitializer;
import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StreamUtil;

@IbisInitializer
public class LarvaServlet extends AbstractHttpServlet {
	private static final URL INDEX_TEMPLATE = getResource("/index.html.template");
	private static final String SERVLET_PATH = "/iaf/larva/";
	private final transient Logger log = LogUtil.getLogger(this);

	private final transient boolean allowSave = AppConstants.getInstance().getBoolean("servlet.LarvaServlet.allowFileSave", false);

	private enum Assets {
		STYLESHEET("/assets/style.css", "text/css"),
		LIB("/assets/lib.js", "text/javascript");

		private final @Getter String contentType;
		private final URL url;
		private final String resource;

		Assets(String resource, String contentType) {
			URL resourceURL = getResource(resource);
			if(resourceURL == null) {
				throw new IllegalStateException("unable to find asset");
			}

			this.url = resourceURL;
			this.resource = resource;
			this.contentType = contentType;
		}

		static Assets findAsset(String resource) {
			for(Assets asset : values()) {
				if(asset.resource.equals(resource)) {
					return asset;
				}
			}
			return null;
		}

		InputStream openStream() throws IOException {
			return url.openStream();
		}
	}

	private static URL getResource(String resource) {
		return LarvaServlet.class.getResource(resource);
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if(req.getPathInfo() == null) {
			resp.sendRedirect(req.getContextPath() + SERVLET_PATH + "index.jsp"); // Avoid that WebSphere removes the slash at the end of the url (causing an endless loop) by explicitly adding the welcome resource
			return;
		}

		req.setCharacterEncoding(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
		super.service(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String path = req.getPathInfo();

		if("/".equals(path) || "/index.jsp".equalsIgnoreCase(path)) {
			handleIndex(req, resp);
			return;
		} else {
			Assets asset = Assets.findAsset(path);
			if (asset != null) {
				handleAsset(asset, resp);
				return;
			}
		}
		resp.sendError(404, "resource not found");
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String path = req.getPathInfo();
		if("/".equals(path) || "/index.jsp".equalsIgnoreCase(path)) {
			handleIndex(req, resp);
			return;
		} else if (allowSave && "/saveResultToFile.jsp".equals(path)) {
			handleSaveResult(req, resp);
			return;
		}

		resp.sendError(404, "resource not found");
	}

	private void handleAsset(Assets asset, HttpServletResponse resp) throws IOException {
		resp.setContentType(asset.getContentType());

		try(InputStream in = asset.openStream()) {
			IOUtils.copy(in, resp.getOutputStream());
		} catch (IOException e) {
			log.warn("error reading or writing resource to servlet", e);
			resp.sendError(500, e.getMessage());
			return;
		}

		resp.flushBuffer();
	}

	private void handleIndex(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		Writer writer = resp.getWriter();
		resp.setContentType("text/html");
		writer.append(getTemplate("Larva Test Tool"));

		try {
			LarvaTool.runScenarios(getServletContext(), req, writer);
		} catch (Exception e) {
			log.warn("error running scenarios", e);
		}

		writer.append("</body></html>");
		resp.flushBuffer();
	}

	private String getTemplate(String title) throws IOException {
		String content = StreamUtil.resourceToString(INDEX_TEMPLATE);
		return content.replace("{{title}}", title);
	}

	private void handleSaveResult(HttpServletRequest request, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/html");
		Writer writer = resp.getWriter();
		if(request.getParameter("cmd") != null) {
			if ("indentWindiff".equals(request.getParameter("cmd"))) {
				writer.append(getTemplate("Comparing result"));
			} else {
				writer.append(getTemplate("Save actual result"));
			}
		}

		if (request.getParameter("init") != null) {
			writer.append("<p>Waiting for data...</p>");
		} else if (request.getParameter("expectedFileName") == null) {
			writer.append("<p>No file name received!</p>");
			writer.append("<p>In case you use Tomcat and large messages this might be caused by maxPostSize which is set to 2097152 (2 megabytes) by default. Add maxPostSize to the Connector element in server.xml with a larger value or 0.</p>");
		} else {
			if ("indentWindiff".equals(request.getParameter("cmd"))) {
				writer.append("<p>Comparing actual result with expected result...</p>");
				writer.flush();
				try {
					LarvaTool larvaTool = LarvaTool.createInstance(getServletContext());
					larvaTool.windiff(request.getParameter("expectedFileName"), request.getParameter("expectedBox"), request.getParameter("resultBox"));
				} catch (SenderException e) {
					log.warn("unable to execute windiff command", e);
					resp.sendError(500, "unable to save file");
				} catch (IOException e) {
					log.warn("unable to write tempFile", e);
					resp.sendError(500, "unable to write tempFile");
				}
			} else {
				writer.append("<p>Overwriting expected result with actual result...</p>");
				writer.flush();
				LarvaTool.writeFile(request.getParameter("expectedFileName"), request.getParameter("resultBox"));
			}
			writer.append("<p>Done!</p>");
			writer.append("<script>window.close();</script>");
		}
	}

	@Override
	public String getUrlMapping() {
		return SERVLET_PATH + "*";
	}

	@Override
	public String[] getAccessGrantingRoles() {
		return new String[] {"IbisTester"};
	}
}
