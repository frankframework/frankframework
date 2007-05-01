/*
 * $Log: IPipeLineExitHandler.java,v $
 * Revision 1.1  2007-05-01 14:08:45  europe\L190409
 * introduction of PipeLine-exithandlers
 *
 */
package nl.nn.adapterframework.core;

/**
 * Interface that allows a Pipe to register an exit handler.
 * This handler will be called <i>allways</i> after PipeLine-processing has finished
 * 
 * @author  Gerrit van Brakel
 * @since   4.6.0  
 * @version Id
 */
public interface IPipeLineExitHandler extends INamedObject {

	/**
	 * Called to allow registered handler to perform cleanup or commit/rollback.
	 * 
	 * @param correlationId	 correlationId of current session
	 * @param pipeLineResult the result of the PipeLine 
	 * @param session		 the PipeLineSession
	 */
	public void atEndOfPipeLine(String correlationId, PipeLineResult pipeLineResult, PipeLineSession session) throws PipeRunException;
}
