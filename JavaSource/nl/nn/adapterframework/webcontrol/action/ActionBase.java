/*
 * $Log: ActionBase.java,v $
 * Revision 1.4.4.3  2007-10-10 14:30:38  europe\L190409
 * synchronize with HEAD (4.8-alpha1)
 *
 * Revision 1.5  2007/10/10 07:29:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * execute control via IbisManager
 *
 * Revision 1.4  2007/02/12 14:34:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.action.DynaActionForm;
import org.apache.struts.util.MessageResources;


/**
 * Implementation of <strong>Action</strong><br/>.
 * 
 * This action is ment to be extended by individual actions in the project.
 * 
 * @version Id
 * @author  Johan Verrips
 * @see     org.apache.struts.action.Action
 */
public abstract class ActionBase extends Action {
	public static final String version="$RCSfile: ActionBase.java,v $ $Revision: 1.4.4.3 $ $Date: 2007-10-10 14:30:38 $";
	protected Logger log = LogUtil.getLogger(this);

    protected Locale locale;
	protected MessageResources messageResources;
	protected ActionErrors errors;
	protected HttpSession session;

    /**
     *the <code>Configuration</code> object
     * @see nl.nn.adapterframework.configuration.Configuration
     */
	protected Configuration config;
    /**
     * the <code>IbisManager</code> object through which 
     * adapters can be controlled.
     */
    protected IbisManager ibisManager;
	protected ActionMessages messages;

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
        ibisManager = (IbisManager) getServlet().getServletContext().getAttribute(AppConstants.getInstance().getProperty("KEY_MANAGER"));
        // TODO: explain why this shouldn't happen too early
        config = ibisManager.getConfiguration(); // NB: Hopefully this doesn't happen too early on in the game
        log = LogUtil.getLogger(this); // logging category for this class
 
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
