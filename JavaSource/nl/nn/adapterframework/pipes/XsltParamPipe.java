package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.XmlUtils;
import java.util.Iterator;

import javax.xml.transform.TransformerException;
import java.io.IOException;


/**
 * {@link XsltPipe XsltPipe} aware of parameters. Beware: this parameter aware implementation for xslt processing is NOT 
 * thread-safe. Therefore, <code>maxThreads</code> should be set to 1.
 * @author Johan Verrips
 * @version Id
 */
public class XsltParamPipe extends XsltPipe {
	public static final String version="$Id: XsltParamPipe.java,v 1.2 2004-04-06 12:57:53 NNVZNL01#L180564 Exp $";

	public void configure() throws ConfigurationException {
		super.configure();
		if (this.getMaxThreads()!=1) log.warn("Pipe ["+getName()+"] should have set maxThreads to 1, as the Pipe is NOT threadsafe!");
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
		// Apply parameters
		getTransformer().clearParameters();
		try {
					Iterator i = this.getParameterList().iterator();
					while (i.hasNext()) {
						PipeParameter p = (PipeParameter) i.next();
						if (p.getValue(session)!=null) {
						
							getTransformer().setParameter(
								p.getName(),
								p.getValue(session));

							log.debug(getLogPrefix(session)+"registering parameter ["
									+ p.toString()
									+ "] on transformer");
						} else {
							log.warn(getLogPrefix(session)+"omitting parameter ["+p.getName()+"] as it has a null-value");
						}

					}
		

			stringResult = XmlUtils.transformXml(getTransformer(), (String) input);

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
}
