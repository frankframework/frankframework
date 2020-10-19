package nl.nn.adapterframework.extensions.cmis.mtom;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		MtomRequestWrapper requestWrapper = new MtomRequestWrapper(request); //Turn every request into an MTOM request
		MtomResponseWrapper responseWrapper = new MtomResponseWrapper(response); //Is this required?

		cmisWebServiceServlet.service(requestWrapper, responseWrapper);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if(cmisWebServiceServlet == null) {
			log.warn("unable to find servlet [" + proxyServlet + "]");
			throw new Exception("proxied servlet not found");
		}
		if(cmisWebServiceServlet.loadOnStartUp() < 0) {
			throw new Exception("proxied servlet must have load on startup enabled!");
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Map<String, Servlet> dynamicServlets = applicationContext.getBeansOfType(DynamicRegistration.Servlet.class);
		cmisWebServiceServlet = dynamicServlets.get(proxyServlet);
	}

}
