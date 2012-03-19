/*
 * $Log: SoapRouterServlet.java,v $
 * Revision 1.5  2012-03-19 15:07:22  m00f069
 * Bugfix mangled file name of WSDL when adapter name contains a space
 *
 * Revision 1.4  2011/12/15 09:55:31  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added Ibis WSDL generator (created by Michiel)
 *
 * Revision 1.1  2006/04/12 16:16:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * extension to Apache RpcRouterServlet, that closes session after each call
 *
 */
package nl.nn.adapterframework.soap;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.*;

import org.apache.soap.server.http.RPCRouterServlet;

/**
 * Modified Apache SOAP RPCRouterServlet, that invalidates the HTTPSession after each request.
 *
 * @author  Gerrit van Brakel
 * @since   4.4.5
 * @version Id
 */
public class SoapRouterServlet extends RPCRouterServlet {

    private final IbisSoapServlet ibisServlet = new IbisSoapServlet();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ibisServlet.init(config);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        ibisServlet.doGet(req, res);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res)
	  throws ServletException, IOException {
	  	try {
	  		super.doPost(req,res);
	  	} finally {
	  		HttpSession session = req.getSession();
	  		if (session!=null) {
	  			session.invalidate();
	  		}
	  	}
	  }

}
