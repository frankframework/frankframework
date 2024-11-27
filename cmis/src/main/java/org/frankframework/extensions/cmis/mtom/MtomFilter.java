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
import java.util.EnumSet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.context.ServletContextAware;

import org.frankframework.lifecycle.IbisInitializer;
import org.frankframework.util.AppConstants;

@IbisInitializer
@DependsOn({"webServices10", "webServices11"})
public class MtomFilter implements Filter, InitializingBean, ServletContextAware {
	private ServletContext servletContext;
	private static final boolean ACTIVE = AppConstants.getInstance().getBoolean("cmis.mtomfilter.active", false);

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		//Nothing to init
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if(ACTIVE) {
			MtomRequestWrapper requestWrapper = new MtomRequestWrapper(request); // Turn every request into an MTOM request
			MtomResponseWrapper responseWrapper = new MtomResponseWrapper(response); // Is this required?
			chain.doFilter(requestWrapper, responseWrapper);
		} else {
			chain.doFilter(request, response);
		}
	}

	@Override
	public void destroy() {
		//Nothing to destroy
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public void afterPropertiesSet() {
		FilterRegistration.Dynamic filter = servletContext.addFilter("CmisMtomFilter", this);
		EnumSet<DispatcherType> dispatcherTypes = EnumSet.of(DispatcherType.REQUEST);
		filter.addMappingForServletNames(dispatcherTypes, true, "WebServices10", "WebServices11");
	}
}
