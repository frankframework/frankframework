package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.XmlUtils;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;

import java.io.IOException;


/**
 * Perform an XSLT transformation with a specified stylesheet.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td><td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setStyleSheetName(String) styleSheetName}</td><td>stylesheet to apply to the input message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOmitXmlDeclaration(boolean) setOmitXmlDeclaration}</td><td>forse the transformer to omit the xml declaration</td><td>true</td></tr>
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
	public static final String version="$Id: XsltPipe.java,v 1.9 2004-08-31 13:19:58 a1909356#db2admin Exp $";

	private ObjectPool transformerPool;
	private String styleSheetName;
	private boolean omitXmlDeclaration=true;
	

	
	
/**
 * The <code>configure()</code> method instantiates a transformer for the specified
 * XSL. If the stylesheetname cannot be accessed, a ConfigurationException is thrown.
 * @throws ConfigurationException
 */
public void configure() throws ConfigurationException {
    super.configure();

    if (styleSheetName==null) {
		throw new ConfigurationException(getLogPrefix(null)+"is not properly configured: styleSheetName is null");
	}
    try {
    	transformerPool = new GenericObjectPool(new BasePoolableObjectFactory() {
			public Object makeObject() throws Exception {
				Transformer t = XmlUtils.createTransformer(ClassUtils.getResourceURL(this, styleSheetName));
				if (isOmitXmlDeclaration())
					t.setOutputProperty("omit-xml-declaration", "yes");
				else 
					t.setOutputProperty("omit-xml-declaration","no");
					
				return t;
  			}
		});
    } catch (Exception e) {
        throw new ConfigurationException(getLogPrefix(null)+"exception in configure", e);
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
		transformer = openTransformer();
        String stringResult = XmlUtils.transformXml(transformer, (String) input);
		return new PipeRunResult(getForward(), stringResult);
    } 
    catch (TransformerException te) {
        PipeRunException pre = new PipeRunException(this, getLogPrefix(session)+" cannot transform input", te);
        try {
            configure();
            start();
            log.debug(
                 getLogPrefix(session)
                + " transformer was reinitialized as an error occured on the last transformation");
        } catch (Throwable e2) {
            log.error(getLogPrefix(session)+ "got error on reinitializing the transformer", e2);
        }
        throw pre;
    } 
    catch (Exception ie) {
		throw new PipeRunException(this, getLogPrefix(session)+ "Exception on transforming input", ie);
    }
    finally {
    	if (transformer != null) {
    		try { closeTransformer(transformer); } catch(Exception e) {};
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
	 * Get a transformer from the pool 
	 */
	public Transformer openTransformer() throws Exception {
		return (Transformer)transformerPool.borrowObject();
	}

	/**
	 * Return the transformer to the pool
	 */
	public void closeTransformer(Transformer t) throws Exception {
		transformerPool.returnObject(t);
	}

	/**
	 * set the "omit xml declaration" on the transfomer. Defaults to true.
	 * @return true or false
	 */
	public boolean isOmitXmlDeclaration() {
		return omitXmlDeclaration;
	}

	/**
	 * @param b boolean.
	 */
	public void setOmitXmlDeclaration(boolean b) {
		omitXmlDeclaration = b;
	}

}
