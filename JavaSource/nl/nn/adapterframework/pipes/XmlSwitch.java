/*
 * $Log: XmlSwitch.java,v $
 * Revision 1.14  2005-05-03 16:00:51  L190409
 * corrected typo
 *
 * Revision 1.13  2005/04/26 09:22:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added SessionVariable facility (by Peter Leeuwenburgh)
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

import javax.xml.transform.TransformerConfigurationException;

import org.apache.commons.lang.StringUtils;


import java.io.IOException;
import java.util.Map;


/**
 * Selects an exitState, based on either the content of the input message, by means
 * of a XSLT-stylesheet, the content of a session variable or, by default, by returning the name of the root-element.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setServiceSelectionStylesheetFilename(String) serviceSelectionStylesheetFilename}</td><td>stylesheet may return a String representing the forward to look up</td><td><i>a stylesheet that returns the name of the root-element</i></td></tr>
 * <tr><td>{@link #setXpathExpression(String) xpathExpression}</td><td>XPath-expression that returns a String representing the forward to look up</td><td></td></tr>
 * <tr><td>{@link #setSessionKey(String) sessionKey}</td><td>name of the key in the <code>PipeLineSession</code> to retrieve the input message from</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNotFoundForwardName(String) notFoundForwardName}</td><td>Forward returned when the pipename derived from the stylesheet could not be found.</i></td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>&lt;name of the root-element&gt;</td><td>default</td></tr>
 * <tr><td>&lt;result of transformation&gt</td><td>when {@link #setServiceSelectionStylesheetFilename(String) serviceSelectionStylesheetFilename} is specified</td></tr>
 * </table>
 * </p>
 * @version Id
 * @author Johan Verrips
 */
public class XmlSwitch extends AbstractPipe {
	public static final String version="$Id: XmlSwitch.java,v 1.14 2005-05-03 16:00:51 L190409 Exp $";
	
    private static final String DEFAULT_SERVICESELECTION_XPATH = XmlUtils.XPATH_GETROOTNODENAME;
	private TransformerPool transformerPool=null;
	private String xpathExpression=null;
    private String serviceSelectionStylesheetFilename=null;
	private String sessionKey=null;
    private String notFoundForwardName=null;

	/**
	 * If no {@link #setServiceSelectionStylesheetFilename(String) serviceSelectionStylesheetFilename} is specified, the
	 * switch uses the root node. 
	 */
	public void configure() throws ConfigurationException {
		super.configure();
		if (getNotFoundForwardName()!=null) {
			if (findForward(getNotFoundForwardName())==null){
				throw new ConfigurationException(getLogPrefix(null)+"has a notFoundForwardName attribute. However, this forward ["+getNotFoundForwardName()+"] is not configured.");
			}
		}

		if (!StringUtils.isEmpty(getXpathExpression())) {
			if (!StringUtils.isEmpty(getServiceSelectionStylesheetFilename())) {
				throw new ConfigurationException(getLogPrefix(null) + "cannot have both an xpathExpression and a serviceSelectionStylesheetFilename specified");
			}
			try {
				transformerPool = new TransformerPool(XmlUtils.createXPathEvaluatorSource(getXpathExpression(), "text"));
			} 
			catch (TransformerConfigurationException te) {
				throw new ConfigurationException(getLogPrefix(null) + "got error creating transformer from xpathExpression [" + getXpathExpression() + "]", te);
			}
		} 
		else {
			if (!StringUtils.isEmpty(getServiceSelectionStylesheetFilename())) {
				try {
					transformerPool = new TransformerPool(ClassUtils.getResourceURL(this, getServiceSelectionStylesheetFilename()));
				} catch (IOException e) {
					throw new ConfigurationException(getLogPrefix(null) + "cannot retrieve ["+ serviceSelectionStylesheetFilename + "]", e);
				} catch (TransformerConfigurationException te) {
					throw new ConfigurationException(getLogPrefix(null) + "got error creating transformer from file [" + serviceSelectionStylesheetFilename + "]", te);
				}
			} else {
				if (StringUtils.isEmpty(getSessionKey())) {
					try {
						// create a transformer that looks to the root node 
						transformerPool = new TransformerPool(XmlUtils.createXPathEvaluatorSource(DEFAULT_SERVICESELECTION_XPATH, "text"));
					} catch (TransformerConfigurationException te) {
						throw new ConfigurationException(getLogPrefix(null) + "got error creating XPathEvaluator from string [" + DEFAULT_SERVICESELECTION_XPATH + "]", te);
					}
				}
			}
		}
	}
	
