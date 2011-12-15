/*
 * $Log: IbisSoapServlet.java,v $
 * Revision 1.2  2011-12-15 10:08:06  m00f069
 * Added CVS log
 *
 */
 package nl.nn.adapterframework.soap;

 import java.io.IOException;
 import java.io.Writer;
 import java.util.HashMap;
 import java.util.Iterator;

 import javax.servlet.ServletConfig;
 import javax.servlet.ServletException;
 import javax.servlet.http.*;
 import javax.xml.stream.XMLStreamException;
 import javax.xml.stream.XMLStreamWriter;

 import org.apache.log4j.Logger;

 import nl.nn.adapterframework.configuration.IbisContext;
 import nl.nn.adapterframework.configuration.IbisManager;
 import nl.nn.adapterframework.core.Adapter;
 import nl.nn.adapterframework.receivers.ServiceDispatcher;
 import nl.nn.adapterframework.util.AppConstants;
 import nl.nn.adapterframework.util.ClassUtils;
 import nl.nn.adapterframework.util.LogUtil;
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

    //@Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String attributeKey = AppConstants.getInstance().getProperty(ConfigurationServlet.KEY_CONTEXT);
        IbisContext ibisContext = (IbisContext) config.getServletContext().getAttribute(attributeKey);
        ibisManager = ibisContext.getIbisManager();

    }


    //@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        res.setHeader("Cache-Control", "max-age=3600, must-revalidate");
        res.setDateHeader("Expires", System.currentTimeMillis() + 3600000L);
        String pi = req.getPathInfo();
        if ((pi != null && pi.endsWith(".wsdl")) || req.getParameter("listener") != null) {
            res.setContentType("application/xml");
            wsdl(req, res);
        } else if (pi != null && pi.endsWith(".xsd")) {
            res.setContentType("application/xml");
            xsd(req, res);
        } else if (pi != null && pi.endsWith(".zip")) {
            res.setContentType("application/octet-stream");
            try {
                zip(req, res);
            } catch (XMLStreamException e) {
                throw new ServletException(e);
            }
        } else {
            res.setContentType("text/html; charset=UTF-8");
            list(req, res.getWriter());
        }
    }

     private void zip(HttpServletRequest req, HttpServletResponse res) throws IOException, XMLStreamException {

         Adapter adapter = getAdapter(ibisManager, req.getPathInfo());
         Wsdl wsdl = new Wsdl(adapter.getPipeLine(), true);
         res.setHeader("Content-Disposition",
             "inline;filename=" + wsdl.getName() + ".zip");
         String servlet = HttpUtils.getRequestURL(req).toString();
         servlet = servlet.substring(0, servlet.lastIndexOf(".")) + ".wsdl";

         wsdl.zip(res.getOutputStream(), servlet);


     }

     private void xsd(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

         String resource = req.getPathInfo();

         if (ClassUtils.getResourceURL(resource) == null) {
             res.sendError(HttpServletResponse.SC_NOT_FOUND);
         } else {
             String dir = resource.substring(0, resource.lastIndexOf('/'));
             Wsdl.XSD xs = new Wsdl.XSD(dir, null, ClassUtils.getResourceURL(resource), 0);
             if (mayServe(resource)) {
                 try {
                     XMLStreamWriter writer
                         = Wsdl.createWriter(res.getOutputStream(), false);
                     Wsdl.includeXSD(xs, writer, new HashMap(), false);
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
             String listener = req.getParameter("listener");
             Wsdl wsdl;
             if (listener == null) {
                 Adapter a = getAdapter(ibisManager, req.getPathInfo());
                 if (a ==  null) {
                     res.sendError(HttpServletResponse.SC_NOT_FOUND);
                     return;
                 }
                 wsdl = new Wsdl(a.getPipeLine(), indent);
             } else {
                 wsdl = new Wsdl(
                     ibisManager, listener, indent);
             }

             res.setHeader("Content-Disposition", "inline;filename=" +
                 wsdl.getName() + ".wsdl");

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
    //@Override
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
        final ServiceDispatcher dispatcher = ServiceDispatcher.getInstance();


        w.write("<html>");
        w.write("  <head>");
        w.write("    <title>Available WSDL's</title>");
        w.write("  </head>");
        w.write("<body>");
        w.write("  <ol>");
        Iterator i = dispatcher.getRegisteredListenerNames();
        while (i.hasNext()) {
            String serviceName = (String) i.next();
            w.write("<li>");
            Wsdl wsdl = new Wsdl(
                ibisManager,
                serviceName, false);
            String url =
                req.getContextPath() +
                req.getServletPath() +
                    "/" + wsdl.getName() + ".wsdl" +
                    "?indent=true";

            w.write("<a href='" + url + "'>" + serviceName + "</a>");

            String zip =
                req.getContextPath() +
                    req.getServletPath() +
                    "/" + wsdl.getName() + ".zip" +
                    "?indent=true";
            w.write(" (<a href='" + zip + "'>ZIP</a>)");
            w.write("</li>");
        }
        w.write("  </ol>");
        w.write("</body>\n</html>");

    }

}
