package nl.nn.adapterframework.webcontrol.api;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

@SuppressWarnings("serial")
public class ServletDispatcher extends HttpServletDispatcher{
	
    public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
    }
	
    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        //HttpServletResponse resp = (HttpServletResponse) response;
        //Fetch authorisation header
        final String authorization = request.getHeader("Authorization");
        
        if (!request.getMethod().equalsIgnoreCase("OPTIONS")) {
	        if(authorization == null) {
	        	//Je moet inloggen
	            //resp.setStatus(401);
	            //return;
	        }
	        if(request.getUserPrincipal() == null) {
	        	//Foutief wachtwoord
	            //resp.setStatus(401);
	            //return;
	        }
        }

        super.service(request, response);
    }
}
