
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.*;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.XmlUtils;
import java.util.Iterator;

import javax.xml.transform.Transformer;

/**
 * Extension of the {@link XmlSwitch XmlSwitch}: an xml switch that can use parameters. The parameters
 * will be used to set them on the transformer instance.
 * @author Johan Verrips
 * @version Id
 */
public class XmlParamSwitch extends XmlSwitch {
	public static final String version="$Id: XmlParamSwitch.java,v 1.5 2004-08-31 13:20:58 a1909356#db2admin Exp $";

	public void configure() throws ConfigurationException {
		super.configure();
	}
	
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException{
		String forward="";
		String sInput=(String) input;
		PipeForward pipeForward=null;

		Transformer transformer = null;
		try {
			transformer = openTransformer();
		   	if (transformer != null) {
				transformer.clearParameters();
				ParameterValueResolver resolver = new ParameterValueResolver(input, session);
				Iterator i = getParameterList().iterator();
				while (i.hasNext()) {
					Parameter p = (Parameter) i.next();
					Object value = resolver.getRawValue(p); 
					
					if (value != null) {
						transformer.setParameter(p.getName(), value);
						log.debug(getLogPrefix(session)+"registering parameter [" + p.toString()+ "] on transformer");
					} 
					else {
						log.warn(getLogPrefix(session)+"omitting parameter ["+p.getName()+"] as it has a null-value");
					}
				}
				forward = XmlUtils.transformXml(transformer, sInput);
				log.debug(getLogPrefix(session)+ "determined forward ["+forward+"]");

			} else {
				log.warn(getLogPrefix(session)+ " cannot determine forward due to lack of serviceSelectionTransformer");
			}
			if (findForward(forward)!=null) {			
				pipeForward=findForward(forward);
			}
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
		finally {
			try { closeTransformer(transformer); } catch(Exception e) {}
		}
	
		if (pipeForward==null) {
			  throw new PipeRunException (this, "cannot find forward or pipe named ["+forward+"]");
		}
		return new PipeRunResult(pipeForward, input);
	}
	

}
