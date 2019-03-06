package nl.nn.adapterframework.senders;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.binary.Base64InputStream;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.filesystem.FileSystemException;
import nl.nn.adapterframework.filesystem.IFileSystem;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;

public class FileSystemSender<F, FS extends IFileSystem<F>> extends SenderWithParametersBase
		implements IFileSystemSender {

	private String action;
	private List<String> actions = new ArrayList<String>(
			Arrays.asList("delete", "upload", "mkdir", "rmdir", "rename", "download", "list"));

	private FS fileSystem;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		getFileSystem().configure();

		if (getAction() == null)
			throw new ConfigurationException(getLogPrefix() + "action must be specified");
		if (!actions.contains(getAction()))
			throw new ConfigurationException(getLogPrefix() + "unknown or invalid action [" + getAction()
					+ "] supported actions are " + actions.toString() + "");

		//Check if necessarily parameters are available
		ParameterList parameterList = getParameterList();
		if (getAction().equals("upload") && (parameterList == null || parameterList.findParameter("file") == null))
			throw new ConfigurationException(
					getLogPrefix() + "the upload action requires the file parameter to be present");
		if (getAction().equals("rename")
				&& (parameterList == null || parameterList.findParameter("destination") == null))
			throw new ConfigurationException(
					getLogPrefix() + "the rename action requires a destination parameter to be present");
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
			throw new SenderException(
					getLogPrefix() + "Sender [" + getName() + "] caught exception evaluating parameters", e);
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
				String result = Misc.streamToString(is);
				is.close();
				return result;
			} else if (action.equalsIgnoreCase("list")) {
				Iterator<F> fileList = ifs.listFiles();
				int count = 0;
				XmlBuilder dirXml = getFileAsXmlBuilder(file,"directory");
				while (fileList.hasNext()) {
					F fileObject = fileList.next();
					dirXml.addSubElement(getFileAsXmlBuilder(fileObject, "file"));
					count++;
				}
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

				return getFileAsXmlBuilder(file, "file").toXML();
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
			throw new SenderException(getLogPrefix() + "unable to process action for File [" + message + "]", e);
		}

		return correlationID;
	}

	public XmlBuilder getFileAsXmlBuilder(F f, String elementName ) throws FileSystemException {
		IFileSystem<F> ifs = getFileSystem();
		XmlBuilder fileXml = new XmlBuilder(elementName);

		String name = ifs.getName(f);
		fileXml.addAttribute("name", name);
		if (!".".equals(name) && !"..".equals(name)) {
			boolean isFolder = ifs.isFolder(f);
			long fileSize = ifs.getFileSize(f, isFolder);
			fileXml.addAttribute("size", "" + fileSize);
			fileXml.addAttribute("fSize", "" + Misc.toFileSize(fileSize, true));
			fileXml.addAttribute("directory", "" + isFolder);
			try {
				fileXml.addAttribute("canonicalName", fileSystem.getCanonicalName(f, isFolder));
			} catch (Exception e) {
				log.warn("cannot get canonicalName for file [" + name + "]", e);
				fileXml.addAttribute("canonicalName", name);
			}
			// Get the modification date of the file
			Date modificationDate = ifs.getModificationTime(f, isFolder);
			//add date
			if (modificationDate != null) {
				String date = DateUtils.format(modificationDate, DateUtils.FORMAT_DATE);
				fileXml.addAttribute("modificationDate", date);

				// add the time
				String time = DateUtils.format(modificationDate, DateUtils.FORMAT_TIME_HMS);
				fileXml.addAttribute("modificationTime", time);
			}

		}

		return fileXml;
	}

	public FS getFileSystem() {
		return fileSystem;
	}

	public void setFileSystem(FS fileSystem) {
		this.fileSystem = fileSystem;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	protected void addActions(List<String> specificActions) {
		actions.addAll(specificActions);
	}

}