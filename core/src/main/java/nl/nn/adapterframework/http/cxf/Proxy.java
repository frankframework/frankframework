package nl.nn.adapterframework.http.cxf;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import nl.nn.adapterframework.http.HttpServletBase;
import nl.nn.adapterframework.lifecycle.DynamicRegistration;
import nl.nn.adapterframework.lifecycle.IbisInitializer;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

@IbisInitializer
public class Proxy extends HttpServletBase implements InitializingBean, ApplicationContextAware {

	private Logger log = LogUtil.getLogger(this);
	private static final long serialVersionUID = 1L;

	private String proxyServlet = AppConstants.getInstance().getProperty("http.proxy.servlet", "webServices11");
	private Servlet cmisWebServiceServlet = null;

	@Override
	public String getUrlMapping() {
		return "/proxy/cmis/webservices/*";
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path = req.getPathInfo();
		System.out.println(path);
		cmisWebServiceServlet.service(req, resp);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Map<String, Servlet> dynamicServlets = applicationContext.getBeansOfType(DynamicRegistration.Servlet.class);
		cmisWebServiceServlet = dynamicServlets.get(proxyServlet);

		if(cmisWebServiceServlet == null) {
			log.warn("unable to find servlet ["+proxyServlet+"]");
		}
	}
}
