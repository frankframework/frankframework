package nl.nn.adapterframework.lifecycle.servlets;

//SecurityContextHolder.getContext().getAuthentication(); can be used to retrieve the username (when available)
public interface IAuthenticator {

	void registerServlet(ServletConfiguration config);

	void build();
}