	public void start() throws PipeStartException {
		super.start();
		if (transformerPool!=null) {
			try {
				transformerPool.open();
			} catch (Exception e) {
				throw new PipeStartException(getLogPrefix(null)+"cannot start TransformerPool", e);
			}
		}
	}
	
	public void stop() {
		super.stop();
		if (transformerPool!=null) {
			transformerPool.close();
		}
	}


	/**
	 * This is where the action takes place, the switching is done. Pipes may only throw a PipeRunException,
	 * to be handled by the caller of this object.<br/>
	 * As WebLogic has the problem that when an non-well formed XML stream is given to
	 * weblogic.xerces the transformer gets corrupt, on an exception the configuration is done again, so that the
	 * transformer is re-initialized.
	 */
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		String forward="";
	    String sInput=(String) input;
	    PipeForward pipeForward=null;

		if (StringUtils.isNotEmpty(getSessionKey())) {
			sInput = (String) session.get(sessionKey);
		}
		if (transformerPool!=null) {
			ParameterList parameterList = null;
			ParameterResolutionContext prc = null;	
			try {
				Map parametervalues = null;
				if (getParameterList()!=null) {
					parameterList =  getParameterList();
					prc = new ParameterResolutionContext(sInput, session); 
					parametervalues = prc.getValueMap(parameterList);
				}
	           	forward = transformerPool.transform(sInput, parametervalues);
			}
		    catch (Throwable e) {
		   	    throw new PipeRunException(this, getLogPrefix(session)+"got exception on transformation", e);
		    }
		} else {
			forward=sInput;
		}

		log.debug(getLogPrefix(session)+ "determined forward ["+forward+"]");

		if (findForward(forward) != null) 
			pipeForward=findForward(forward);
		else {
			log.info(getLogPrefix(session)+"determined forward ["+forward+"], which is not defined. Will use ["+getNotFoundForwardName()+"] instead");
			pipeForward=findForward(getNotFoundForwardName());
		}
		
		if (pipeForward==null) {
			  throw new PipeRunException (this, getLogPrefix(session)+"cannot find forward or pipe named ["+forward+"]");
		}
		return new PipeRunResult(pipeForward, input);
	}
	
	public String getServiceSelectionStylesheetFilename() {
		return serviceSelectionStylesheetFilename;
	}
	/**
	 * Set the stylesheet to use. The stylesheet should return a <code>String</code>
	 * that indicates the name of the Forward or Pipe to execute.
	 */
	public void setServiceSelectionStylesheetFilename(String newServiceSelectionStylesheetFilename) {
		serviceSelectionStylesheetFilename = newServiceSelectionStylesheetFilename;
	}
	
	public void setNotFoundForwardName(String notFound){
		notFoundForwardName=notFound;
	}
	public String getNotFoundForwardName(){
		return notFoundForwardName;
	}
	
	public String getXpathExpression() {
		return xpathExpression;
	}
	
	/**
	 * Set the xpath expression to evaluate. The evaluation should result in a <code>String</code>
	 * that indicates the name of the Forward or Pipe to execute.
	 */
	public void setXpathExpression(String xpathExpression) {
		this.xpathExpression = xpathExpression;
	}

	public void setSessionKey(String sessionKey){
		this.sessionKey = sessionKey;
	}

	public String getSessionKey(){
		return sessionKey;
	}
}
