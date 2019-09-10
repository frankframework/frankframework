package nl.nn.adapterframework.http;

import javax.servlet.http.HttpServlet;

import nl.nn.adapterframework.lifecycle.DynamicRegistration;

/**
 * Base class for @IbisInitializer capable servlets
 * 
 * @author Niels Meijer
 *
 */
public abstract class HttpServletBase extends HttpServlet implements DynamicRegistration.Servlet {

	private static final long serialVersionUID = 1L;

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public int loadOnStartUp() {
		return -1;
	}

	@Override
	public HttpServlet getServlet() {
		return this;
	}

	@Override
	public String[] getRoles() {
		return null;
	}

	
}
