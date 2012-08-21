/*
 * $Log: FxfWrapperPipe.java,v $
 * Revision 1.4  2012-08-21 10:01:20  m00f069
 * Set destination parameter with default value when wrapping FxF message
 *
 * Revision 1.3  2012/08/17 15:46:44  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added some documentation
 *
 * Revision 1.2  2012/08/17 14:34:15  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Extended FxfWrapperPipe for sending files
 * Implemented FxfXmlValidator
 *
 * Revision 1.1  2012/08/15 08:08:20  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Implemented FxF3 listener as a wrapper and FxF3 cleanup mechanism
 *
 */
package nl.nn.adapterframework.extensions.fxf;

import java.io.File;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.extensions.esb.EsbSoapWrapperPipe;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * FxF wrapper to be used with FxF3. When receiving files (direction=unwrap)
 * the message handed to the pipeline is the local filename extracted from an
 * ESB SOAP message. When sending files (direction=wrap) input should be a local
 * filename which will be wrapped into an ESB SOAP message. Please note: When
 * writing files which need to be send through FxF they should be written to
 * ${fxf.dir}/NNX00000/out. The property ${fxf.dir} will automatically be
 * available on the OTAP environment (define it in StageSpecifics_LOC.properties
 * only). Replace NNX00000 with the specific flowId and generate a unique
 * filename (files will automatically be cleaned after 30 days or any other
 * value specified by ${fxf.retention}).
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.extensions.fxf.FxfListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDirection(String) direction}</td><td>either <code>wrap</code> or <code>unwrap</code></td><td>wrap</td></tr>
 * <tr><td>{@link #setFlowId(String) flowId}</td><td>The flowId of the file transfer when direction=wrap. When direction=unwrap the flowId will be extracted from the incoming message and added as a sessionKey to the pipeline.</td><td></td></tr>
 * <tr><td>{@link #setTransformFilename(String) transformFilename}</td><td>When true and direction=wrap the input which is expected to be a local filename will be transformed to the filename as known on the IUF State machine.</td><td>true</td></tr>
 * </table>
 * 
 * @author Jaco de Groot
 * @version Id
 */
public class FxfWrapperPipe extends EsbSoapWrapperPipe {
	private AppConstants appConstants;
	private String instanceName;
	private String instanceNameLowerCase;
	private String fxfDir;
	private String flowId;
	private String environment;
	private boolean transformFilename = true;
	private TransformerPool transferFlowIdTp = null;
	private TransformerPool clientFilenameTp = null;
	private String soapBodySessionKey = "soapBody";
	private String transferFlowIdSessionKey = "transferFlowId";
	private String clientFilenameSessionKey = "clientFilename";
	private String flowIdSessionKey = "flowId";
	private String fxfDirSessionKey = "fxfDir";


	public void configure() throws ConfigurationException {
		setRemoveOutputNamespaces(true);
		if ("wrap".equalsIgnoreCase(getDirection())) {
			ParameterList parameterList = getParameterList();
			Parameter parameter = parameterList.findParameter(DESTINATION);
			if (parameter == null) {
				parameter = new Parameter();
				parameter.setName(DESTINATION);
				parameter.setValue("ESB.Infrastructure.US.Transfer.FileTransfer.1.StartTransfer.1.Action");
				parameterList.add(parameter);
			}
		}
		super.configure();
		if ("wrap".equalsIgnoreCase(getDirection())) {
			instanceName = appConstants.getResolvedProperty("instance.name");
			if (StringUtils.isEmpty(instanceName)) {
				throw new ConfigurationException("instance.name not available");
			}
			instanceNameLowerCase = appConstants.getResolvedProperty("instance.name.lc");
			if (StringUtils.isEmpty(instanceNameLowerCase)) {
				throw new ConfigurationException("instance.name.lc not available");
			}
			environment = appConstants.getResolvedProperty("otap.stage");
			if (StringUtils.isEmpty(environment) || environment.length() < 1) {
				throw new ConfigurationException("otap.stage not available");
			}
			environment = environment.substring(0, 1);
			if (StringUtils.isEmpty(getFlowId())) {
				throw new ConfigurationException("attribute flowId must be specified");
			} else if (getFlowId().length() < 3) {
				throw new ConfigurationException("attribute flowId too short");
			}
		} else {
			if (!StringUtils.isEmpty(getFlowId())) {
				throw new ConfigurationException("attribute flowId must not be specified");
			}
			fxfDir = appConstants.getResolvedProperty("fxf.dir");
			if (fxfDir == null) {
				throw new ConfigurationException("property fxf.dir has not been initialised");
			}
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
			XmlBuilder xmlStartTransfer_Action = new XmlBuilder("StartTransfer_Action");
			xmlStartTransfer_Action.addAttribute("xmlns", "http://nn.nl/XSD/Infrastructure/Transfer/FileTransfer/1/StartTransfer/1");
			XmlBuilder xmlTransferDetails = new XmlBuilder("TransferDetails");
			xmlStartTransfer_Action.addSubElement(xmlTransferDetails);
			XmlBuilder xmlSenderApplication = new XmlBuilder("SenderApplication");
			xmlSenderApplication.setValue(instanceName);
			xmlTransferDetails.addSubElement(xmlSenderApplication);
			XmlBuilder xmlRecipientApplication = new XmlBuilder("RecipientApplication");
			xmlTransferDetails.addSubElement(xmlRecipientApplication);
			XmlBuilder xmlFilename = new XmlBuilder("Filename");
			if (input != null) {
				String filename = input.toString();
				if (isTransformFilename()) {
					String filenameOnIufState = "/opt/data/FXF/"
							+ instanceNameLowerCase + "/" + getFlowId()
							+ "/out/" + new File(filename).getName();
					xmlFilename.setValue(filenameOnIufState);
				} else {
					xmlFilename.setValue(filename);
				}
			}
			xmlTransferDetails.addSubElement(xmlFilename);
			XmlBuilder xmlTransferFlowId = new XmlBuilder("TransferFlowId");
			String transferFlowId = getFlowId().substring(0, 2) + environment
					+ getFlowId().substring(3);
			xmlTransferFlowId.setValue(transferFlowId);
			xmlTransferDetails.addSubElement(xmlTransferFlowId);
			return super.doPipe(xmlStartTransfer_Action.toXML(), session);
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

	public AppConstants getAppConstants() {
		return appConstants;
	}

	public void setAppConstants(AppConstants appConstants) {
		this.appConstants = appConstants;
	}

	public String getFlowId() {
		return flowId;
	}

	public void setFlowId(String flowId) {
		this.flowId = flowId;
	}

	public boolean isTransformFilename() {
		return transformFilename;
	}

	public void setTransformFilename(boolean transformFilename) {
		this.transformFilename = transformFilename;
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
