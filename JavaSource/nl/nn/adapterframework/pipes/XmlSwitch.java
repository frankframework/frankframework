package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.XmlUtils;

import javax.xml.transform.Transformer;
import java.io.IOException;


/**
 * Selects an exitState, based on either the contents of the input message, by means
 * of a XSLT-stylesheet, or, by default, by returning the name of the root-element.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setServiceSelectionStylesheetFilename(String) serviceSelectionStylesheetFilename}</td><td>stylesheet may return a String representing the forward to look up</td><td><i>a stylesheet that returns the name of the root-element</i></td></tr>
 * <tr><td>{@link #setNotFoundPipeName(String) setNotFoundPipeName(String)}</td><td>Pipename returned when the pipename derrived from the stylesheet could not be found.</i></td></tr>
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
	public static final String version="$Id: XmlSwitch.java,v 1.4 2004-04-06 12:57:37 NNVZNL01#L180564 Exp $";
	
	    private static final String DEFAULT_SERVICESELECTION_XSLT = XmlUtils.XSLT_GETROOTNODENAME;
	    private Transformer serviceSelectionTransformer;
	    private String serviceSelectionStylesheetFilename=null;
	    private String notFoundForwardName=null;
		/**
		 * return the current transformer
		 * @return Transformer
		 */
	    protected Transformer getServiceSelectionTransformer() {
	    	return serviceSelectionTransformer;
	    }
	
	/**
	 * If no {@link #setServiceSelectionStylesheetFilename(String) serviceSelectionStylesheetFilename} is specified, the
	 * switch uses the root node. 
	 */
	public void configure()
		throws ConfigurationException
	{
		// create a transformer for the service selection
		if (serviceSelectionStylesheetFilename != null)
        {
	        try {
		        serviceSelectionTransformer = XmlUtils.createTransformer(ClassUtils.getResourceURL(this,serviceSelectionStylesheetFilename));
	        } catch (IOException e) {
                throw new ConfigurationException(
                    "Pipe [" + getName() + "] cannot retrieve [" + serviceSelectionStylesheetFilename + "]");
            } catch (javax.xml.transform.TransformerConfigurationException te) {
	            throw new ConfigurationException( "Pipe [" + getName() + "] got error creating transformer from file [" + serviceSelectionStylesheetFilename + "]", te);
            }
  
	        
        } else
        	// create a transformer that looks to the root node 
        {
           try {
		        serviceSelectionTransformer = XmlUtils.createTransformer( DEFAULT_SERVICESELECTION_XSLT);
            } catch (javax.xml.transform.TransformerConfigurationException te) {
	            throw new ConfigurationException( "Pipe [" + getName() + "] got error creating transformer from string [" + DEFAULT_SERVICESELECTION_XSLT + "]", te);
            }
		}
		if (getNotFoundForwardName()!=null) {
			if (findForward(getNotFoundForwardName())==null){
				throw new ConfigurationException("Pipe [" + getName() + "] has a notFoundForwardName attribute. However, this forward ["+getNotFoundForwardName()+"] is not configured.");
			}
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

	try {
 	   if (serviceSelectionTransformer != null) {
            forward = XmlUtils.transformXml(serviceSelectionTransformer, sInput);
            log.debug(getLogPrefix(session)+ "determined forward ["+forward+"]");

        } else {
            log.warn(getLogPrefix(session)+ " cannot determine forward due to lack of serviceSelectionTransformer");
        }
		if (findForward(forward)!=null) 
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
	    } catch (Throwable e2) {
		    log.error("Pipe [" + getName() + "] got error on reinitializing the transformer", e2);
	    }
   	    throw new PipeRunException(this, "Pipe [" + getName() + "] got exception on transformation", e);
    }
	
	if (pipeForward==null) {
		  throw new PipeRunException (this, "cannot find forward or pipe named ["+forward+"]");
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
}
