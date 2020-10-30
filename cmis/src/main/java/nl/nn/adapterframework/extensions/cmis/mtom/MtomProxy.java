package nl.nn.adapterframework.extensions.cmis.mtom;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.DependsOn;

import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.http.HttpServletBase;
import nl.nn.adapterframework.lifecycle.DynamicRegistration;
import nl.nn.adapterframework.lifecycle.IbisInitializer;
import nl.nn.adapterframework.lifecycle.ServletManager;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

@IbisInitializer
@DependsOn({"webServices10", "webServices11"})
@Deprecated // remove this class, use default webservices endpoints in combination with the CmisFilter
public class MtomProxy extends HttpServletBase implements InitializingBean, ApplicationContextAware {

	private final Logger log = LogUtil.getLogger(this);
	private static final long serialVersionUID = 3L;

	private static final boolean ACTIVE = AppConstants.getInstance().getBoolean("cmis.mtomproxy.active", false);
	private static final String PROXY_SERVLET = AppConstants.getInstance().getProperty("cmis.mtomproxy.servlet", "webServices11");
	private Servlet cmisWebServiceServlet = null;

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
	public void afterPropertiesSet() throws Exception {
		if(ACTIVE && cmisWebServiceServlet == null) {
			log.warn("unable to find servlet [" + PROXY_SERVLET + "]");
			throw new IllegalStateException("proxied servlet ["+PROXY_SERVLET+"] not found");
		}
		if(ACTIVE && cmisWebServiceServlet.loadOnStartUp() < 0) {
			throw new IllegalStateException("proxied servlet ["+PROXY_SERVLET+"] must have load on startup enabled!");
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Map<String, Servlet> dynamicServlets = applicationContext.getBeansOfType(DynamicRegistration.Servlet.class);
		cmisWebServiceServlet = dynamicServlets.get(PROXY_SERVLET);
	}

	@Autowired
	@Override
	public void setServletManager(ServletManager servletManager) {
		if(ACTIVE) {
			super.setServletManager(servletManager);
			ConfigurationWarnings.addGlobalWarning(log, "CmisProxy has been deprecated. Please enable the MtomFilter [cmis.mtomfilter.active=true] and use default cmis endpoints instead!");
		}
	}
}
