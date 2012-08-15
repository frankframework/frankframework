/*
 * $Log: FxfWrapperPipe.java,v $
 * Revision 1.1  2012-08-15 08:08:20  m00f069
 * Implemented FxF3 listener as a wrapper and FxF3 cleanup mechanism
 *
 */
package nl.nn.adapterframework.extensions.fxf;

import java.io.File;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.extensions.esb.EsbSoapWrapperPipe;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.TransformerPool;

/**
 * FxF wrapper to be used with FxF3. When receiving files (direction=unwrap)
 * the message handed to the pipeline is the local filename extracted from an
 * ESB SOAP message. When sending files (direction=wrap) input should be a local
 * filename which will be wrapped into an ESB SOAP message.
 * 
 * @author Jaco de Groot
 * @version Id
 */
public class FxfWrapperPipe extends EsbSoapWrapperPipe {
	private String fxfDir;
	private TransformerPool transferFlowIdTp = null;
	private TransformerPool clientFilenameTp = null;
	private String soapBodySessionKey = "soapBody";
	private String transferFlowIdSessionKey = "transferFlowId";
	private String clientFilenameSessionKey = "clientFilename";
	private String flowIdSessionKey = "flowId";
	private String fxfDirSessionKey = "fxfDir";


	public void configure() throws ConfigurationException {
		setRemoveOutputNamespaces(true);
		super.configure();
		if ("wrap".equalsIgnoreCase(getDirection())) {
			// TODO Implement for sending FxF files
		} else {
			fxfDir = configureFxfDir(true);
			transferFlowIdTp = TransformerPool.configureTransformer0(
					getLogPrefix(null), null,
					"/OnCompletedTransferNotify_Action/TransferFlowId", null,
					"text", false, getParameterList(), true);
			clientFilenameTp = TransformerPool.configureTransformer0(
					getLogPrefix(null), null,
					"/OnCompletedTransferNotify_Action/ClientFilename", null,
					"text", false, getParameterList(), true);
		}
	}

	public static String configureFxfDir(boolean fxfDirMandatory) throws ConfigurationException {
		String fxfDir = AppConstants.getInstance().getProperty("fxf.dir");
		if (fxfDir == null) {
			// Use default location, see was.policy too
			fxfDir = System.getProperty("APPSERVER_ROOT_DIR");
			if (fxfDir != null) {
				fxfDir = fxfDir + File.separator + "fxf-work";
			} else {
				if (fxfDirMandatory) {
					throw new ConfigurationException("Could not determine FxF directory");
				}
			}
		}
		if (!new File(fxfDir).isDirectory()) {
			if (fxfDirMandatory) {
				throw new ConfigurationException("Could not find FxF directory: " + fxfDir);
			}
			fxfDir = null;
		}
		return fxfDir;
	}

	public void start() throws PipeStartException {
		super.start();
		if (transferFlowIdTp != null) {
			try {
				transferFlowIdTp.open();
			} catch (Exception e) {
				throw new PipeStartException(getLogPrefix(null)+"cannot start transfer flow id TransformerPool", e);
			}
		}
		if (clientFilenameTp != null) {
			try {
				clientFilenameTp.open();
			} catch (Exception e) {
				throw new PipeStartException(getLogPrefix(null)+"cannot start client filename TransformerPool", e);
			}
		}
	}

	public void stop() {
		super.stop();
		if (transferFlowIdTp != null) {
			transferFlowIdTp.close();
		}
		if (clientFilenameTp != null) {
			clientFilenameTp.close();
		}
	}

	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		if ("wrap".equalsIgnoreCase(getDirection())) {
			// TODO Implement for sending FxF files
			return super.doPipe(input, session);
		} else {
			String soapBody = (String)super.doPipe(input, session).getResult();
			session.put(getSoapBodySessionKey(), soapBody);
			String transferFlowId;
			String clientFilename;
			try {
				transferFlowId = transferFlowIdTp.transform(soapBody, null);
				session.put(getTransferFlowIdSessionKey(), transferFlowId);
				clientFilename = clientFilenameTp.transform(soapBody, null);
				session.put(getClientFilenameSessionKey(), clientFilename);
			} catch (Throwable t) {
				throw new PipeRunException(this, getLogPrefix(session) + " Unexpected exception during (un)wrapping ", t);
			}
			String flowId = transferFlowId.substring(0, 2) + "X" + transferFlowId.substring(3);
			session.put(getFlowIdSessionKey(), flowId);
			session.put(getFxfDirSessionKey(), fxfDir);
			// Transform the filename as it is known locally on the IUF state
			// machine to the filename as know on the application server (which
			// has a mount to the IUF state machine).
			String result = fxfDir + File.separator + flowId + File.separator
							+ "in" + File.separator
							+ new File(clientFilename).getName();
			return new PipeRunResult(getForward(), result);
		}
	}

	public String getSoapBodySessionKey() {
		return soapBodySessionKey;
	}

	public void setSoapBodySessionKey(String soapBodySessionKey) {
		this.soapBodySessionKey = soapBodySessionKey;
	}

	public String getTransferFlowIdSessionKey() {
		return transferFlowIdSessionKey;
	}

	public void setTransferFlowIdSessionKey(String transferFlowIdSessionKey) {
		this.transferFlowIdSessionKey = transferFlowIdSessionKey;
	}

	public String getClientFilenameSessionKey() {
		return clientFilenameSessionKey;
	}

	public void setClientFilenameSessionKey(String clientFilenameSessionKey) {
		this.clientFilenameSessionKey = clientFilenameSessionKey;
	}

	public String getFlowIdSessionKey() {
		return flowIdSessionKey;
	}

	public void setFlowIdSessionKey(String flowIdSessionKey) {
		this.flowIdSessionKey = flowIdSessionKey;
	}

	public String getFxfDirSessionKey() {
		return fxfDirSessionKey;
	}

	public void setFxfDirSessionKey(String fxfDirSessionKey) {
		this.fxfDirSessionKey = fxfDirSessionKey;
	}

}
