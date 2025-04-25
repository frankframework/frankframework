/*
   Copyright 2013, 2016, 2020 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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
package org.frankframework.extensions.fxf;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.TransformerConfigurationException;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.extensions.esb.EsbSoapWrapperPipe;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.TransformerPool.OutputType;
import org.frankframework.util.XmlBuilder;

/**
 * FxF wrapper to be used with FxF3. When receiving files (direction=unwrap)
 * the message handed to the pipeline is the local filename extracted from an
 * ESB SOAP message. When sending files (direction=wrap) input should be a local
 * filename which will be wrapped into an ESB SOAP message. Please note: When
 * writing files which need to be send through FxF they should be written to
 * ${fxf.dir}/NNX00000/out. The property ${fxf.dir} will automatically be
 * available on the DTAP environment (define it in StageSpecifics_LOC.properties
 * only). Replace NNX00000 with the specific flowId and generate a unique
 * filename (files will automatically be cleaned after 30 days or any other
 * value specified by ${fxf.retention}).
 *
 * @author Jaco de Groot
 */
public class FxfWrapperPipe extends EsbSoapWrapperPipe {
	private String instanceName;
	private String instanceNameLowerCase;
	private String fxfDir;
	private @Getter String flowId;
	private String environment;
	private @Getter boolean transformFilename = true;
	private @Getter String flowOutFolder = "";
	private @Getter String fxfVersion = "3.1";
	private TransformerPool transferFlowIdTp = null;
	private TransformerPool clientFilenameTp = null;
	private @Getter String soapBodySessionKey = "soapBody";
	private @Getter String transferFlowIdSessionKey = "transferFlowId";
	private @Getter String clientFilenameSessionKey = "clientFilename";
	private @Getter String flowIdSessionKey = "flowId";
	private @Getter String fxfDirSessionKey = "fxfDir";
	private @Getter String fxfFileSessionKey = "fxfFile";
	private @Getter boolean createFolder = false;
	private @Getter boolean useServerFilename = false;

	private static final String DESTINATION_PREFIX = "ESB.Infrastructure.US.Transfer.FileTransfer.1.StartTransfer";
	private static final String DESTINATION_SUFFIX = "Action";
	private static final String ON_COMPLETED_TRANSFER_NOTIFY_ACTION = "/OnCompletedTransferNotify_Action/";
	private static final String TRANSFORMER_FLOW_ID_XPATH = ON_COMPLETED_TRANSFER_NOTIFY_ACTION + "TransferFlowId";
	private static final String SERVER_FILENAME_XPATH = ON_COMPLETED_TRANSFER_NOTIFY_ACTION+"ServerFilename";
	private static final String CLIENT_FILENAME_XPATH = ON_COMPLETED_TRANSFER_NOTIFY_ACTION+"ClientFilename";
	private static final String TRANSFER_ACTION_NAMESPACE_PREFIX = "http://nn.nl/XSD/Infrastructure/Transfer/FileTransfer/1/StartTransfer/";
	private static final String FILEPATH_PREFIX = "/opt/data/FXF/";

