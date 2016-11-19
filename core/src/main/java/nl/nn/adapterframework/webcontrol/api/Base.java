package nl.nn.adapterframework.webcontrol.api;


import javax.servlet.ServletConfig;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.webcontrol.ConfigurationServlet;

import org.apache.log4j.Logger;

public abstract class Base {
	protected Logger log = LogUtil.getLogger(this);
	protected IbisContext ibisContext = null;
	protected IbisManager ibisManager = null;
	
	protected void initBase(ServletConfig servletConfig) {
		String attributeKey = AppConstants.getInstance().getProperty(ConfigurationServlet.KEY_CONTEXT);
		ibisContext = (IbisContext) servletConfig.getServletContext().getAttribute(attributeKey);
		ibisManager = null;
        if (ibisContext != null) {
        	ibisManager = ibisContext.getIbisManager();
        }
		if (ibisManager==null) {
			log.warn("Could not retrieve ibisManager from context");
		} else {
			log.debug("retrieved ibisManager ["+ClassUtils.nameOf(ibisManager)+"]["+ibisManager+"] from servlet context attribute ["+attributeKey+"]");
		}
	}
}
