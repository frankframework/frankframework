/*
   Copyright 2013 Nationale-Nederlanden, 2022-2023 WeAreFrank!

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
package org.frankframework.webcontrol;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import org.frankframework.http.HttpServletBase;
import org.frankframework.lifecycle.IbisInitializer;
import org.frankframework.util.AppConstants;
import org.frankframework.util.FileUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.Misc;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.XmlEncodingUtils;

/**
 * Shows a textfile either as HTML or as Text.
 * <br/>
 * This is a servlet because in JSP you cannot give the
 * content-type depending on a condition.<br/>
 * <p>Parameters:<br/>
 * <ul>
 * <li>fileName: the filename to retrieve</li>
 * <li>returnType: either HTML or TEXT</li>
 * </ul>
 * </p>
 * <p>When the <code>resultType</code> is HTML, the
 * <code>FileViewerServlet.signal</code> properties are looked
 * up from the <code>AppConstants</code> and additional styling is
 * done.</p>
 * <p>
 * <code><pre>
 	FileViewerServlet.signal=ERROR WARN
 	FileViewerServlet.signal.ERROR.pre=&lt;font color=red&gt;
 	FileViewerServlet.signal.ERROR.post=&lt;/font&gt;
 	FileViewerServlet.signal.WARN.pre=&lt;font color=blue&gt;
 	FileViewerServlet.signal.WARN.post=&lt;/font&gt;
 	etc.
 	FileViewerServlet.signal.css=IE4.css
 	</pre></code>
 	The last item specifies which stylesheet to use.
 * @author Johan Verrips
 */
@Deprecated
@IbisInitializer
public class FileViewerServlet extends HttpServletBase {
	protected static Logger log = LogUtil.getLogger(FileViewerServlet.class);

	// key that is looked up to retrieve texts to be signaled
	private static final String fvConfigKey="FileViewerServlet.signal";

	public static final String permissionRules = AppConstants.getInstance().getProperty("FileViewerServlet.permission.rules");

	public static String makeConfiguredReplacements(String input) {
		for (final String signal : AppConstants.getInstance().getListProperty(fvConfigKey)) {
			String pre = AppConstants.getInstance().getProperty(fvConfigKey + "." + signal + ".pre");
			String post = AppConstants.getInstance().getProperty(fvConfigKey + "." + signal + ".post");
			input = StringUtils.replace(input, signal, pre + signal + post);
		}
		return StringUtils.replace(input, "\t", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
	}

	private static void showReaderContents(String filepath, String type, HttpServletResponse response) throws IOException {
		PrintWriter out = response.getWriter();
		if (type==null) {
			response.setContentType("text/html");
			out.println("resultType not specified");
			return;
		}

		if (type.equalsIgnoreCase("html")) { // Assume it's a log file
			response.setContentType("text/html");

			out.println("<html>");
			out.println("<head>");
			out.println("<title>" + AppConstants.getInstance().getProperty("instance.name.lc") + "@" + Misc.getHostname() + " - " + filepath + "</title>");
			out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"iaf/"+AppConstants.getInstance().getProperty(fvConfigKey+".css")+"\">");
			out.println("</head>");
			out.println("<body>");

			try(FileReader reader = new FileReader(filepath); LineNumberReader lnr = new LineNumberReader(reader)) {
				String line;
				while ((line=lnr.readLine())!=null) {
					out.println(makeConfiguredReplacements(XmlEncodingUtils.encodeChars(line))+"<br/>");
				}
			}

			out.println("</body>");
			out.println("</html>");
		} else {
			String filename = FilenameUtils.getName(filepath);
			if (type.equalsIgnoreCase("text")) {
				response.setContentType("text/plain");
				response.setHeader("Content-Disposition","attachment; filename=\""+filename+"\""); //download
			} else if (type.equalsIgnoreCase("xml")) {
				response.setContentType("application/xml");
				response.setHeader("Content-Disposition","inline; filename=\""+filename+"\""); //show in browser
			}
			try(FileReader reader = new FileReader(filepath)) {
				StreamUtil.readerToWriter(reader, out);
			}
		}
		out.close();
	}

	public static void showInputStreamContents(String filepath, String type, HttpServletResponse response) throws IOException {
		try(InputStream inputStream = new DataInputStream(new FileInputStream(filepath))) {
			ServletOutputStream outputStream = response.getOutputStream();
			if (type.equalsIgnoreCase("zip")) {
				response.setContentType("application/zip");
			} else {
				response.setContentType("application/octet-stream");
			}
			String filename = FilenameUtils.getName(filepath);
			response.setHeader("Content-Disposition","attachment; filename=\""+filename+"\"");
			StreamUtil.streamToStream(inputStream, outputStream);
			outputStream.close();
		}
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			String type=(String)request.getAttribute("resultType");
			if (type==null) { type=request.getParameter("resultType"); }
			String fileName=(String)request.getAttribute("fileName");
			if (fileName==null) { fileName=request.getParameter("fileName"); }

			if (fileName==null) {
				PrintWriter out = response.getWriter();
				response.setContentType("text/html");
				out.println("fileName not specified");
				return;
			} else {
				if (!FileUtils.readAllowed(permissionRules, request, fileName)) {
					PrintWriter out = response.getWriter();
					response.setContentType("text/html");
					out.println("not allowed");
					return;
				}
			}

			if (type.equalsIgnoreCase("zip") || type.equalsIgnoreCase("bin")) {
				showInputStreamContents(fileName, type, response);
			} else {
				showReaderContents(fileName, type, response);
			}
		} catch (IOException e) {
			log.error("FileViewerServlet caught IOException", e);
			response.resetBuffer();
			response.sendError(500);
		}
	}

	@Override
	public String[] getAccessGrantingRoles() {
		return ALL_IBIS_USER_ROLES;
	}

	@Override
	public String getUrlMapping() {
		return "/FileViewerServlet";
	}

}
