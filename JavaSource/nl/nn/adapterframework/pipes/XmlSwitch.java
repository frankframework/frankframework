package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.XmlUtils;

import javax.xml.transform.Transformer;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;


import java.io.IOException;


/**
 * Selects an exitState, based on either the contents of the input message, by means
 * of a XSLT-stylesheet, or, by default, by returning the name of the root-element.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setServiceSelectionStylesheetFilename(String) serviceSelectionStylesheetFilename}</td><td>stylesheet may return a String representing the forward to look up</td><td><i>a stylesheet that returns the name of the root-element</i></td></tr>
 * <tr><td>{@link #setXpathExpression(String) xpathExpression}</td><td>XPath-expression that returns a String representing the forward to look up</td><td></td></tr>
 * <tr><td>{@link #setNotFoundForwardName(String) setNotFoundForwardName(String)}</td><td>Forward returned when the pipename derived from the stylesheet could not be found.</i></td></tr>
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
	public static final String version="$Id: XmlSwitch.java,v 1.7 2004-08-31 13:19:58 a1909356#db2admin Exp $";
	
    private static final String DEFAULT_SERVICESELECTION_XSLT = XmlUtils.XSLT_GETROOTNODENAME;
	private ObjectPool transformerPool;
	private String xpathExpression=null;
    private String serviceSelectionStylesheetFilename=null;
    private String notFoundForwardName=null;

	/**
	 * If no {@link #setServiceSelectionStylesheetFilename(String) serviceSelectionStylesheetFilename} is specified, the
	 * switch uses the root node. 
	 */
	public void configure() throws ConfigurationException {
		if (getNotFoundForwardName()!=null) {
			if (findForward(getNotFoundForwardName())==null){
				throw new ConfigurationException(getLogPrefix(null)+"has a notFoundForwardName attribute. However, this forward ["+getNotFoundForwardName()+"] is not configured.");
			}
		}

		transformerPool = new GenericObjectPool(new BasePoolableObjectFactory() {
			public Object makeObject() throws Exception {
				// create a transformer for the service selection
				if (!StringUtils.isEmpty(getXpathExpression())) {
					if (serviceSelectionStylesheetFilename != null) {
						throw new ConfigurationException(getLogPrefix(null) + "cannot have both an xpathExpression and a serviceSelectionStylesheetFilename specified");
					}
					try {
						return XmlUtils.createXPathEvaluator(getXpathExpression(), "text");
					} 
					catch (javax.xml.transform.TransformerConfigurationException te) {
						throw new ConfigurationException(getLogPrefix(null) + "got error creating transformer from xpathExpression [" + getXpathExpression() + "]", te);
					}
				} 
				else {
					if (serviceSelectionStylesheetFilename != null) {
						try {
							return XmlUtils.createTransformer(ClassUtils.getResourceURL(this, serviceSelectionStylesheetFilename));
						} 
						catch (IOException e) {
							throw new ConfigurationException(getLogPrefix(null) + "cannot retrieve ["+ serviceSelectionStylesheetFilename + "]", e);
						} 
						catch (javax.xml.transform.TransformerConfigurationException te) {
							throw new ConfigurationException(getLogPrefix(null) + "got error creating transformer from file [" + serviceSelectionStylesheetFilename + "]", te);
						}
					} 
					else {
						try {
							// create a transformer that looks to the root node 
							return XmlUtils.createTransformer(DEFAULT_SERVICESELECTION_XSLT);
						} 
						catch (javax.xml.transform.TransformerConfigurationException te) {
							throw new ConfigurationException(getLogPrefix(null) + "got error creating transformer from string [" + DEFAULT_SERVICESELECTION_XSLT + "]", te);
						}
					}
				}
			}
		});
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
	
		Transformer t = null;
		try {
	 		t = openTransformer();
            forward = XmlUtils.transformXml(t, sInput);
            log.debug(getLogPrefix(session)+ "determined forward ["+forward+"]");

			if (findForward(forward) != null) 
				pipeForward=findForward(forward);
			else {
				log.info(getLogPrefix(session)+"determined forward ["+forward+"], which is not defined. Will use ["+getNotFoundForwardName()+"] instead");
				pipeForward=findForward(getNotFoundForwardName());
			}
		}
	    catch (Throwable e) {
		    try {
			    configure();
			    start();
			    log.debug(getLogPrefix(session)+ ": transformer was reinitialized as an error occured on the last transformation");
		    } 
		    catch (Throwable e2) {
			    log.error("Pipe [" + getName() + "] got error on reinitializing the transformer", e2);
		    }
	   	    throw new PipeRunException(this, getLogPrefix(null)+"got exception on transformation", e);
	    }
		finally {
			try { closeTransformer(t); } catch(Exception e) {}
		}
		
		if (pipeForward==null) {
			  throw new PipeRunException (this, getLogPrefix(null)+"cannot find forward or pipe named ["+forward+"]");
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

}
