
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.XmlUtils;
import java.util.Iterator;

/**
 * Extension of the {@link XmlSwitch XmlSwitch}: an xml switch that can use parameters. The parameters
 * will be used to set them on the transformer instance.
 * @author Johan Verrips
 * @version Id
 */
public class XmlParamSwitch extends XmlSwitch {
	public static final String version="$Id: XmlParamSwitch.java,v 1.1 2004-04-06 12:57:14 NNVZNL01#L180564 Exp $";

	public void configure() throws ConfigurationException {
		super.configure();
		if (this.getMaxThreads()!=1) log.warn("Pipe ["+getName()+"] should have set maxThreads to 1, as the Pipe is NOT threadsafe!");
		
	}
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException{
		String forward="";
		String sInput=(String) input;
		PipeForward pipeForward=null;

		try {
		   if (getServiceSelectionTransformer() != null) {
				getServiceSelectionTransformer().clearParameters();
				Iterator i = getParameterList().iterator();
				while (i.hasNext()) {
					PipeParameter p = (PipeParameter) i.next();
					if (p.getValue(session)!=null) {
						
						getServiceSelectionTransformer().setParameter(
							p.getName(),
							p.getValue(session));

						log.debug(getLogPrefix(session)+"registering parameter ["
								+ p.toString()
								+ "] on transformer");
					} else {
						log.warn(getLogPrefix(session)+"omitting parameter ["+p.getName()+"] as it has a null-value");
					}
				}
				forward = XmlUtils.transformXml(getServiceSelectionTransformer(), sInput);
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
	
		if (pipeForward==null) {
			  throw new PipeRunException (this, "cannot find forward or pipe named ["+forward+"]");
		}
		return new PipeRunResult(pipeForward, input);
	
	}
	

}
