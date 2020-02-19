/*
   Copyright 2013, 2016 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.http.HttpUtils;
import nl.nn.adapterframework.lifecycle.IbisApplicationServlet;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

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
 * @author  Johan Verrips
 * @see     org.apache.struts.action.Action
 */
public abstract class ActionBase extends Action {
	protected Logger log = LogUtil.getLogger(this);
	protected Logger secLog = LogUtil.getLogger("SEC");

    protected Locale locale;
	protected MessageResources messageResources;
	protected ActionErrors errors;
	protected HttpSession session;

	private boolean secLogMessage = AppConstants.getInstance().getBoolean("sec.log.includeMessage", false);
	private boolean writeToSecLog = false;
	private List<String> secLogParamNames = new ArrayList<String>();
	private boolean writeSecLogMessage = false;

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
		if (isWriteToSecLog()) {
			if (secLogMessage && isWriteSecLogMessage()) {
				DynaActionForm dynaActionForm = (DynaActionForm) form;
				String form_message = null;
				try {
					form_message = (String) dynaActionForm.get("message");
				}
				catch(IllegalArgumentException e) {
					try {
						form_message = (String) dynaActionForm.get("query");
					}
					catch(IllegalArgumentException e2) {
						form_message = "could not derive message or query from DynaForm";
					}
				}

				secLog.info(HttpUtils.getExtendedCommandIssuedBy(request, secLogParamNames, form_message));
			}
			else {
				secLog.info(HttpUtils.getExtendedCommandIssuedBy(request, secLogParamNames));
			}
		}
		return executeSub(mapping, form, request, response);
	}

    public abstract ActionForward executeSub(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException;
    
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
		IbisContext ibisContext = IbisApplicationServlet.getIbisContext(getServlet().getServletContext());
		ibisManager = null;
		if (ibisContext != null) {
			ibisManager = ibisContext.getIbisManager();
		} 
		if (ibisManager==null) {
			log.warn("Could not retrieve ibisManager from context");
		} else {
			log.debug("retrieved ibisManager ["+ClassUtils.nameOf(ibisManager)+"]["+ibisManager+"] from servlet context");
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

	public void setWriteToSecLog(boolean b) {
		writeToSecLog = b;
	}
	
	public boolean isWriteToSecLog() {
		return writeToSecLog;
	}

	public void addSecLogParamName(String paramName) {
		secLogParamNames.add(paramName);
	}

	public void setWriteSecLogMessage(boolean b) {
		writeSecLogMessage = b;
	}
	
	public boolean isWriteSecLogMessage() {
		return writeSecLogMessage;
	}
}
