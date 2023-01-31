package nl.nn.adapterframework.webcontrol;

import javax.servlet.ServletContext;

import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

public class Dummy extends ContextLoaderListener {

	@Override
	protected WebApplicationContext createWebApplicationContext(ServletContext sc) {
		
//		return super.createWebApplicationContext(sc);
	}
}
