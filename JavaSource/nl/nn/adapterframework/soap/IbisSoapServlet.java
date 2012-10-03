/*
 * $Log: IbisSoapServlet.java,v $
 * Revision 1.8  2012-10-03 14:30:46  m00f069
 * Different filename for ESB Soap WSDL
 *
 * Revision 1.7  2012/10/01 15:23:44  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Strip schemaLocation from xsd import in case of generated WSDL with inline XSD's.
 *
 * Revision 1.6  2012/09/27 13:44:31  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Updates in generating wsdl namespace, wsdl input message name, wsdl output message name, wsdl port type name and wsdl operation name in case of EsbSoap
 *
 * Revision 1.5  2012/08/23 11:57:43  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Updates from Michiel
 *
 * Revision 1.4  2012/03/19 15:07:22  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Bugfix mangled file name of WSDL when adapter name contains a space
 *
 * Revision 1.3  2012/03/16 15:35:43  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Michiel added EsbSoapValidator and WsdlXmlValidator, made WSDL's available for all adapters and did a bugfix on XML Validator where it seems to be dependent on the order of specified XSD's
 *
 * Revision 1.2  2011/12/15 10:08:06  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added CVS log
 *
 */
 package nl.nn.adapterframework.soap;

import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.HashMap;

import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.util.*;
import nl.nn.adapterframework.webcontrol.ConfigurationServlet;

 /**
 * A soap servlet. I originally did not see that {@link SoapRouterServlet} returns dummy soap if not otherwise recognized, so I tried to redo the stuff without using apache rpc (in {@link Soap} in {@link #doPost}. This is currently unused, and I just call do {@link #doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}) from {@link SoapRouterServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}.
 *
 * @version Id
 * @author  Michiel Meeuwissen
 */
public class IbisSoapServlet extends HttpServlet {
     private static final Logger LOG = LogUtil.getLogger(IbisSoapServlet.class);

