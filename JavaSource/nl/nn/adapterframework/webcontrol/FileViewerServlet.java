/*
 * $Log: FileViewerServlet.java,v $
 * Revision 1.4  2005-07-19 11:42:09  europe\L190409
 * performance improvements
 *
 */
package nl.nn.adapterframework.webcontrol;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.StringTokenizer;

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
	public static final String version = "$RCSfile: FileViewerServlet.java,v $ $Revision: 1.4 $ $Date: 2005-07-19 11:42:09 $";
	protected Logger log = Logger.getLogger(this.getClass());	

	// key that is looked up to retrieve texts to be signalled
	private static String fvConfigKey="FileViewerServlet.signal";


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

	public static void showReaderContents(Reader reader, String type, HttpServletResponse response) throws IOException {
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
		if (type.equalsIgnoreCase("text")){
			response.setContentType("text/plain");
			Misc.readerToWriter(reader, out);
		}
		out.close();
	}

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
	    try {
	        PrintWriter out = response.getWriter();
	
	        String type=(String)request.getAttribute("resultType");
	        if (type==null) type=request.getParameter("resultType");
	        String fileName=(String)request.getAttribute("fileName");
	        if (fileName==null) fileName=request.getParameter("fileName");
	
	        if (fileName==null) {
	            response.setContentType("text/html");
	            out.println("fileName not specified");
	            return;
	        }
	        showReaderContents(new FileReader(fileName),type,response);
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
