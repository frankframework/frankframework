package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.*;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.XmlUtils;
import java.util.Iterator;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import java.io.IOException;

/**
 * {@link XsltPipe XsltPipe} aware of parameters. Beware: this parameter aware implementation for xslt processing is NOT 
 * thread-safe. Therefore, <code>maxThreads</code> should be set to 1.
 * @author Johan Verrips
 * @version Id
 */
public class XsltParamPipe extends XsltPipe {
	public static final String version = "$Id: XsltParamPipe.java,v 1.5 2004-08-31 12:54:31 a1909356#db2admin Exp $";

	public void configure() throws ConfigurationException {
		super.configure();
	}
	/**
	 * Here the actual transforming is done. Under weblogic the transformer object becomes
	 * corrupt when a not-well formed xml was handled. The transformer is then re-initialized
	 * via the configure() and start() methods.
	 */
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		if (!(input instanceof String)) {
			throw new PipeRunException(this, "Pipe [" + getName() + "] got an invalid type as input, expected String, got " + input.getClass().getName());
		}
		// Apply parameters
		Transformer transformer = null;
		try {
			transformer = openTransformer();
			transformer.clearParameters();

			ParameterValueResolver resolver = new ParameterValueResolver(input, session);
			Iterator i = getParameterList().iterator();
			while (i.hasNext()) {
				Parameter p = (Parameter) i.next();
				Object value = resolver.getRawValue(p);

				if (value != null) {
					transformer.setParameter(p.getName(), value);
					log.debug(getLogPrefix(session) + "registering parameter [" + p.toString() + "] on transformer");
				}
				else {
					log.warn(getLogPrefix(session) + "omitting parameter [" + p.getName() + "] as it has a null-value");
				}
			}
			String stringResult = XmlUtils.transformXml(transformer, (String) input);
			return new PipeRunResult(getForward(), stringResult);
		}
		catch (TransformerException te) {
			PipeRunException pre = new PipeRunException(this, "TransformerException while transforming [" + input + "]", te);
			try {
				configure();
				start();
				log.debug(getLogPrefix(session) + " transformer was reinitialized as an error occured on the last transformation");
			}
			catch (Throwable e2) {
				log.error(getLogPrefix(session) + "got error on reinitializing the transformer", e2);
			}
			throw pre;
		}
		catch (Exception e) {
			PipeRunException prei = new PipeRunException(this, "Exception while transforming [" + input + "]", e);
			throw prei;
		}
		finally {
			if (transformer != null) {
				try { closeTransformer(transformer); } catch(Exception e) {};
			}
		}
	}
}
