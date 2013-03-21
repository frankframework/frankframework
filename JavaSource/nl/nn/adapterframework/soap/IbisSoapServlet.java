/*
   Copyright 2013 Nationale-Nederlanden

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
/*
 * $Log: IbisSoapServlet.java,v $
 * Revision 1.13  2012-12-06 15:19:28  m00f069
 * Resolved warnings which showed up when using addNamespaceToSchema (src-include.2.1: The targetNamespace of the referenced schema..., src-resolve.4.2: Error resolving component...)
 * Handle includes in XSD's properly when generating a WSDL
 * Removed XSD download (unused and XSD's were not adjusted according to e.g. addNamespaceToSchema)
 * Sort schema's in WSDL (made sure the order is always the same)
 * Indent WSDL with tabs instead of spaces
 * Some cleaning and refactoring (made WSDL generator and XmlValidator share code)
 *
 * Revision 1.12  2012/10/26 15:43:18  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Made WSDL without separate XSD's the default
 *
 * Revision 1.11  2012/10/24 14:34:00  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Load imported XSD's into the WSDL too
 * When more than one XSD with the same namespace is present merge them into one schema element in the WSDL
 * Exclude SOAP Envelope XSD
 *
 * Revision 1.10  2012/10/11 09:45:58  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added WSDL filename to WSDL documentation
 *
 * Revision 1.9  2012/10/04 11:28:57  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Fixed ESB Soap namespace
 * Added location (url) of WSDL generation to the WSDL documentation
 * Show warning add the bottom of the WSDL (if any) instead of Ibis logging
 *
 * Revision 1.8  2012/10/03 14:30:46  Jaco de Groot <jaco.de.groot@ibissource.org>
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

import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUtils;
import javax.xml.stream.XMLStreamException;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.webcontrol.ConfigurationServlet;

 /**
 * A soap servlet. I originally did not see that {@link SoapRouterServlet} returns dummy soap if not otherwise recognized, so I tried to redo the stuff without using apache rpc (in {@link Soap} in {@link #doPost}. This is currently unused, and I just call do {@link #doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}) from {@link SoapRouterServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}.
 *
 * @version $Id$
 * @author  Michiel Meeuwissen
 */
public class IbisSoapServlet extends HttpServlet {
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
        if ((pi != null && pi.endsWith(getWsdlExtention())) || req.getParameter("listener") != null) {
            res.setContentType("application/xml");
            wsdl(req, res);
        } else if (pi != null && pi.endsWith(".zip")) {
            res.setContentType("application/octet-stream");
            try {
                zip(req, res);
            } catch (XMLStreamException e) {
                throw new ServletException(e);
            } catch (NamingException e) {
                throw new ServletException(e);
            }
        } else {
            res.setContentType("text/html; charset=UTF-8");
            list(req, res.getWriter());
        }
    }

     private void zip(HttpServletRequest req, HttpServletResponse res) throws IOException, XMLStreamException, NamingException {
         Adapter adapter = getAdapter(ibisManager, req.getPathInfo());
         Wsdl wsdl = new Wsdl(adapter.getPipeLine());
         wsdl.setUseIncludes(true);
         setDocumentation(wsdl, req);
         wsdl.init();
         res.setHeader("Content-Disposition",
             "inline;filename=\"" + wsdl.getFilename() + ".zip\"");
         String servlet = HttpUtils.getRequestURL(req).toString();
         servlet = servlet.substring(0, servlet.lastIndexOf(".")) + getWsdlExtention();
         wsdl.zip(res.getOutputStream(), servlet);
     }

     private void wsdl(HttpServletRequest req, HttpServletResponse res) throws ServletException {
         String servlet = HttpUtils.getRequestURL(req).toString();
         try {
             Wsdl wsdl;
             Adapter a = getAdapter(ibisManager, req.getPathInfo());
             if (a ==  null) {
                 res.sendError(HttpServletResponse.SC_NOT_FOUND);
                 return;
             }
             wsdl = new Wsdl(a.getPipeLine());
             if (req.getParameter("indent") != null) {
                 wsdl.setIndent("true".equals(req.getParameter("indent")));
             }
             if (req.getParameter("useIncludes") != null) {
                 wsdl.setUseIncludes("true".equals(req.getParameter("useIncludes")));
             }
             setDocumentation(wsdl, req);
             wsdl.init();
             res.setHeader("Content-Disposition", "inline;filename=\"" +  wsdl.getFilename() + ".wsdl\"");
             wsdl.wsdl(res.getOutputStream(), servlet);
         } catch (Exception e) {
             throw new ServletException(e);
         }
     }

     protected Adapter getAdapter(IbisManager ibisManager, String pathInfo) {
         if (pathInfo.startsWith("/")) pathInfo = pathInfo.substring(1);
         int dot = pathInfo.lastIndexOf('.');
         pathInfo = pathInfo.substring(0, dot);
         return (Adapter) ibisManager.getConfiguration().getRegisteredAdapter(pathInfo);
     }

	protected static void setDocumentation(Wsdl wsdl, HttpServletRequest req) {
		wsdl.setDocumentation("Generated at " + req.getRequestURL()
				+ " as " + wsdl.getFilename() + getWsdlExtention()
				+ " on " + DateUtils.getIsoTimeStamp() + ".");
	}

	protected static String getWsdlExtention() {
		return ".wsdl";
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
                Wsdl wsdl = new Wsdl(adapter.getPipeLine());
                setDocumentation(wsdl, req);
                wsdl.init(true);
                String url =
                    req.getContextPath() +
                        req.getServletPath() +
                        "/" + wsdl.getName() + getWsdlExtention();

                w.write("<a href='" + url + "'>" + wsdl.getName() + "</a>");

                String useIncludes  =
                    req.getContextPath() +
                        req.getServletPath() +
                        "/" + wsdl.getName() + getWsdlExtention() +
                        "?useIncludes=true";

                w.write(" (<a href='" + useIncludes + "'>using includes</a>");

                String zip =
                    req.getContextPath() +
                        req.getServletPath() +
                        "/" + wsdl.getName() + ".zip";
                w.write(" <a href='" + zip + "'>zip</a>)");

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
