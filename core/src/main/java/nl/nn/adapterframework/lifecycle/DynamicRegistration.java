package nl.nn.adapterframework.lifecycle;

import javax.servlet.http.HttpServlet;


public interface DynamicRegistration {

	public interface Servlet extends DynamicRegistration {
		public HttpServlet getServletClass();
		public String getUrlMapping();
		public String[] getRoles();
	}

	/**
	 * @return Name of the to-be implemented class
	 */
	public String getName();

	/**
	 * Order in which to automatically instantiate and load the class.</br>
	 * @return <code>0</code> to let the ibis determine, <code>-1</code> to disable
	 */
	public int loadOnStartUp();
}
