/*
 * $Log: XsltPipe.java,v $
 * Revision 1.10  2004-10-05 10:55:59  L190409
 * reworked pooling (using TransformerPool)
 * included functionality of XPathPipe:
 *   added xpathExpression attribute
 *   added sessionKey attribute
 *
 */
package nl.nn.adapterframework.pipes;

import java.io.IOException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

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
 * <tr><td>{@link #setOmitXmlDeclaration(boolean) setOmitXmlDeclaration}</td><td>forse the transformer to omit the xml declaration</td><td>true</td></tr>
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
	public static final String version="$Id: XsltPipe.java,v 1.10 2004-10-05 10:55:59 L190409 Exp $";

	private TransformerPool transformerPool;
	private String xpathExpression=null;
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
				transformerPool = new TransformerPool(XmlUtils.createXPathEvaluatorSource(getXpathExpression(), "text"));
			} 
			catch (TransformerConfigurationException te) {
				throw new ConfigurationException(getLogPrefix(null) + "got error creating transformer from xpathExpression [" + getXpathExpression() + "]", te);
			}
		} 
		else {
			if (!StringUtils.isEmpty(styleSheetName)) {
				try {
					transformerPool = new TransformerPool(ClassUtils.getResourceURL(this, styleSheetName));
				} catch (IOException e) {
					throw new ConfigurationException(getLogPrefix(null) + "cannot retrieve ["+ styleSheetName + "]", e);
				} catch (TransformerConfigurationException te) {
					throw new ConfigurationException(getLogPrefix(null) + "got error creating transformer from file [" + styleSheetName + "]", te);
				}
			} else {
				throw new ConfigurationException(getLogPrefix(null) + "either xpathExpression or styleSheetName must be specified");
			}
		}
		if (isOmitXmlDeclaration()) {
			transformerPool.setIncludeXmlDeclaration(false);
		}
		else {
			transformerPool.setIncludeXmlDeclaration(true);
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
    
    Transformer transformer = null;
    try {
		transformer = transformerPool.getTransformer();
		if (getParameterList()!=null) {
			XmlUtils.setTransformerParameters(transformer, new ParameterResolutionContext(input, session).getValues(getParameterList()));
		}
        String stringResult = XmlUtils.transformXml(transformer, (String) input);
		if (StringUtils.isEmpty(getSessionKey())){
			return new PipeRunResult(getForward(), stringResult);
		} else {
			session.put(getSessionKey(), stringResult);
			return new PipeRunResult(getForward(), input);
		}
    } 
    catch (TransformerException te) {
        PipeRunException pre = new PipeRunException(this, getLogPrefix(session)+" cannot transform input", te);
        try {
        	transformerPool.invalidateTransformer(transformer);
            log.debug(getLogPrefix(session)+ " transformer was removed from pool as an error occured on the last transformation");
        } catch (Throwable e2) {
            log.error(getLogPrefix(session)+ "got error on removing transformer from pool", e2);
        }
        throw pre;
    } 
    catch (Exception ie) {
		throw new PipeRunException(this, getLogPrefix(session)+ "Exception on transforming input", ie);
    }
    finally {
    	if (transformer != null) {
    		try {
    			transformerPool.releaseTransformer(transformer);
    		} catch(Exception e) {
    			log.warn(getLogPrefix(session)+"exception returning transformer to pool",e);
    		};
    	}
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

}
