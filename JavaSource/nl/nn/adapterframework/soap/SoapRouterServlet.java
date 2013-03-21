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
 * @version $Id$
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
