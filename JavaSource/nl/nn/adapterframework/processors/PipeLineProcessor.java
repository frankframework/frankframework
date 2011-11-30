/*
 * $Log: PipeLineProcessor.java,v $
 * Revision 1.5  2011-11-30 13:51:54  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2011/08/22 14:29:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added first pipe to interface
 *
 * Revision 1.2  2010/09/07 15:55:13  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;

/**
 * @author Jaco de Groot
 * @version Id
 */
public interface PipeLineProcessor {

	PipeLineResult processPipeLine(PipeLine pipeLine, String messageId, String message, PipeLineSession pipeLineSession, String firstPipe) throws PipeRunException;

}
