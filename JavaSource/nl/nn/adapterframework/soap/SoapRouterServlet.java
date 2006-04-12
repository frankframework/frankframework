/*
 * $Log: SoapRouterServlet.java,v $
 * Revision 1.1  2006-04-12 16:16:35  europe\L190409
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
