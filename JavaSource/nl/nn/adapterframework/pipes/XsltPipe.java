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
import java.io.IOException;


/**
 * Perform an XSLT transformation with a specified stylesheet.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td><td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setStyleSheetName(String) styleSheetName}</td><td>stylesheet to apply to the input message</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * @author Johan Verrips
 */

public class XsltPipe extends FixedForwardPipe {
	public static final String version="$Id: XsltPipe.java,v 1.1 2004-02-04 08:36:06 a1909356#db2admin Exp $";

	private String styleSheetName;
	private Transformer transformer;
/**
 * The <code>configure()</code> method instantiates a transformer for the specified
 * XSL. If the stylesheetname cannot be accessed, a ConfigurationException is thrown.
 * @throws ConfigurationException
 */
public void configure() throws ConfigurationException {
    super.configure();

    if (styleSheetName==null) {
		throw new ConfigurationException("Pipe ["+getName()+"] is not properly configured: styleSheetName is null");
	}
    try {
        transformer =
            XmlUtils.createTransformer(
                ClassUtils.getResourceURL(this, styleSheetName));
        transformer.setOutputProperty("omit-xml-declaration", "yes");
  
    } catch (IOException e) {
        throw new ConfigurationException(
            "Pipe [" + getName() + "] cannot retrieve [" + styleSheetName + "]");
    } catch (TransformerConfigurationException te) {
        throw new ConfigurationException(
            "Pipe ["
                + getName()
                + "] got error creating transformer from file ["
                + styleSheetName
                + "]",
            te);
    } catch (Exception e) {
	    log.error(e);
        throw new ConfigurationException(
            "Pipe [" + getName() + "] got exception: "+e.getMessage(), e);
    }

}
/**
 * Here the actual transforming is done. Under weblogic the transformer object becomes
 * corrupt when a not-well formed xml was handled. The transformer is then re-initialized
 * via the configure() and start() methods.
 */
public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
    String stringResult = null;

    if (!(input instanceof String)) {
        throw new PipeRunException(this,
            "Pipe ["
                + getName()
                + "] got an invalid type as input, expected String, got "
                + input.getClass().getName());
    }
    log.debug("Pipe [" + getName() + "] input [" + input + "]");
    try {

        stringResult = XmlUtils.transformXml(transformer, (String) input);

    } catch (TransformerException te) {
        PipeRunException pre = new PipeRunException(this, "TransformerException while transforming ["+input+"]",te);
            try {
                configure();
                start();
                log.debug(
	                 getLogPrefix(session)
	                + " transformer was reinitialized as an error occured on the last transformation");
            } catch (Throwable e2) {
                log.error(
                    getLogPrefix(session)+ "got error on reinitializing the transformer",
                    e2);
            }
        throw pre;
    } catch (IOException ie) {
        PipeRunException prei = new PipeRunException(this, "IOException while transforming ["+input+"]",ie);
        throw prei;
    }

    return new PipeRunResult(getForward(), stringResult);
}
	/**
	 * Specify the stylesheet to use
	 */
	public void setStyleSheetName(String stylesheetName){
		this.styleSheetName=stylesheetName;
	}
}
