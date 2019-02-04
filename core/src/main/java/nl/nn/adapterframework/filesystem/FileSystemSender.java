package nl.nn.adapterframework.filesystem;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.codec.binary.Base64InputStream;

public class FileSystemSender<F, FS extends IFileSystem<F>> extends SenderWithParametersBase
		implements ISambaSender {

	private String action = null;
	private List<String> actions = Arrays.asList("delete", "upload", "mkdir", "rmdir", "rename",
			"download", "list");

	private FS fileSystem;

	public FS getFileSystem() {
		return fileSystem;
	}

	public void setFileSystem(FS fileSystem) {
		this.fileSystem = fileSystem;
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (getAction() == null)
			throw new ConfigurationException(getLogPrefix() + "action must be specified");
		if (!actions.contains(getAction()))
			throw new ConfigurationException(getLogPrefix() + "unknown or invalid action ["
					+ getAction() + "] supported actions are " + actions.toString() + "");
	}

	@Override
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc)
			throws SenderException, TimeOutException {
		ParameterValueList pvl = null;
		try {
			if (prc != null && paramList != null) {
				pvl = prc.getValues(paramList);
			}
		} catch (ParameterException e) {
			throw new SenderException(getLogPrefix() + "Sender [" + getName()
					+ "] caught exception evaluating parameters", e);
		}
		IFileSystem<F> ifs = getFileSystem();
		F file;
		try {
			file = ifs.toFile(message);
		} catch (Exception e) {
			throw new SenderException(getLogPrefix() + "unable to get file", e);
		}

		try {
			if (action.equalsIgnoreCase("delete")) {
				ifs.deleteFile(file);
			} else if (action.equalsIgnoreCase("download")) {
				InputStream is = new Base64InputStream(ifs.readFile(file), true);
				return Misc.streamToString(is);
			} else if (action.equalsIgnoreCase("list")) {
				Iterator<F> fileList = ifs.listFiles();

				int count = 0;
				XmlBuilder dirXml = new XmlBuilder("directory");
				while (fileList.hasNext()) {
					F fileObject = fileList.next();
					dirXml.addSubElement(ifs.getFileAsXmlBuilder(fileObject));
					count++;
				}
				ifs.augmentDirectoryInfo(dirXml, file);
				dirXml.addAttribute("count", count);
				return dirXml.toXML();

			} else if (action.equalsIgnoreCase("upload")) {
				Object paramValue = pvl.getParameterValue("file").getValue();
				byte[] fileBytes = null;
				if (paramValue instanceof InputStream)
					fileBytes = Misc.streamToBytes((InputStream) paramValue);
				else if (paramValue instanceof byte[])
					fileBytes = (byte[]) paramValue;
				else if (paramValue instanceof String)
					fileBytes = ((String) paramValue).getBytes(Misc.DEFAULT_INPUT_STREAM_ENCODING);
				else
					throw new SenderException("expected InputStream, ByteArray or String but got ["
							+ paramValue.getClass().getName() + "] instead");
				OutputStream out = null;
				out = ifs.createFile(file);
				out.write(fileBytes);
				out.close();
				return ifs.getFileAsXmlBuilder(file).toXML();
			} else if (action.equalsIgnoreCase("mkdir")) {
				ifs.createFolder(file);
			} else if (action.equalsIgnoreCase("rmdir")) {
				ifs.removeFolder(file);
			} else if (action.equalsIgnoreCase("rename")) {
				String destination = (String) pvl.getParameterValue("destination").getValue();
				if (destination == null)
					throw new SenderException("unknown destination[" + destination + "]");
				ifs.renameTo(file, destination);
			}
		} catch (Exception e) {
			throw new SenderException(getLogPrefix() + "unable to process action for SmbFile ["
					+ message + "]", e);
		}

		return correlationID;
	}

	public void setAction(String action) {
		this.action = action.toLowerCase();
	}

	public String getAction() {
		return action;
	}
}
