package nl.nn.adapterframework.webcontrol.action;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.util.AppConstants;
import org.apache.log4j.Logger;
import org.apache.struts.action.*;
import org.apache.struts.util.MessageResources;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Locale;


/**
 * Implementation of <strong>Action</strong><br/>
 * This action is ment to be extended by individual actions in the project.
 * <p>$Id: ActionBase.java,v 1.2 2004-02-04 10:02:08 a1909356#db2admin Exp $</p>
 * @author  Johan Verrips
 * @see     org.apache.struts.action.Action
 */

public abstract class ActionBase extends Action {
	public static final String version="$Id: ActionBase.java,v 1.2 2004-02-04 10:02:08 a1909356#db2admin Exp $";
	
    /**
     * log category; set to the current classname
     */
    protected Logger log ; // logging category for this class

    Locale locale;

    MessageResources messageResources;

    ActionErrors errors;

    HttpSession session;

    /**
     *the <code>Configuration</code> object
     * @see nl.nn.adapterframework.configuration.Configuration
     */
    Configuration config;
    ActionMessages messages;

    /**
     * This proc should start with <code>initAction(request)</code>
     * @see Action
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {

        // Forward control to the specified success URI
        log.debug("forward to success");
        return (mapping.findForward("success"));

    }
	public String getCommandIssuedBy(HttpServletRequest request){
    String commandIssuedBy= " remoteHost ["+request.getRemoteHost()+"]";
	commandIssuedBy+=" remoteAddress ["+request.getRemoteAddr()+"]";
	commandIssuedBy+=" remoteUser ["+request.getRemoteUser()+"]";
	return commandIssuedBy;
	}
    /**
     * looks under the session for an attribute named forward. Returns it as an ActionForward
     */
    public ActionForward getDefaultActionForward(HttpServletRequest request) {
        HttpSession session = request.getSession();
        ActionForward definedForward = (ActionForward) session.getAttribute("forward");

        return definedForward;
    }
    /**
     * Gets the full request Uri, that is, the reference suitable for ActionForwards.<br/>
     * Queryparameters of the request are added to it.
     */
    public String getFullRequestUri (HttpServletRequest request) {
            String ctxtPath = request.getContextPath();
            String reqUri = request.getRequestURI();

            reqUri = reqUri.substring(ctxtPath.length(), reqUri.length());
            String queryString = request.getQueryString();

            if (null != queryString) {
                reqUri += "?" + request.getQueryString();
            }
            return reqUri;
          }
protected DynaActionForm getPersistentForm(ActionMapping mapping, ActionForm form, HttpServletRequest request) {
    if (form == null) {
        log.debug(
            " Creating new FormBean under key " + mapping.getAttribute());

        form = new DynaActionForm();

        if ("request".equals(mapping.getScope())) {
            request.setAttribute(mapping.getAttribute(), form);
        } else {
            session.setAttribute(mapping.getAttribute(), form);
        }
    }
    return (DynaActionForm) form;
}
 	/**
 	 * Initializes the fields in the <code>Action</code> for this specific
 	 * application. Most important: it retrieves the Configuration object
 	 * from the servletContext.
 	 * @see nl.nn.adapterframework.configuration.Configuration
 	 */ 
	public void initAction(HttpServletRequest request) {

        locale = getLocale(request);
        messageResources = getResources(request);
        errors = new ActionErrors();

        session = request.getSession();
        config = (Configuration) getServlet().getServletContext().getAttribute(AppConstants.getInstance().getProperty("KEY_CONFIGURATION"));

        log = Logger.getLogger(this.getClass()); // logging category for this class
 
        if (null == config) {
            log.info("configuration not present in context. Configuration probably add errors. see log");
        }



    }
    /**
     * removes formbean <br/>
     * removes what is defined under the Attribute of a mapping from either the
     * request or the session
     */
    public void removeFormBean(ActionMapping mapping, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (mapping.getAttribute() != null) {
            if ("request".equals(mapping.getScope()))
                request.removeAttribute(mapping.getAttribute());
            else
                session.removeAttribute(mapping.getAttribute());
        }
    }
    /**
     * Sets under the session an attribute named forward with an ActionForward to the current
     * request.
     */
    public ActionForward setDefaultActionForward(HttpServletRequest request) {
        HttpSession session = request.getSession();
        // store the request uri with query parameters in an actionforward
        String reqUri=getFullRequestUri(request);
        ActionForward forward = new ActionForward();

        forward.setPath(reqUri);
        log.info("default forward set to :" + reqUri);
        session.setAttribute("forward", forward);
        return forward;
    }
}
