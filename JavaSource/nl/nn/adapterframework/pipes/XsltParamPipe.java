/*
 * $Log: XsltParamPipe.java,v $
 * Revision 1.8  2008-12-30 17:01:12  m168309
 * added configuration warnings facility (in Show configurationStatus)
 *
 * Revision 1.7  2004/10/05 10:54:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved all functionality to XsltPipe
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;

/**
 * {@link XsltPipe XsltPipe} aware of parameters. 
 * @author Johan Verrips
 * @version Id
 * @deprecated please use plain XsltPipe, that now supports parameters too(since 4.2d)
 */
public class XsltParamPipe extends XsltPipe {
	public static final String version = "$Id: XsltParamPipe.java,v 1.8 2008-12-30 17:01:12 m168309 Exp $";

	public void configure() throws ConfigurationException {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = getLogPrefix(null)+"The class ["+getClass().getName()+"] has been deprecated. Please change to ["+getClass().getSuperclass().getName()+"]";
		configWarnings.add(log, msg);
		super.configure();
	}
}
