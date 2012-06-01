/*
 * $Log: XmlSwitch.java,v $
 * Revision 1.26  2012-06-01 10:52:50  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.25  2011/11/30 13:51:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.23  2008/12/30 17:01:12  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added configuration warnings facility (in Show configurationStatus)
 *
 * Revision 1.22  2008/08/06 16:40:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for flexible monitoring
 *
 * Revision 1.21  2007/12/10 10:13:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fix configuration of notFoundForward (no exception when not found)
 *
 * Revision 1.20  2006/01/05 14:36:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.19  2005/12/29 15:19:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.18  2005/10/24 09:20:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made namespaceAware an attribute of AbstractPipe
 *
 * Revision 1.17  2005/10/17 11:35:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * throw ConfigurationException when stylesheet not found
 *
 * Revision 1.16  2005/06/13 11:46:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected version-string
 *
 * Revision 1.15  2005/06/13 11:45:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attribute 'namespaceAware'
 *
 * Revision 1.14  2005/05/03 16:00:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected typo
 *
 * Revision 1.13  2005/04/26 09:22:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added SessionVariable facility (by Peter Leeuwenburgh)
 *
 */
package nl.nn.adapterframework.pipes;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;


/**
 * Selects an exitState, based on either the content of the input message, by means
 * of a XSLT-stylesheet, the content of a session variable or, by default, by returning the name of the root-element.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.XmlSwitch</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setDurationThreshold(long) durationThreshold}</td><td>if durationThreshold >=0 and the duration (in milliseconds) of the message processing exceeded the value specified the message is logged informatory</td><td>-1</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNamespaceAware(boolean) namespaceAware}</td><td>controls namespace-awareness of transformation</td><td>application default</td></tr>
 * <tr><td>{@link #setServiceSelectionStylesheetFilename(String) serviceSelectionStylesheetFilename}</td><td>stylesheet may return a String representing the forward to look up</td><td><i>a stylesheet that returns the name of the root-element</i></td></tr>
 * <tr><td>{@link #setXpathExpression(String) xpathExpression}</td><td>XPath-expression that returns a String representing the forward to look up</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSessionKey(String) sessionKey}</td><td>name of the key in the <code>PipeLineSession</code> to retrieve the input message from. (N.B. same as <code>getInputFromSessionKey</code>)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNotFoundForwardName(String) notFoundForwardName}</td><td>Forward returned when the pipename derived from the stylesheet could not be found.</i></td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>&lt;name of the root-element&gt;</td><td>default</td></tr>
 * <tr><td>&lt;result of transformation&gt</td><td>when {@link #setServiceSelectionStylesheetFilename(String) serviceSelectionStylesheetFilename} or {@link #setXpathExpression(String) xpathExpression} is specified</td></tr>
 * </table>
 * </p>
 * @version Id
 * @author Johan Verrips
 */
public class XmlSwitch extends AbstractPipe {
	public static final String version="$RCSfile: XmlSwitch.java,v $ $Revision: 1.26 $ $Date: 2012-06-01 10:52:50 $";

	public static final String XML_SWITCH_FORWARD_FOUND_MONITOR_EVENT = "Switch: Forward Found";
	public static final String XML_SWITCH_FORWARD_NOT_FOUND_MONITOR_EVENT = "Switch: Forward Not Found";
	
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
//				throw new ConfigurationException(getLogPrefix(null)+"has a notFoundForwardName attribute. However, this forward ["+getNotFoundForwardName()+"] is not configured.");
				ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
				String msg = getLogPrefix(null)+"has a notFoundForwardName attribute. However, this forward ["+getNotFoundForwardName()+"] is not configured.";
				configWarnings.add(log, msg);
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
					URL stylesheetURL = ClassUtils.getResourceURL(this, getServiceSelectionStylesheetFilename());
					if (stylesheetURL==null) {
						throw new ConfigurationException(getLogPrefix(null) + "cannot find stylesheet ["+getServiceSelectionStylesheetFilename()+"]");
					}
					transformerPool = new TransformerPool(stylesheetURL);
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
		registerEvent(XML_SWITCH_FORWARD_FOUND_MONITOR_EVENT);
		registerEvent(XML_SWITCH_FORWARD_NOT_FOUND_MONITOR_EVENT);
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
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		String forward="";
	    String sInput=(String) input;
	    PipeForward pipeForward=null;

		if (StringUtils.isNotEmpty(getSessionKey())) {
			sInput = (String) session.get(sessionKey);
		}
		if (transformerPool!=null) {
			ParameterList parameterList = null;
			ParameterResolutionContext prc = new ParameterResolutionContext(sInput, session, isNamespaceAware()); ;	
			try {
				Map parametervalues = null;
				if (getParameterList()!=null) {
					parameterList =  getParameterList();
					parametervalues = prc.getValueMap(parameterList);
				}
	           	forward = transformerPool.transform(prc.getInputSource(), parametervalues);
			}
		    catch (Throwable e) {
		   	    throw new PipeRunException(this, getLogPrefix(session)+"got exception on transformation", e);
		    }
		} else {
			forward=sInput;
		}

		log.debug(getLogPrefix(session)+ "determined forward ["+forward+"]");

		if (findForward(forward) != null) {
			throwEvent(XML_SWITCH_FORWARD_FOUND_MONITOR_EVENT);
			pipeForward=findForward(forward);
		}
		else {
			log.info(getLogPrefix(session)+"determined forward ["+forward+"], which is not defined. Will use ["+getNotFoundForwardName()+"] instead");
			throwEvent(XML_SWITCH_FORWARD_NOT_FOUND_MONITOR_EVENT);
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
