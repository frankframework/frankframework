/*
   Copyright 2013 Nationale-Nederlanden, 2022 WeAreFrank!

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
package nl.nn.adapterframework.webcontrol;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.http.HttpServletBase;
import nl.nn.adapterframework.lifecycle.IbisInitializer;
import nl.nn.adapterframework.statistics.StatisticsUtil;
import nl.nn.adapterframework.statistics.parser.StatisticsParser;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.EncapsulatingReader;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlUtils;

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
@IbisInitializer
public class FileViewerServlet extends HttpServletBase {
	protected static Logger log = LogUtil.getLogger(FileViewerServlet.class);

	// key that is looked up to retrieve texts to be signaled
	private static final String fvConfigKey="FileViewerServlet.signal";

	private static final String log4j_html_xslt = "/xml/xsl/log4j_html.xsl";
	private static final String log4j_text_xslt = "/xml/xsl/log4j_text.xsl";
	private static final String log4j_prefix    = "<log4j:log4j xmlns:log4j=\"http://jakarta.apache.org/log4\">\n\n";
	private static final String log4j_postfix	  = "</log4j:log4j>";
	private static final String stats_html_xslt = "/xml/xsl/stats_html.xsl";
	private static final String stats_prefix    = "<statisticsCollections>";
	private static final String stats_postfix	  = "</statisticsCollections>";

	public static final String permissionRules = AppConstants.getInstance().getResolvedProperty("FileViewerServlet.permission.rules");

	public static String makeConfiguredReplacements(String input) {
		StringTokenizer tok=AppConstants.getInstance().getTokenizedProperty(fvConfigKey);
		while (tok.hasMoreTokens()){
			String signal=tok.nextToken();
			String pre=AppConstants.getInstance().getProperty(fvConfigKey+"."+signal+".pre");
			String post=AppConstants.getInstance().getProperty(fvConfigKey+"."+signal+".post");
			input=StringUtils.replace(input, signal, pre+signal+post);
		}
		return StringUtils.replace(input, "\t", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
	}


	public static void transformReader(Reader reader, String filename, Map<String, Object> parameters, HttpServletResponse response, String input_prefix, String input_postfix, String stylesheetUrl, String title) throws DomBuilderException, TransformerException, IOException {
		PrintWriter out = response.getWriter();
		Reader fileReader = new EncapsulatingReader(reader, input_prefix, input_postfix, true);
		URL xsltSource = ClassUtils.getResourceURL(stylesheetUrl);
		if (xsltSource!=null) {
			Transformer transformer = XmlUtils.createTransformer(xsltSource);
			if (parameters!=null) {
				XmlUtils.setTransformerParameters(transformer, parameters);
			}
			XmlUtils.transformXml(transformer, new StreamSource(fileReader),out);
			out.close();
		} else {
			showReaderContents(fileReader,filename,"text",response,title);
		}
	}

	public static void transformSource(Source source, Map<String, Object> parameters, HttpServletResponse response, String stylesheetUrl, String title) throws TransformerException, IOException {
		PrintWriter out = response.getWriter();
		URL xsltSource = ClassUtils.getResourceURL(stylesheetUrl);
		Transformer transformer = XmlUtils.createTransformer(xsltSource);
		if (parameters!=null) {
			XmlUtils.setTransformerParameters(transformer, parameters);
		}
		XmlUtils.transformXml(transformer, source, out);
		out.close();
	}

	public static void showReaderContents(Reader reader, String filename, String type, HttpServletResponse response, String title) throws DomBuilderException, TransformerException, IOException {
		PrintWriter out = response.getWriter();
		if (type==null) {
			response.setContentType("text/html");
			out.println("resultType not specified");
			return;
		}

		if (type.equalsIgnoreCase("html")){
			response.setContentType("text/html");

			out.println("<html>");
			out.println("<head>");
			out.println("<title>"+AppConstants.getInstance().getResolvedProperty("instance.name.lc")+"@"+Misc.getHostname()+" - "+title+"</title>");
			out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"iaf/"+AppConstants.getInstance().getProperty(fvConfigKey+".css")+"\">");
			out.println("</head>");
			out.println("<body>");

			LineNumberReader lnr = new LineNumberReader(reader);
			String line;
			while ((line=lnr.readLine())!=null) {
				out.println(makeConfiguredReplacements(XmlUtils.encodeChars(line))+"<br/>");
			}

			out.println("</body>");
			out.println("</html>");
		}
		if (type.equalsIgnoreCase("text")) {
			response.setContentType("text/plain");
			String lastPart;
			try {
				File f= new File(filename);
				lastPart=f.getName();
			} catch (Throwable t) {
				lastPart=filename;
			}
			response.setHeader("Content-Disposition","attachment; filename=\""+lastPart+"\"");
			StreamUtil.readerToWriter(reader, out);
		}
		if (type.equalsIgnoreCase("xml")) {
			response.setContentType("application/xml");
			String lastPart;
			try {
				File f= new File(filename);
				lastPart=f.getName();
			} catch (Throwable t) {
				lastPart=filename;
			}
			response.setHeader("Content-Disposition","inline; filename=\""+lastPart+"\"");
			LineNumberReader lnr;
			if (filename.indexOf("_xml.log")>=0) {
				Reader fileReader = new EncapsulatingReader(reader, log4j_prefix, log4j_postfix, true);
				lnr = new LineNumberReader(fileReader);
			} else {
				if (filename.indexOf("-stats_")>=0) {
					Reader fileReader = new EncapsulatingReader(reader, stats_prefix, stats_postfix, true);
					lnr = new LineNumberReader(fileReader);
				} else {
					lnr = new LineNumberReader(reader);
				}
			}
			String line;
			while ((line=lnr.readLine())!=null) {
				out.println(line+"\n");
			}
		}
		out.close();
	}

	public static void showInputStreamContents(InputStream inputStream, String filename, String type, HttpServletResponse response) throws DomBuilderException, TransformerException, IOException {
		ServletOutputStream outputStream = response.getOutputStream();
		if (type.equalsIgnoreCase("zip")) {
			response.setContentType("application/zip");
		} else {
			response.setContentType("application/octet-stream");
		}
		String lastPart;
		try {
			File f= new File(filename);
			lastPart=f.getName();
		} catch (Throwable t) {
			lastPart=filename;
		}
		response.setHeader("Content-Disposition","attachment; filename=\""+lastPart+"\"");
		StreamUtil.streamToStream(inputStream, outputStream);
		outputStream.close();
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		try {

			String type=(String)request.getAttribute("resultType");
			if (type==null) { type=request.getParameter("resultType"); }
			String fileName=(String)request.getAttribute("fileName");
			if (fileName==null) { fileName=request.getParameter("fileName"); }
			String log4j = (String) request.getAttribute("log4j");
			if (log4j == null) { log4j = request.getParameter("log4j"); }
			String stats = (String) request.getAttribute("stats");
			if (stats == null) { stats = request.getParameter("stats"); }
//			String pipeSplit = (String) request.getAttribute("pipeSplit");
//			if (pipeSplit == null) { pipeSplit = request.getParameter("pipeSplit"); }

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
			boolean log4jFlag = "xml".equalsIgnoreCase(log4j) || "true".equalsIgnoreCase(log4j);
			if (log4jFlag) {
				String stylesheetUrl;
				if ("html".equalsIgnoreCase(type)) {
					response.setContentType("text/html");
					stylesheetUrl=log4j_html_xslt;
				} else {
					response.setContentType("text/plain");
					stylesheetUrl=log4j_text_xslt;
				}
				transformReader(new FileReader(fileName), fileName, null, response, log4j_prefix, log4j_postfix, stylesheetUrl, fileName);
			} else {
				boolean statsFlag = "xml".equalsIgnoreCase(stats) || "true".equalsIgnoreCase(stats);
				if (statsFlag) {
//					boolean pipeSplitFlag = "true".equalsIgnoreCase(pipeSplit);
					Map<String, Object> parameters = new Hashtable<>();
					String timestamp = (String) request.getAttribute("timestamp");
					if (timestamp == null) { timestamp = request.getParameter("timestamp"); }
					if (timestamp!= null) {
						parameters.put("timestamp", timestamp);
					}
					String servletPath="FileViewerServlet?resultType="+type+"&fileName="+fileName+"&stats="+stats+"&";
					parameters.put("servletPath",servletPath);
					String adapterName = (String) request.getAttribute("adapterName");
					if (adapterName == null) { adapterName = request.getParameter("adapterName"); }

					//log.debug("adapterName ["+adapterName+"]");
					String extract;

					if (StringUtils.isEmpty(adapterName) || StringUtils.isEmpty(timestamp)) {
						StatisticsParser sp = new StatisticsParser(adapterName,timestamp); // split by pipe not yet supported
						sp.digestStatistics(fileName);
						extract=sp.toXml().toXML();
					} else {
						parameters.put("adapterName", adapterName);
						extract=stats_prefix+StatisticsUtil.fileToString(fileName, timestamp, adapterName)+stats_postfix;
					}
					//log.debug(extract);
					if ("xml".equals(request.getParameter("output"))) {
						response.setContentType("text/xml;charset="+StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
						PrintWriter pw = response.getWriter();
						pw.write(extract);
						pw.close();
					} else {
						response.setContentType("text/html;charset="+StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
						Source s= XmlUtils.stringToSourceForSingleUse(extract);
						String stylesheetUrl=stats_html_xslt;
						transformSource(s,parameters,response,stylesheetUrl,fileName);
					}

				} else {
					if (type.equalsIgnoreCase("zip") || type.equalsIgnoreCase("bin")) {
						showInputStreamContents(new DataInputStream(new FileInputStream(fileName)), fileName, type, response);
					} else {
//						Reader r=new BufferedReader(new InputStreamReader(new FileInputStream(fileName),"ISO-8859-1"));
//						showReaderContents(r, fileName, type, response, request.getContextPath().substring(1),fileName);
						showReaderContents(new FileReader(fileName), fileName, type, response, fileName);
					}
				}
			}
		} catch (IOException e) {
			log.error("FileViewerServlet caught IOException" , e);
			throw e;
		} catch (Throwable e) {
			log.error("FileViewerServlet caught Throwable" , e);
			throw new ServletException("FileViewerServlet caught Throwable" ,e);
		}
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		doGet(request, response);
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
