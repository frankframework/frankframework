package nl.nn.adapterframework.webcontrol;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.Misc;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
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
 	FileViewerServlet.signal.ERROR.post=&lt;font color=red&gt;
 	FileViewerServlet.signal.WARN.pre=&lt;font color=blue&gt;
 	FileViewerServlet.signal.WARN.post=&lt;font color=blue&gt;
 	etc.
 	FileViewerServlet.signal.css=IE4.css
 	</pre></code>
 	The last item specifies which stylesheet to use.
 * <p>$Id: FileViewerServlet.java,v 1.2 2004-02-04 10:02:13 a1909356#db2admin Exp $</p>
 * @author Johan Verrips 
 */
public class FileViewerServlet extends HttpServlet  {
	public static final String version="$Id: FileViewerServlet.java,v 1.2 2004-02-04 10:02:13 a1909356#db2admin Exp $";
	
	protected Logger log = Logger.getLogger(this.getClass());	

	// key that is looked up to retrieve texts to be signalled
	private static String fvConfigKey="FileViewerServlet.signal";
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
	    try {
        PrintWriter out = response.getWriter();

        String fileContent="";
        String type=(String)request.getAttribute("resultType");
        if (type==null) type=request.getParameter("resultType");
        String fileName=(String)request.getAttribute("fileName");
        if (fileName==null) fileName=request.getParameter("fileName");

        if (fileName==null) {
            response.setContentType("text/html");
            out.println("fileName not specified");
            return;
        }
        if (type==null) {
            response.setContentType("text/html");
            out.println("resultType not specified");
            return;
        }

        if (type.equalsIgnoreCase("html")){
            fileContent=Misc.fileToString(fileName, "<br/>", true);
            response.setContentType("text/html");

            out.println("<html>");
            out.println("<head>");
            out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\""+AppConstants.getInstance().getProperty(fvConfigKey+".css")+"\">");
            out.println("</head>");
            out.println("<body>");
            StringTokenizer tok=AppConstants.getInstance().getTokenizer(fvConfigKey);
            while (tok.hasMoreTokens()){
	            String signal=tok.nextToken();
	            String pre=AppConstants.getInstance().getProperty(fvConfigKey+"."+signal+".pre");
	            String post=AppConstants.getInstance().getProperty(fvConfigKey+"."+signal+".post");
	            fileContent=StringUtils.replace(
		            fileContent, 
		            signal,
		            pre+signal+post);
	            
            }
            fileContent=StringUtils.replace(
                     fileContent,
                     "\t",
                     "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
             out.println(fileContent);
            out.println("</body>");
            out.println("</html>");
        }
        if (type.equalsIgnoreCase("text")){
            response.setContentType("text/plain");
            fileContent=Misc.fileToString(fileName, SystemUtils.LINE_SEPARATOR, false);
            out.println(fileContent);
        }
	    } catch (IOException e) {
		    log.error("FileViewerServlet caught IOException" , e);
		    throw e;
	    } catch (Throwable e) {
		    log.error("FileViewerServlet caught Throwable" , e);
		    throw new ServletException("FileViewerServlet caught Throwable" ,e);
	    }
	    

    }
    public void doPost(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        doGet(request, response);
    }
}
