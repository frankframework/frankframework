/*
   Copyright 2019-2021 WeAreFrank!

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
package org.frankframework.extensions.cmis.mtom;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.DependsOn;

import org.frankframework.configuration.ApplicationWarnings;
import org.frankframework.http.AbstractHttpServlet;
import org.frankframework.lifecycle.DynamicRegistration;
import org.frankframework.lifecycle.IbisInitializer;
import org.frankframework.lifecycle.ServletManager;
import org.frankframework.lifecycle.servlets.ServletConfiguration;
import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StringUtil;

@IbisInitializer
@DependsOn({"webServices10", "webServices11"})
@Deprecated(forRemoval = true, since = "7.6.0") // remove this class, use default webservices endpoints in combination with the CmisFilter
public class MtomProxy extends AbstractHttpServlet implements InitializingBean, ApplicationContextAware {

	private final Logger log = LogUtil.getLogger(this);
	private static final long serialVersionUID = 3L;

	private static final boolean ACTIVE = AppConstants.getInstance().getBoolean("cmis.mtomproxy.active", false);
	private static final String PROXY_SERVLET = AppConstants.getInstance().getProperty("cmis.mtomproxy.servlet", "WebServices11");
	private transient Servlet cmisWebServiceServlet = null;
	private transient ApplicationContext applicationContext;

	@Override
	public String getUrlMapping() {
		return "/cmis/proxy/webservices/*";
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		MtomRequestWrapper requestWrapper = new MtomRequestWrapper(request); //Turn every request into an MTOM request
		MtomResponseWrapper responseWrapper = new MtomResponseWrapper(response); //Check amount of parts, return either a SWA, MTOM or soap message

		cmisWebServiceServlet.service(requestWrapper, responseWrapper);
	}

	@Override
	public void afterPropertiesSet() {
		if(!isEnabled()) {
			return;
		}

		Map<String, Servlet> dynamicServlets = applicationContext.getBeansOfType(DynamicRegistration.Servlet.class);
		String servletName = StringUtil.lcFirst(PROXY_SERVLET);
		cmisWebServiceServlet = dynamicServlets.get(servletName);

		ServletManager servletManager = applicationContext.getBean("servletManager", ServletManager.class);
		ServletConfiguration config = servletManager.getServlet(PROXY_SERVLET);

		if(cmisWebServiceServlet == null || config == null) {
			log.warn("unable to find servlet [{}]", PROXY_SERVLET);
			throw new IllegalStateException("proxied servlet ["+PROXY_SERVLET+"] not found");
		}
		if(config.getLoadOnStartup() < 0) {
			throw new IllegalStateException("proxied servlet ["+PROXY_SERVLET+"] must have load on startup enabled!");
		}

		ApplicationWarnings.add(log, "CmisProxy has been deprecated. Please enable the MtomFilter [cmis.mtomfilter.active=true] and use default cmis endpoints instead!");

	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public boolean isEnabled() {
		return ACTIVE;
	}
}
