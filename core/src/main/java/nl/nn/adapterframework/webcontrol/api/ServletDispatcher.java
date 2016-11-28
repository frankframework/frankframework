/*
Copyright 2016 Integration Partners B.V.

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
package nl.nn.adapterframework.webcontrol.api;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.util.AppConstants;

import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

/**
* Main dispatcher for all API calls.
* 
* @author Niels Meijer
*/

@SuppressWarnings("serial")
public class ServletDispatcher extends HttpServletDispatcher{

    private boolean consoleActive = AppConstants.getInstance().getBoolean("console.active", false);

    public void init(ServletConfig servletConfig) throws ServletException {
        if(consoleActive) {
            super.init(servletConfig);
        }
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        if(!consoleActive) {
            return;
        }
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
