/*
 * $Log: ActionBase.java,v $
 * Revision 1.7.2.1  2008-05-22 14:36:57  europe\L190409
 * sync from HEAD
 *
 * Revision 1.8  2008/05/22 07:29:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added error and warn methods
 *
 * Revision 1.7  2007/12/10 10:24:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added getAndSetProperty
 *
 * Revision 1.6  2007/10/16 09:12:28  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Merge with changes from EJB branch in preparation for creating new EJB brance
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
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.webcontrol.ConfigurationServlet;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionError;
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
	public static final String version="$RCSfile: ActionBase.java,v $ $Revision: 1.7.2.1 $ $Date: 2008-05-22 14:36:57 $";
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

	public String getAndSetProperty(HttpServletRequest request, DynaActionForm form, String propertyName) {
		return getAndSetProperty(request, form, propertyName, "");
	}

	public String getAndSetProperty(HttpServletRequest request, DynaActionForm form, String propertyName, String defaultValue) {
		String result=request.getParameter(propertyName);
		if (StringUtils.isNotEmpty(result)) {
			form.set(propertyName,result);
			log.debug("set property ["+propertyName+"] to ["+result+"] from request");
		} else {
			result=(String)form.get(propertyName);
			if (StringUtils.isNotEmpty(result)) {
				log.debug("get property ["+propertyName+"] value ["+result+"] from form");
			} else {
				if (StringUtils.isNotEmpty(defaultValue)) {
					result=defaultValue;
					form.set(propertyName,result);
					log.debug("get property ["+propertyName+"] value ["+result+"] from default");
				} else {
					log.debug("get property ["+propertyName+"] value empty, no default");
				}
			}
		}
		return result;
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
        String attributeKey=AppConstants.getInstance().getProperty(ConfigurationServlet.KEY_MANAGER);
        ibisManager = (IbisManager) getServlet().getServletContext().getAttribute(attributeKey);
        log.debug("retrieved ibisManager ["+ClassUtils.nameOf(ibisManager)+"]["+ibisManager+"] from servlet context attribute ["+attributeKey+"]");
        // TODO: explain why this shouldn't happen too early
        config = ibisManager.getConfiguration(); // NB: Hopefully this doesn't happen too early on in the game
        log = LogUtil.getLogger(this); // logging category for this class
 
        if (null == config) {
            log.info("initAction(): configuration not present in context. Configuration probably has errors. see log");
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
    
	protected void warn(String message) {
		warn(message,null);
	}

	protected void warn(Throwable t) {
		warn(null,t);
	}
	protected void warn(String message, Throwable t) {
		if (t!=null) {
			if (StringUtils.isEmpty(message)) {
				message=ClassUtils.nameOf(t)+" "+t.getMessage();
			} else {
				message+=": "+ClassUtils.nameOf(t)+" "+t.getMessage();
			}
		}
		log.warn(message);
		errors.add("", new ActionError("errors.generic", XmlUtils.encodeChars(message)));
	}


	protected void error(String message, Throwable t) {
		error("","errors.generic",message,t);
	}

	protected void error(String category, String message, Throwable t) {
		error("",category,message,t);
	}

	protected void error(String key, String category, String message, Throwable t) {
		log.error(message,t);
		if (t!=null) {
			if (StringUtils.isEmpty(message)) {
				message=ClassUtils.nameOf(t)+" "+t.getMessage();
			} else {
				message+=": "+ClassUtils.nameOf(t)+" "+t.getMessage();
			}
		}
		errors.add(key, new ActionError(category, XmlUtils.encodeChars(message)));
	}
}
