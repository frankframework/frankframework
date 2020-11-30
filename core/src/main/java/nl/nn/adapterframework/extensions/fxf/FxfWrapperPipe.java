/*
   Copyright 2013, 2016, 2020 Nationale-Nederlanden, 2020 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.extensions.fxf;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.extensions.esb.EsbSoapWrapperPipe;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

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
 * <tr><td>{@link #setTransformFilename(boolean) transformFilename}</td><td>When true and direction=wrap the input which is expected to be a local filename will be transformed to the filename as known on the IUF State machine.</td><td>true</td></tr>
 * <tr><td>{@link #setFxfVersion(String) fxfVersion}</td><td>either 3.1 or 3.2</td><td>3.1</td></tr>
 * </table>
 * 
 * @author Jaco de Groot
 */
public class FxfWrapperPipe extends EsbSoapWrapperPipe {
	private String instanceName;
	private String instanceNameLowerCase;
	private String fxfDir;
	private String flowId;
	private String environment;
	private boolean transformFilename = true;
	private String flowOutFolder = "";
	private String fxfVersion = "3.1";
	private TransformerPool transferFlowIdTp = null;
	private TransformerPool clientFilenameTp = null;
	private String soapBodySessionKey = "soapBody";
	private String transferFlowIdSessionKey = "transferFlowId";
	private String clientFilenameSessionKey = "clientFilename";
	private String flowIdSessionKey = "flowId";
	private String fxfDirSessionKey = "fxfDir";
	private String fxfFileSessionKey = "fxfFile";
	private boolean createFolder = false;


