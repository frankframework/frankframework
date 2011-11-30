/*
 * $Log: SoapRouterServlet.java,v $
 * Revision 1.3  2011-11-30 13:52:00  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:53  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2006/04/12 16:16:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * extension to Apache RpcRouterServlet, that closes session after each call
 *
 */
package nl.nn.adapterframework.soap;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.soap.server.http.RPCRouterServlet;

/**
 * Modified Apache SOAP RPCRouterServlet, that invalidates the HTTPSession after each request.
 * 
 * @author  Gerrit van Brakel
 * @since   4.4.5
 * @version Id
 */
public class SoapRouterServlet extends RPCRouterServlet {
	
	public void doPost (HttpServletRequest req, HttpServletResponse res)
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
