/*
   Copyright 2023 WeAreFrank!

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
package nl.nn.adapterframework.webcontrol.runner;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

//web.xml configured listener
public class WebAppWacListener extends ContextLoaderListener {
	private static final String NAME = "Frank!Console";
	private AnnotationConfigWebApplicationContext context;

	@Override
	protected WebApplicationContext createWebApplicationContext(ServletContext sc) {
		context = new AnnotationConfigWebApplicationContext();
		context.register(CreateApiEndpointBean.class);

		try {
			WebApplicationContext parentApplicationContext = WebApplicationContextUtils.getWebApplicationContext(sc);
			if(parentApplicationContext != null) {
				context.setParent(parentApplicationContext);
			}
		}
		catch (Throwable t) {
			sc.log("Frank!Flow detected a WAC but was unable to set it!");
		}

		context.setDisplayName(NAME);
		context.setServletContext(sc);
		context.refresh();
		return context;
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		sce.getServletContext().log("Shutting down Frank!Flow");
		context.close();
	}
}