	@Override
	public void configure() throws ConfigurationException {
		setRemoveOutputNamespaces(true);
		if ("wrap".equalsIgnoreCase(getDirection())) {
			ParameterList parameterList = getParameterList();
			Parameter parameter = parameterList.findParameter(DESTINATION);
			if (parameter == null) {
				parameter = new Parameter();
				parameter.setName(DESTINATION);
				parameter.setValue("ESB.Infrastructure.US.Transfer.FileTransfer.1.StartTransfer."+retrieveStartTransferVersion()+".Action");
				parameterList.add(parameter);
			}
		}
		super.configure();
		AppConstants rootAppConstants = AppConstants.getInstance();
		if ("wrap".equalsIgnoreCase(getDirection())) {
			instanceName = rootAppConstants.getResolvedProperty("instance.name");
			if (StringUtils.isEmpty(instanceName)) {
				throw new ConfigurationException("instance.name not available");
			}
			instanceNameLowerCase = rootAppConstants.getResolvedProperty("instance.name.lc");
			if (StringUtils.isEmpty(instanceNameLowerCase)) {
				throw new ConfigurationException("instance.name.lc not available");
			}
			environment = rootAppConstants.getResolvedProperty("dtap.stage");
			if (StringUtils.isEmpty(environment) || environment.length() < 1) {
				throw new ConfigurationException("dtap.stage not available");
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
			fxfDir = AppConstants.getInstance(getConfigurationClassLoader()).getResolvedProperty("fxf.dir");
			if (fxfDir == null) {
				throw new ConfigurationException("property fxf.dir has not been initialised");
			}
			if(isCreateFolder() && !new File(fxfDir).exists() && !new File(fxfDir).mkdirs()) {
				throw new ConfigurationException("cannot create fxf.dir in the path [" + fxfDir + "]");
			}
			if(!new File(fxfDir).isDirectory()) {
				throw new ConfigurationException("fxf.dir [" + fxfDir + "] doesn't exist or is not a directory");
			}
			transferFlowIdTp = XmlUtils.getXPathTransformerPool(null, "/OnCompletedTransferNotify_Action/TransferFlowId", "text", false, getParameterList());
			clientFilenameTp = XmlUtils.getXPathTransformerPool(null, "/OnCompletedTransferNotify_Action/ClientFilename", "text", false, getParameterList());
		}
		if (StringUtils.isNotEmpty(getFlowOutFolder()) && !getFlowOutFolder().endsWith("/")) {
			setFlowOutFolder(getFlowOutFolder()+"/");
		}
		if (!getFxfVersion().equals("3.1") && !getFxfVersion().equals("3.2")) {
			throw new ConfigurationException("illegal value for fxfVersion [" + getFxfVersion() + "], must be '3.1' or '3.2'");
		}
	}

	@Override
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

	@Override
	public void stop() {
		super.stop();
		if (transferFlowIdTp != null) {
			transferFlowIdTp.close();
		}
		if (clientFilenameTp != null) {
			clientFilenameTp.close();
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, IPipeLineSession session) throws PipeRunException {
		if ("wrap".equalsIgnoreCase(getDirection())) {
			XmlBuilder xmlStartTransfer_Action = new XmlBuilder("StartTransfer_Action");
			xmlStartTransfer_Action.addAttribute("xmlns", "http://nn.nl/XSD/Infrastructure/Transfer/FileTransfer/1/StartTransfer/"+retrieveStartTransferVersion());
			XmlBuilder xmlTransferDetails = new XmlBuilder("TransferDetails");
			xmlStartTransfer_Action.addSubElement(xmlTransferDetails);
			XmlBuilder xmlSenderApplication = new XmlBuilder("SenderApplication");
			xmlSenderApplication.setValue(instanceName);
			xmlTransferDetails.addSubElement(xmlSenderApplication);
			XmlBuilder xmlRecipientApplication = new XmlBuilder("RecipientApplication");
			xmlTransferDetails.addSubElement(xmlRecipientApplication);
			XmlBuilder xmlFilename = new XmlBuilder("Filename");
			if (message != null) {
				String filename;
				try {
					filename = message.asString();
				} catch (IOException e) {
					throw new PipeRunException(this, getLogPrefix(session)+"cannot open stream", e);
				}
				if (isTransformFilename()) {
					String filenameOnIufState = "/opt/data/FXF/" + instanceNameLowerCase + "/" + getFlowId() + "/out/" + new File(filename).getName();
					xmlFilename.setValue(filenameOnIufState);
				} else {
					xmlFilename.setValue(getFlowOutFolder()+filename);
				}
			}
			xmlTransferDetails.addSubElement(xmlFilename);
			XmlBuilder xmlTransferFlowId = new XmlBuilder("TransferFlowId");
			String transferFlowId = getFlowId().substring(0, 2) + environment + getFlowId().substring(3);
			xmlTransferFlowId.setValue(transferFlowId);
			xmlTransferDetails.addSubElement(xmlTransferFlowId);
			return super.doPipe(new Message(xmlStartTransfer_Action.toXML()), session);
		} else {
			String soapBody;
			try {
				soapBody = super.doPipe(message, session).getResult().asString();
			} catch (IOException e) {
				throw new PipeRunException(this,"cannot convert result",e);
			}
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
			String fxfFile = fxfDir + File.separator + flowId + File.separator + "in" + File.separator + new File(clientFilename).getName();
			session.put(getFxfFileSessionKey(), fxfFile);
			return new PipeRunResult(getForward(), fxfFile);
		}
	}

	private int retrieveStartTransferVersion() {
		if ("3.1".equals(getFxfVersion())) {
			return 1;
		} else if ("3.2".equals(getFxfVersion())) {
			return 2;
		}
		return 0;
	}

	@IbisDoc({"1", "specifies the id of the transfer flow", ""})
	public void setFlowId(String flowId) {
		this.flowId = flowId;
	}
	public String getFlowId() {
		return flowId;
	}

	@IbisDoc({"2", "specifies the output folder if transformFilename=<code>false</code> and direction=wrap", ""})
	public void setFlowOutFolder(String flowOutFolder) {
		this.flowOutFolder = flowOutFolder;
	}
	public String getFlowOutFolder() {
		return flowOutFolder;
	}

	@IbisDoc({"3", "when <code>true</code> and direction=wrap, the folder the output folder <code>flowOutFolder</code> will be constructed as <code>/opt/data/FXF/&lt;instanceNameLowerCase&gt;/&lt;flowId&gt;/out/</code> ", "true"})
	public void setTransformFilename(boolean transformFilename) {
		this.transformFilename = transformFilename;
	}
	public boolean isTransformFilename() {
		return transformFilename;
	}

	public void setSoapBodySessionKey(String soapBodySessionKey) {
		this.soapBodySessionKey = soapBodySessionKey;
	}
	public String getSoapBodySessionKey() {
		return soapBodySessionKey;
	}

	public void setTransferFlowIdSessionKey(String transferFlowIdSessionKey) {
		this.transferFlowIdSessionKey = transferFlowIdSessionKey;
	}
	public String getTransferFlowIdSessionKey() {
		return transferFlowIdSessionKey;
	}

	public void setClientFilenameSessionKey(String clientFilenameSessionKey) {
		this.clientFilenameSessionKey = clientFilenameSessionKey;
	}
	public String getClientFilenameSessionKey() {
		return clientFilenameSessionKey;
	}

	public void setFlowIdSessionKey(String flowIdSessionKey) {
		this.flowIdSessionKey = flowIdSessionKey;
	}
	public String getFlowIdSessionKey() {
		return flowIdSessionKey;
	}

	public void setFxfDirSessionKey(String fxfDirSessionKey) {
		this.fxfDirSessionKey = fxfDirSessionKey;
	}
	public String getFxfDirSessionKey() {
		return fxfDirSessionKey;
	}

	public void setFxfFileSessionKey(String fxfFileSessionKey) {
		this.fxfFileSessionKey = fxfFileSessionKey;
	}
	public String getFxfFileSessionKey() {
		return fxfFileSessionKey;
	}

	public void setFxfVersion(String fxfVersion) {
		this.fxfVersion = fxfVersion;
	}
	public String getFxfVersion() {
		return fxfVersion;
	}

	@IbisDoc({"when set to <code>true</code>, the folder corresponding fxf.dir property will be created in case it does not exist", "false"})
	public void setCreateFolder(boolean createFolder) {
		this.createFolder = createFolder;
	}
	public boolean isCreateFolder() {
		return createFolder;
	}
}
