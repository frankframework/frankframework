/*
 * $Log: FxfSender.java,v $
 * Revision 1.7  2008-12-30 17:01:12  m168309
 * added configuration warnings facility (in Show configurationStatus)
 *
 * Revision 1.6  2008/09/04 12:05:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * test for version of fxf
 *
 * Revision 1.5  2008/08/27 15:55:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added remote filename option
 *
 * Revision 1.4  2008/02/22 14:29:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * implement ISender
 *
 * Revision 1.3  2008/02/21 12:42:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed default of script
 * options to delete or backup file after sending
 *
 * Revision 1.2  2008/02/19 09:39:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.1  2008/02/13 12:53:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of FxF components
 *
 */
package nl.nn.adapterframework.extensions.fxf;

import java.io.File;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.ProcessUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Sender for transferring files using the FxF protocol. Assumes sender input is local filename.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.extensions.fxf.FxfListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setScript(String) script}</td><td>full pathname to the FXF script to be executed to transfer the file</td><td>/usr/local/bin/FXF_init</td></tr>
 * <tr><td>{@link #setTransfername(String) transfername}</td><td>FXF transfername</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProcessedDirectory(String) processedDirectory}</td><td>Directory where files are stored after being processed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNumberOfBackups(int) numberOfBackups}</td><td>number of copies held of a file with the same name. Backup files have a dot and a number suffixed to their name. If set to 0, no backups will be kept.</td><td>5</td></tr>
 * <tr><td>{@link #setOverwrite(boolean) overwrite}</td><td>when set <code>true</code>, the destination file will be deleted if it already exists</td><td>false</td></tr>
 * <tr><td>{@link #setDelete(boolean) delete}</td><td>when set <code>true</code>, the file processed will deleted after being processed, and not stored</td><td>true</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>remoteFilename</td><td><i>String</i></td><td>remote filename, used as 4th parameter of fxf command. When not specified, the remote filename is determined by FXF from the its configuration</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.8
 * @version Id
 */
public class FxfSender extends SenderWithParametersBase {
	protected Logger log = LogUtil.getLogger(this);

	public static final String REMOTE_FILENAME_PARAM="remoteFilename";

	private String name;
	private String script="/usr/local/bin/FXF_init";
	private String transfername;
	private String processedDirectory;
	private int numberOfBackups = 0;
	private boolean overwrite = false;
	private boolean delete = true;
	
	private Parameter remoteFilenameParam=null;

	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getTransfername())) {
			throw new ConfigurationException("FxfSender ["+getName()+"] must specify transfername");
		}
		if (paramList!=null && paramList.size()>0) {
			remoteFilenameParam=(Parameter)paramList.get(0);
			if (!REMOTE_FILENAME_PARAM.equalsIgnoreCase(remoteFilenameParam.getName())) {
				ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
				String msg = getLogPrefix()+"name of parameter for remote filename ["+remoteFilenameParam.getName()+"] is not equal to ["+REMOTE_FILENAME_PARAM+"], as expected. Using it anyway";
				configWarnings.add(log, msg);
			}
		}
		if (log.isDebugEnabled()) {
			String version=FxfUtil.getVersion(getScript());
			log.debug(getLogPrefix()+"FxF version ["+version+"]");
		}
	}


	public boolean isSynchronous() {
		return false;
	}

	public String sendMessage(String correlationID, String message) throws SenderException, TimeOutException {
		return sendMessage(correlationID, message, null);
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException {
		String command = getScript()+" put "+getTransfername() +" "+message;
		String remoteFilename = null;
		if (remoteFilenameParam!=null && prc!=null) {
			try {
				remoteFilename=(String)prc.getValues(paramList).getParameterValue(0).getValue();
				command += " "+remoteFilename;
			} catch (ParameterException e) {
				throw new SenderException("Could not resolve remote filename", e);
			}
		}
		log.debug("sending local file ["+message+"] by executing command ["+command+"]");
		String execResult=ProcessUtil.executeCommand(command);
		log.debug("output of command ["+execResult+"]");
		
		// delete file or move it to processed directory
		if (isDelete() || StringUtils.isNotEmpty(getProcessedDirectory())) {
			File f=new File(message);
			try {
				log.debug("moving or deleteing file ["+message+"]");
				FileUtils.moveFileAfterProcessing(f, getProcessedDirectory(), isDelete(), isOverwrite(), getNumberOfBackups()); 
			} catch (Exception e) {
				throw new SenderException("Could not move file ["+message+"]",e);
			}
		}
		return execResult;
	}

	public void setScript(String string) {
		script = string;
	}
	public String getScript() {
		return script;
	}

	public void setTransfername(String string) {
		transfername = string;
	}
	public String getTransfername() {
		return transfername;
	}

	public void setProcessedDirectory(String processedDirectory) {
		this.processedDirectory = processedDirectory;
	}
	public String getProcessedDirectory() {
		return processedDirectory;
	}

	public void setNumberOfBackups(int i) {
		numberOfBackups = i;
	}
	public int getNumberOfBackups() {
		return numberOfBackups;
	}

	public void setOverwrite(boolean b) {
		overwrite = b;
	}
	public boolean isOverwrite() {
		return overwrite;
	}

	public void setDelete(boolean b) {
		delete = b;
	}
	public boolean isDelete() {
		return delete;
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name=name;
	}

}
