/*
   Copyright 2013, 2015, 2016 Nationale-Nederlanden

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
 package nl.nn.adapterframework.soap;

import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Iterator;

import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUtils;
import javax.xml.stream.XMLStreamException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.http.RestListener;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.webcontrol.ConfigurationServlet;

 /**
 * A soap servlet. I originally did not see that {@link SoapRouterServlet} returns dummy soap if not otherwise recognized, so I tried to redo the stuff without using apache rpc (in {@link Soap} in {@link #doPost}. This is currently unused, and I just call do {@link #doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}) from {@link SoapRouterServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}.
 *
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
            } catch (ConfigurationException e) {
                throw new ServletException(e);
            }
        } else {
            res.setContentType("text/html; charset=UTF-8");
            list(req, res.getWriter());
        }
    }

     private void zip(HttpServletRequest req, HttpServletResponse res) throws IOException, XMLStreamException, NamingException, ConfigurationException {
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
         return (Adapter) ibisManager.getRegisteredAdapter(pathInfo);
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
        w.write(     "<title>Webservices</title>");
        w.write(  "</head>");
        w.write("<body>");
        w.write("<h1>Webservices</h1>");
        listRestServices(req, w);
        listWsdls(req, w);
        w.write("</body></html>");
    }

    protected void listWsdls(HttpServletRequest req, Writer w) throws IOException {
    	w.write("<h2>Available WSDL's:</h2>");
    	w.write("<ol>");

        int count = 0;
        for (IAdapter a : ibisManager.getRegisteredAdapters()) {
            count++;
            w.write("<li>");
            try {
                Adapter adapter = (Adapter) a;
                Wsdl wsdl = new Wsdl(adapter.getPipeLine());
                setDocumentation(wsdl, req);
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
                w.write(a.getName() + ": ");
                if (e.getMessage() != null) {
                    w.write(e.getMessage());
                } else {
                    w.write(e.toString());
                }
            }
            w.write("</li>");
        }
        w.write("</ol>");
        if (count == 0) {
            w.write("No registered listeners found");
        }
    }

    protected void listRestServices(HttpServletRequest req, Writer w) throws IOException {
    	w.write("<h2>Available REST services:</h2>");
        w.write("<ol>");

        int count = 0;
        for (IAdapter a : ibisManager.getRegisteredAdapters()) {
			Adapter adapter = (Adapter) a;
			Iterator recIt=adapter.getReceiverIterator();
			while (recIt.hasNext()){
				IReceiver receiver=(IReceiver) recIt.next();
				if (receiver instanceof ReceiverBase ) {
					ReceiverBase rb = (ReceiverBase) receiver;
					IListener listener = rb.getListener();
					if (listener instanceof RestListener) {
						RestListener rl = (RestListener) listener;
						if (rl.isView()) {
				        	count++;
				            w.write("<li>");
			                w.write("<a href=../" + rl.getRestUriPattern() + ">" + rb.getName() + "</a>");
				            w.write("</li>");
						}
					}
				}
			}
        }
        w.write("</ol>");
        if (count == 0) {
            w.write("No rest listeners found");
        }
    }
}
