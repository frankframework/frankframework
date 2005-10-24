/*
 * $Log: XsltPipe.java,v $
 * Revision 1.19  2005-10-24 09:20:18  europe\L190409
 * made namespaceAware an attribute of AbstractPipe
 *
 * Revision 1.18  2005/08/09 15:55:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid nullpointer in configure when stylesheet does not exist
 *
 * Revision 1.17  2005/06/20 09:03:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added outputType attribute (for xpath-expressions)
 *
 * Revision 1.16  2005/06/13 11:46:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected version-string
 *
 * Revision 1.15  2005/06/13 11:45:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attribute 'namespaceAware'
 *
 * Revision 1.14  2005/01/10 08:56:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Xslt parameter handling by Maps instead of by Ibis parameter system
 *
 * Revision 1.13  2004/10/19 15:27:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved transformation to pool
 *
 * Revision 1.12  2004/10/14 16:11:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ParameterResolutionContext from Object,Hashtable to String, PipelineSession
 *
 * Revision 1.11  2004/10/12 15:13:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed handling of output of  XmlDeclaration
 *
 * Revision 1.10  2004/10/05 10:55:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reworked pooling (using TransformerPool)
 * included functionality of XPathPipe:
 *   added xpathExpression attribute
 *   added sessionKey attribute
 *
 */
package nl.nn.adapterframework.pipes;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
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


/**
 * Perform an XSLT transformation with a specified stylesheet.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td><td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setStyleSheetName(String) styleSheetName}</td><td>stylesheet to apply to the input message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setXpathExpression(String) xpathExpression}</td><td>alternatively: XPath-expression to create stylesheet from</td><td></td></tr>
 * <tr><td>{@link #setOutputType(String) outputType}</td><td>either 'text' or 'xml'. Only valid for xpathExpression</td><td>text</td></tr>
 * <tr><td>{@link #setOmitXmlDeclaration(boolean) omitXmlDeclaration}</td><td>force the transformer generated from the XPath-expression to omit the xml declaration</td><td>true</td></tr>
 * <tr><td>{@link #setNamespaceAware(boolean) namespaceAware}</td><td>controls namespace-awareness of transformation</td><td>application default</td></tr>
 * <tr><td>{@link #setSessionKey(String) sessionKey}</td><td>If specified, the result is put 
 * in the PipeLineSession under the specified key, and the result of this pipe will be 
 * the same as the input (the xml). If NOT specified, the result of the xpath expression 
 * will be the result of this pipe</td><td>&nbsp;</td></tr>
 * </table>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the pipe will be applied to the created transformer</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * @version Id
 * @author Johan Verrips
 */

public class XsltPipe extends FixedForwardPipe {
	public static final String version="$RCSfile: XsltPipe.java,v $ $Revision: 1.19 $ $Date: 2005-10-24 09:20:18 $";

	private TransformerPool transformerPool;
	private String xpathExpression=null;
	private String outputType="text";
	private String styleSheetName;
	private boolean omitXmlDeclaration=true;
	private String sessionKey=null;

	
	
	/**
	 * The <code>configure()</code> method instantiates a transformer for the specified
	 * XSL. If the stylesheetname cannot be accessed, a ConfigurationException is thrown.
	 * @throws ConfigurationException
	 */
	public void configure() throws ConfigurationException {
	    super.configure();
	
		if (!StringUtils.isEmpty(getXpathExpression())) {
			if (!StringUtils.isEmpty(styleSheetName)) {
				throw new ConfigurationException(getLogPrefix(null) + "cannot have both an xpathExpression and a styleSheetName specified");
			}
			try {
				transformerPool = new TransformerPool(XmlUtils.createXPathEvaluatorSource("",getXpathExpression(), getOutputType(), !isOmitXmlDeclaration()));
			} 
			catch (TransformerConfigurationException te) {
				throw new ConfigurationException(getLogPrefix(null) + "got error creating transformer from xpathExpression [" + getXpathExpression() + "]", te);
			}
		} 
		else {
			if (!StringUtils.isEmpty(styleSheetName)) {
				URL resource = ClassUtils.getResourceURL(this, styleSheetName);
				if (resource==null) {
					throw new ConfigurationException(getLogPrefix(null) + "cannot find ["+ styleSheetName + "]"); 
				}
				try {
					transformerPool = new TransformerPool(resource);
				} catch (IOException e) {
					throw new ConfigurationException(getLogPrefix(null) + "cannot retrieve ["+ styleSheetName + "], resource ["+resource.toString()+"]", e);
				} catch (TransformerConfigurationException te) {
					throw new ConfigurationException(getLogPrefix(null) + "got error creating transformer from file [" + styleSheetName + "]", te);
				}
			} else {
				throw new ConfigurationException(getLogPrefix(null) + "either xpathExpression or styleSheetName must be specified");
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
	 * Here the actual transforming is done. Under weblogic the transformer object becomes
	 * corrupt when a not-well formed xml was handled. The transformer is then re-initialized
	 * via the configure() and start() methods.
	 */
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
	    if (!(input instanceof String)) {
	        throw new PipeRunException(this,
	            getLogPrefix(session)+"got an invalid type as input, expected String, got "
	                + input.getClass().getName());
	    }
	    
		ParameterList parameterList = null;
		ParameterResolutionContext prc = new ParameterResolutionContext((String)input, session, isNamespaceAware()); 
	    try {
			Map parametervalues = null;
			if (getParameterList()!=null) {
				parameterList =  getParameterList();
				parametervalues = prc.getValueMap(parameterList);
			}
			
	        String stringResult = transformerPool.transform(prc.getInputSource(), parametervalues); 
			if (StringUtils.isEmpty(getSessionKey())){
				return new PipeRunResult(getForward(), stringResult);
			} else {
				session.put(getSessionKey(), stringResult);
				return new PipeRunResult(getForward(), input);
			}
	    } 
	    catch (Exception e) {
	        throw new PipeRunException(this, getLogPrefix(session)+" Exception on transforming input", e);
	    } 
	}

	/**
	 * Specify the stylesheet to use
	 */
	public void setStyleSheetName(String stylesheetName){
		this.styleSheetName=stylesheetName;
	}

	/**
	 * set the "omit xml declaration" on the transfomer. Defaults to true.
	 * @return true or false
	 */
	public boolean isOmitXmlDeclaration() {
		return omitXmlDeclaration;
	}

	public void setOmitXmlDeclaration(boolean b) {
		omitXmlDeclaration = b;
	}

	public String getXpathExpression() {
		return xpathExpression;
	}

	public void setXpathExpression(String string) {
		xpathExpression = string;
	}
	/**
	 * The name of the key in the <code>PipeLineSession</code> to store the input in
	 * @see nl.nn.adapterframework.core.PipeLineSession
	 */
	public String getSessionKey() {
		return sessionKey;
	}
	/**
	 * The name of the key in the <code>PipeLineSession</code> to store the input in
	 * @see nl.nn.adapterframework.core.PipeLineSession
	 */
	public void setSessionKey(String newSessionKey) {
		sessionKey = newSessionKey;
	}

	public String getOutputType() {
		return outputType;
	}

	public void setOutputType(String string) {
		outputType = string;
	}

}
