package nl.nn.adapterframework.extensions.cmis;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

public class CmisRepositoryContextListener implements ServletContextListener {
	private final String LISTENER_CLASS = "org.apache.chemistry.opencmis.server.impl.CmisRepositoryContextListener";
	private ServletContextListener listener;
	private final Logger log = LogUtil.getLogger(this);

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		try {
			listener = (ServletContextListener) ClassUtils.newInstance(LISTENER_CLASS);
			listener.contextInitialized(sce);
		} catch (ClassNotFoundException e) {
			// We don't really need to log anything here...
		} catch (UnsupportedClassVersionError e) {
			log.error("CMIS was found on the classpath but requires Java 1.7 or higher to run");
		} catch (Exception e) {
			// Do log all other exceptions though..
			log.error("unhandled exception occured while loading or initiating cmis listener ["+LISTENER_CLASS+"]", e);
		}
	}
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		if(listener != null)
			listener.contextDestroyed(sce);
	}
}
