package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.Misc;

public class FileSystemSender<F, FS extends IFileSystem<F>> extends SenderWithParametersBase {

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
		IFileSystem<F> ifs = getFileSystem();
		F file;
		try {
			file = (F) ifs.toFile(message);
		} catch (Exception e) {
			throw new SenderException(getLogPrefix() + "unable to get file", e);
		}

		if (action.equalsIgnoreCase("delete")) {
			try {
				ifs.deleteFile(file);
			} catch (FileSystemException e) {
				e.printStackTrace();
			}
		} else if (action.equalsIgnoreCase("download")) {
			try {
				try {
					return Misc.streamToString(ifs.readFile(file));
				} catch (FileSystemException e) {
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (action.equalsIgnoreCase("list")) {
			try {
				ifs.listFiles();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (action.equalsIgnoreCase("upload")) {

		} else if (action.equalsIgnoreCase("mkdir")) {
			try {
				ifs.createFolder(file);
			} catch (FileSystemException e) {
				e.printStackTrace();
			}
		} else if (action.equalsIgnoreCase("rmdir")) {
			try {
				ifs.removeFolder(file);
			} catch (FileSystemException e) {
				e.printStackTrace();
			}
		} else if (action.equalsIgnoreCase("rename")) {

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