/*
 * $Log: PostboxRetrieverPipe.java,v $
 * Revision 1.1  2004-05-21 10:47:30  a1909356#db2admin
 * Add (modifications) due to the postbox retriever implementation
 *
 * Revision 1.1  2004/05/21 07:59:30  unknown <unknown@ibissource.org>
 * Add (modifications) due to the postbox sender implementation
 *
 */
package nl.nn.adapterframework.pipes;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.IPostboxListener;
import nl.nn.adapterframework.core.IPostboxSender;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.ParameterValueResolver;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;

/**
 * Sends a message using a {@link ISender} and optionally receives a reply from the same sender, or from a {@link ICorrelatedPullingListener listener}.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #addParameter(Parameter) parameterList}</td><td>Parameters of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td><code>sender.*</td><td>any attribute of the sender instantiated by descendant classes</td><td>&nbsp;</td></tr>
 * </table>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link IPostboxSender sender}</td><td>specification of postbox sender to send messages with</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default when the message was successfully sent</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified, and otherwise under same condition as "success"</td></tr>
 * </table>
 * </p>
  * 
 * @author John Dekker
 * @version Id
 */
public class PostboxRetrieverPipe  extends FixedForwardPipe {
	public static final String version="$Id: PostboxRetrieverPipe.java,v 1.1 2004-05-21 10:47:30 a1909356#db2admin Exp $";
	private IPostboxListener listener = null;
	private String resultOnEmptyPostbox = "empty postbox";
		
	/** 
	 * @see nl.nn.adapterframework.core.IPipe#configure()
	 */
	public void configure() throws ConfigurationException {
		super.configure();

		if (getListener() == null) {
				throw new ConfigurationException(getLogPrefix(null) + "no sender defined ");
		}
	}

	/**
	 * @return
	 */
	public IPostboxListener getListener() {
		return listener;
	}

	/**
	 * @param listener
	 */
	public void setListener(IPostboxListener listener) {
		this.listener = listener;
	}

	/** 
	 * @see nl.nn.adapterframework.core.IPipe#start()
	 */
	public void start() throws PipeStartException {
		try {
			getListener().open();
		} 
		catch (Exception e) {
			throw new PipeStartException(e);
		}
	}

	/** 
	 * @see nl.nn.adapterframework.core.IPipe#stop()
	 */
	public void stop() {
		try {
			getListener().close();
		} 
		catch (Exception e) {
			log.warn(getLogPrefix(null) + " exception closing sender", e);
		}
	}

	/**
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(java.lang.Object, nl.nn.adapterframework.core.PipeLineSession)
	 */
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		if (! (input instanceof String)) {
			throw new PipeRunException(this, "String expected, got a [" + input.getClass().getName() + "]");
		}

		HashMap threadContext = null;
		try {
			threadContext = getListener().openThread();
			Object rawMessage = getListener().retrieveRawMessage((String)input, threadContext);
			
			if (rawMessage == null)
				return new PipeRunResult(findForward("emptyPostbox"), getResultOnEmptyPostbox());
				
			String result = getListener().getStringFromRawMessage(rawMessage, threadContext);
			return new PipeRunResult(getForward(), result);
		} 
		catch (Exception e) {
			throw new PipeRunException( this, getLogPrefix(session) + "caught exception", e);
		} 
		finally {
			try {
				getListener().closeThread(threadContext);
			} 
			catch (ListenerException le) {
				log.error(getLogPrefix(session)+"got error closing listener");
			}
		}
	}

	/**
	 * @return
	 */
	public String getResultOnEmptyPostbox() {
		return resultOnEmptyPostbox;
	}

	/**
	 * @param string
	 */
	public void setResultOnEmptyPostbox(String string) {
		resultOnEmptyPostbox = string;
	}

}