     private IbisManager ibisManager;
     private boolean caching = true;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        AppConstants appConstants = AppConstants.getInstance();
        String attributeKey = appConstants.getProperty(ConfigurationServlet.KEY_CONTEXT);
        IbisContext ibisContext = (IbisContext) config.getServletContext().getAttribute(attributeKey);
        if (ibisContext == null) throw new IllegalStateException("No ibis context found with " + ConfigurationServlet.KEY_CONTEXT);
        ibisManager = ibisContext.getIbisManager();
        if ("false".equals(appConstants.getProperty("wsdl.caching"))) {
            caching = false;
        }
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        if (caching) {
            res.setHeader("Cache-Control", "max-age=3600, must-revalidate");
            res.setDateHeader("Expires", System.currentTimeMillis() + 3600000L);
        }
        String pi = req.getPathInfo();
        if ((pi != null && pi.endsWith(".wsdl")) || req.getParameter("listener") != null) {
            res.setContentType("application/xml");
            wsdl(req, res);
        } else if (pi != null && pi.endsWith(".xsd")) {
            res.setContentType("application/xml");
            try {
                xsd(req, res);
            } catch (URISyntaxException e) {
                throw new ServletException(e.getMessage(), e);
            }
        } else if (pi != null && pi.endsWith(".zip")) {
            res.setContentType("application/octet-stream");
            try {
                zip(req, res);
            } catch (XMLStreamException e) {
                throw new ServletException(e);
            } catch (URISyntaxException e) {
                throw new ServletException(e);
            } catch (NamingException e) {
                throw new ServletException(e);
            }
        } else {
            res.setContentType("text/html; charset=UTF-8");
            list(req, res.getWriter());
        }
    }

     private void zip(HttpServletRequest req, HttpServletResponse res) throws IOException, XMLStreamException, URISyntaxException, NamingException {

         Adapter adapter = getAdapter(ibisManager, req.getPathInfo());
         Wsdl wsdl = new Wsdl(adapter.getPipeLine(), true);
         res.setHeader("Content-Disposition",
             "inline;filename=\"" + wsdl.getFilename() + ".zip\"");
         String servlet = HttpUtils.getRequestURL(req).toString();
         servlet = servlet.substring(0, servlet.lastIndexOf(".")) + ".wsdl";

         wsdl.zip(res.getOutputStream(), servlet);


     }

     private void xsd(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException, URISyntaxException {

         String resource = req.getPathInfo();

         if (ClassUtils.getResourceURL(resource) == null) {
             res.sendError(HttpServletResponse.SC_NOT_FOUND);
         } else {
             String dir = resource.substring(0, resource.lastIndexOf('/'));
             XSD xs = new XSD(dir, null, ClassUtils.getResourceURL(resource).toURI(), 0);
             if (mayServe(resource)) {
                 try {
                     XMLStreamWriter writer
                         = WsdlUtils.createWriter(res.getOutputStream(), false);
                     WsdlUtils.includeXSD(xs, writer, new HashMap<String, String>(), false, false);
                 } catch (XMLStreamException e) {
                     throw new ServletException(e);
                 }
             } else {
                 res.sendError(HttpServletResponse.SC_FORBIDDEN);
             }
         }
     }

     private void wsdl(HttpServletRequest req, HttpServletResponse res) throws ServletException {
         String servlet = HttpUtils.getRequestURL(req).toString();
         try {
             boolean indent = "true".equals(req.getParameter("indent"));
             Wsdl wsdl;
             Adapter a = getAdapter(ibisManager, req.getPathInfo());
             if (a ==  null) {
                 res.sendError(HttpServletResponse.SC_NOT_FOUND);
                 return;
             }
             wsdl = new Wsdl(a.getPipeLine(), indent);
             res.setHeader("Content-Disposition", "inline;filename=\"" +  wsdl.getFilename() + ".wsdl\"");
             wsdl.setIncludeXsds("true".equals(req.getParameter("includeXsds")));
             wsdl.wsdl(res.getOutputStream(), servlet);
         } catch (Exception e) {

             throw new ServletException(e);
         }
     }

     protected boolean mayServe(String resource) {
         // TODO, perhaps we should simple check wether the resource is indeed used in a WSDL
         return resource.toLowerCase().endsWith(".xsd");
     }


     protected Adapter getAdapter(IbisManager ibisManager, String pathInfo) {
         if (pathInfo.startsWith("/")) pathInfo = pathInfo.substring(1);
         int dot = pathInfo.lastIndexOf('.');
         pathInfo = pathInfo.substring(0, dot);
         return (Adapter) ibisManager.getConfiguration().getRegisteredAdapter(pathInfo);
     }
     /**
      * TODO Unused
      */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            res.setContentType("text/xml");
            new Soap().soap(req, res);

        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            HttpSession session = req.getSession();
            if (session!=null) {
                session.invalidate();
            }
        }
	  }



    protected void list(HttpServletRequest req, Writer w) throws IOException {
        w.write("<html>");
        w.write(  "<head>");
        w.write(     "<title>Available WSDL's</title>");
        w.write(  "</head>");
        w.write("<body>");
        w.write("<ol>");


        int count = 0;
        for (IAdapter a : ibisManager.getConfiguration().getRegisteredAdapters()) {
            count++;
            w.write("<li>");
            try {
                Adapter adapter = (Adapter) a;
                Wsdl wsdl = new Wsdl(adapter.getPipeLine(),
                    false);
                String url =
                    req.getContextPath() +
                        req.getServletPath() +
                        "/" + wsdl.getName() + ".wsdl" +
                        "?indent=true";

                w.write("<a href='" + url + "'>" + wsdl.getName() + "</a>");

                String includeXsds  =
                    req.getContextPath() +
                        req.getServletPath() +
                        "/" + wsdl.getName() + ".wsdl" +
                        "?indent=true&amp;includeXsds=true";

                w.write(" (<a href='" + includeXsds + "'>with xsds</a>)");

                String zip =
                    req.getContextPath() +
                        req.getServletPath() +
                        "/" + wsdl.getName() + ".zip" +
                        "?indent=true";
                w.write(" (<a href='" + zip + "'>ZIP</a>)");

            } catch (Exception e) {
                w.write(e.getMessage());
            }
            w.write("</li>");
        }
        w.write("</ol>");
        if (count == 0) {
            w.write("<p>No registered listeners found</p>");
        }
        w.write("</body></html>");

    }

}