	@Override
	public void configure() throws ConfigurationException {
		setRemoveOutputNamespaces(true);
		if (getDirection()==Direction.WRAP) {
			ParameterList parameterList = getParameterList();
			if (!parameterList.hasParameter(DESTINATION_PARAMETER_NAME)) {
				Parameter p = SpringUtils.createBean(getApplicationContext());
				p.setName(DESTINATION_PARAMETER_NAME);
				p.setValue(DESTINATION_PREFIX+"."+retrieveStartTransferVersion()+"."+DESTINATION_SUFFIX);
				parameterList.add(p);
			}
		}
		super.configure();
		AppConstants rootAppConstants = AppConstants.getInstance();
		if (getDirection()==Direction.WRAP) {
			instanceName = rootAppConstants.getProperty("instance.name");
			if (StringUtils.isEmpty(instanceName)) {
				throw new ConfigurationException("instance.name not available");
			}
			instanceNameLowerCase = rootAppConstants.getProperty("instance.name.lc");
			if (StringUtils.isEmpty(instanceNameLowerCase)) {
				throw new ConfigurationException("instance.name.lc not available");
			}
			environment = rootAppConstants.getProperty("dtap.stage");
			if (StringUtils.isEmpty(environment)) {
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
			fxfDir = AppConstants.getInstance(getConfigurationClassLoader()).getProperty("fxf.dir");
			if (fxfDir == null) {
				throw new ConfigurationException("property fxf.dir has not been initialised");
			}
			if(isCreateFolder() && !new File(fxfDir).exists() && !new File(fxfDir).mkdirs()) {
				throw new ConfigurationException("cannot create fxf.dir in the path [" + fxfDir + "]");
			}
			if(!new File(fxfDir).isDirectory()) {
				throw new ConfigurationException("fxf.dir [" + fxfDir + "] doesn't exist or is not a directory");
			}
			try {
				transferFlowIdTp = TransformerPool.getXPathTransformerPool(null, TRANSFORMER_FLOW_ID_XPATH, OutputType.TEXT, false, getParameterList());
			} catch (TransformerConfigurationException e) {
				throw new ConfigurationException("Cannot create TransformerPool for XPath expression ["+TRANSFORMER_FLOW_ID_XPATH+"]", e);
			}
			String xpathFilename = isUseServerFilename() ? SERVER_FILENAME_XPATH : CLIENT_FILENAME_XPATH;
			try {
				clientFilenameTp = TransformerPool.getXPathTransformerPool(null, xpathFilename, OutputType.TEXT, false, getParameterList());
			} catch (TransformerConfigurationException e) {
				throw new ConfigurationException("Cannot create TransformerPool for XPath expression ["+xpathFilename+"]", e);
			}
		}
		if (StringUtils.isNotEmpty(getFlowOutFolder()) && !getFlowOutFolder().endsWith("/")) {
			setFlowOutFolder(getFlowOutFolder()+"/");
		}
		if (!getFxfVersion().equals("3.1") && !getFxfVersion().equals("3.2")) {
			throw new ConfigurationException("illegal value for fxfVersion [" + getFxfVersion() + "], must be '3.1' or '3.2'");
		}
	}

	@Override
	public void start() {
		super.start();

		if (transferFlowIdTp != null) {
			transferFlowIdTp.open();
		}

		if (clientFilenameTp != null) {
			clientFilenameTp.open();
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
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		if (getDirection()==Direction.WRAP) {
			XmlBuilder xmlStartTransfer_Action = new XmlBuilder("StartTransfer_Action");
			xmlStartTransfer_Action.addAttribute("xmlns", TRANSFER_ACTION_NAMESPACE_PREFIX+retrieveStartTransferVersion());
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
					throw new PipeRunException(this, "cannot open stream", e);
				}
				if (isTransformFilename()) {
					String filenameOnIufState = FILEPATH_PREFIX + instanceNameLowerCase + "/" + getFlowId() + "/out/" + new File(filename).getName();
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
			return super.doPipe(xmlStartTransfer_Action.asMessage(), session);
		}
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
			transferFlowId = transferFlowIdTp.transformToString(soapBody, null);
			session.put(getTransferFlowIdSessionKey(), transferFlowId);
			clientFilename = clientFilenameTp.transformToString(soapBody, null);
			session.put(getClientFilenameSessionKey(), clientFilename);
		} catch (Throwable t) {
			throw new PipeRunException(this, "Unexpected exception during (un)wrapping ", t);
		}
		String flowId = transferFlowId.substring(0, 2) + "X" + transferFlowId.substring(3);
		session.put(getFlowIdSessionKey(), flowId);
		session.put(getFxfDirSessionKey(), fxfDir);
		// Windows style file separator will be a problem in linux with new File(clientFilename).getName() so replace it
		if(StringUtils.isNotEmpty(clientFilename) && clientFilename.contains("\\")) {
			clientFilename = clientFilename.replace("\\", File.separator);
		}
		// Transform the filename as it is known locally on the IUF state
		// machine to the filename as know on the application server (which
		// has a mount to the IUF state machine).
		String fxfFile = fxfDir + File.separator + flowId + File.separator + "in" + File.separator + new File(clientFilename).getName();
		session.put(getFxfFileSessionKey(), fxfFile);
		return new PipeRunResult(getSuccessForward(), fxfFile);
	}

	private int retrieveStartTransferVersion() {
		if ("3.1".equals(getFxfVersion())) {
			return 1;
		} else if ("3.2".equals(getFxfVersion())) {
			return 2;
		}
		return 0;
	}

	/** The flowId of the file transfer when direction=wrap. When direction=unwrap the flowId will be extracted from the incoming message and added as a sessionKey to the pipeline. */
	public void setFlowId(String flowId) {
		this.flowId = flowId;
	}

	/** specifies the output folder if transformFilename=<code>false</code> and direction=wrap */
	public void setFlowOutFolder(String flowOutFolder) {
		this.flowOutFolder = flowOutFolder;
	}

	/**
	 * when <code>true</code> and direction=wrap, the input which is expected to be a local filename will be transformed to the filename as known on the IUF State machine.
	 * @ff.default true
	 */
	public void setTransformFilename(boolean transformFilename) {
		this.transformFilename = transformFilename;
	}

	public void setSoapBodySessionKey(String soapBodySessionKey) {
		this.soapBodySessionKey = soapBodySessionKey;
	}

	public void setTransferFlowIdSessionKey(String transferFlowIdSessionKey) {
		this.transferFlowIdSessionKey = transferFlowIdSessionKey;
	}

	public void setClientFilenameSessionKey(String clientFilenameSessionKey) {
		this.clientFilenameSessionKey = clientFilenameSessionKey;
	}

	public void setFlowIdSessionKey(String flowIdSessionKey) {
		this.flowIdSessionKey = flowIdSessionKey;
	}

	public void setFxfDirSessionKey(String fxfDirSessionKey) {
		this.fxfDirSessionKey = fxfDirSessionKey;
	}

	public void setFxfFileSessionKey(String fxfFileSessionKey) {
		this.fxfFileSessionKey = fxfFileSessionKey;
	}

	/**
	 * either 3.1 or 3.2
	 * @ff.default 3.1
	 */
	public void setFxfVersion(String fxfVersion) {
		this.fxfVersion = fxfVersion;
	}

	/**
	 * when set to <code>true</code>, the folder corresponding fxf.dir property will be created in case it does not exist
	 * @ff.default false
	 */
	public void setCreateFolder(boolean createFolder) {
		this.createFolder = createFolder;
	}

	/**
	 * when set to <code>true</code>, ServerFileName from the input will be used as the filename
	 * @ff.default false
	 */
	public void setUseServerFilename(boolean useServerFilename) {
		this.useServerFilename = useServerFilename;
	}
}
