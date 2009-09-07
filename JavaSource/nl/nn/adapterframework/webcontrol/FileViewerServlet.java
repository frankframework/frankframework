/*
 * $Log: FileViewerServlet.java,v $
 * Revision 1.12  2009-09-07 13:54:23  L190409
 * made some static variables final
 *
 * Revision 1.11  2009/04/03 14:34:36  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added statistics file viewer
 *
 * Revision 1.10  2007/09/24 13:05:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * ability to download file, using correct filename
 *
 * Revision 1.9  2007/06/14 09:45:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * strip slash from context path
 *
 * Revision 1.8  2007/02/12 14:41:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.7  2005/12/05 08:36:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified handling of log4j-xml files
 *
 * Revision 1.6  2005/10/17 11:05:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * replace non-printable characters from log4j.xml
 *
 * Revision 1.5  2005/10/17 09:34:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added log4j xml view facility
 *
 * Revision 1.4  2005/07/19 11:42:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * performance improvements
 *
 */
package nl.nn.adapterframework.webcontrol;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.EncapsulatingReader;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StatisticsUtil;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

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
 * @version Id
 * @author Johan Verrips 
 */
public class FileViewerServlet extends HttpServlet  {
	public static final String version = "$RCSfile: FileViewerServlet.java,v $ $Revision: 1.12 $ $Date: 2009-09-07 13:54:23 $";
	protected static Logger log = LogUtil.getLogger(FileViewerServlet.class);	

	// key that is looked up to retrieve texts to be signalled
	private static final String fvConfigKey="FileViewerServlet.signal";

	private static final String log4j_html_xslt = "/xml/xsl/log4j_html.xsl";
	private static final String log4j_text_xslt = "/xml/xsl/log4j_text.xsl";
	private static final String log4j_prefix    = "<log4j:log4j xmlns:log4j=\"http://jakarta.apache.org/log4\">\n\n";
	private static final String log4j_postfix	  = "</log4j:log4j>";
	private static final String stats_html_xslt = "/xml/xsl/stats_html.xsl";
	private static final String stats_prefix    = "<statisticsCollections>";
	private static final String stats_postfix	  = "</statisticsCollections>";

	public static String makeConfiguredReplacements(String input) {
		StringTokenizer tok=AppConstants.getInstance().getTokenizer(fvConfigKey);
		while (tok.hasMoreTokens()){
			String signal=tok.nextToken();
			String pre=AppConstants.getInstance().getProperty(fvConfigKey+"."+signal+".pre");
			String post=AppConstants.getInstance().getProperty(fvConfigKey+"."+signal+".post");
			input=StringUtils.replace(input, signal, pre+signal+post);
		}
		return StringUtils.replace(input, "\t", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
	}


	public static void transformReader(Reader reader, String filename, Map parameters, HttpServletResponse response, String input_prefix, String input_postfix, String stylesheetUrl, String instanceName, String title) throws DomBuilderException, TransformerException, IOException { 
		PrintWriter out = response.getWriter();
		Reader fileReader = new EncapsulatingReader(reader, input_prefix, input_postfix, true);
		URL xsltSource = ClassUtils.getResourceURL( FileViewerServlet.class, stylesheetUrl);
		if (xsltSource!=null) {
			Transformer transformer = XmlUtils.createTransformer(xsltSource);
			if (parameters!=null) {
				XmlUtils.setTransformerParameters(transformer, parameters);
			}
			XmlUtils.transformXml(transformer, new StreamSource(fileReader),out);
			out.close();
		} else {
			showReaderContents(fileReader,filename,"text",response,instanceName,title);
		}
	}

	public static void showReaderContents(Reader reader, String filename, String type, HttpServletResponse response, String instanceName, String title) throws DomBuilderException, TransformerException, IOException {
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
			out.println("<title>"+instanceName+"@"+Misc.getHostname()+" - "+title+"</title>");
			out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\""+AppConstants.getInstance().getProperty(fvConfigKey+".css")+"\">");
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
			Misc.readerToWriter(reader, out);
		}
		if (type.equalsIgnoreCase("xml")) {
			response.setContentType("application/xml");
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
	
	        if (fileName==null) {
				PrintWriter out = response.getWriter();
	            response.setContentType("text/html");
	            out.println("fileName not specified");
	            return;
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
				transformReader(new FileReader(fileName), fileName, null, response, log4j_prefix, log4j_postfix, stylesheetUrl, request.getContextPath().substring(1),fileName);
	        } else {
				boolean statsFlag = "xml".equalsIgnoreCase(stats) || "true".equalsIgnoreCase(stats);
				if (statsFlag) {
					Map parameters = new Hashtable();
					String timestamp = (String) request.getAttribute("timestamp");
					if (timestamp == null) { timestamp = request.getParameter("timestamp"); }
					if (timestamp!= null) {
						parameters.put("timestamp", timestamp);
					}
					String adapterName = (String) request.getAttribute("adapterName");
					if (adapterName == null) { adapterName = request.getParameter("adapterName"); }
					if (adapterName!= null) {
						parameters.put("adapterName", adapterName);
					}
					String stylesheetUrl;
					response.setContentType("text/html");
					stylesheetUrl=stats_html_xslt;

					transformReader(new StringReader(StatisticsUtil.fileToString(fileName, timestamp, adapterName)), fileName, parameters, response, stats_prefix, stats_postfix, stylesheetUrl, request.getContextPath().substring(1),fileName);
				} else {
//					Reader r=new BufferedReader(new InputStreamReader(new FileInputStream(fileName),"ISO-8859-1"));
//					showReaderContents(r, fileName, type, response, request.getContextPath().substring(1),fileName);
					showReaderContents(new FileReader(fileName), fileName, type, response, request.getContextPath().substring(1),fileName);
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
    
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doGet(request, response);
    }
}
