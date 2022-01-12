package nl.nn.adapterframework.extensions.cmis.mtom;

import java.io.IOException;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.context.ServletContextAware;

import nl.nn.adapterframework.lifecycle.IbisInitializer;
import nl.nn.adapterframework.util.AppConstants;

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
	public void afterPropertiesSet() throws Exception {
		FilterRegistration.Dynamic filter = servletContext.addFilter("CmisMtomFilter", this);
		EnumSet<DispatcherType> dispatcherTypes = EnumSet.of(DispatcherType.REQUEST);
		filter.addMappingForServletNames(dispatcherTypes, true, "WebServices10", "WebServices11");
	}
}
