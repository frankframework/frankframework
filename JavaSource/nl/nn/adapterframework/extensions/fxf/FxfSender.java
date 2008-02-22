/*
 * $Log: FxfSender.java,v $
 * Revision 1.4  2008-02-22 14:29:22  europe\L190409
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
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.ProcessUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Sender for transferring files using the FxF protocol. Assumes pipe input is local name
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
 * 
 * @author  Gerrit van Brakel
 * @since   4.8
 * @version Id
 */
public class FxfSender implements ISender {
	protected Logger log = LogUtil.getLogger(this);

	private String name;
	private String script="/usr/local/bin/FXF_init";
	private String transfername;
	private String processedDirectory;
	private int numberOfBackups = 0;
	private boolean overwrite = false;
	private boolean delete = true;

	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getTransfername())) {
			throw new ConfigurationException("FxfSender ["+getName()+"] must specify transfername");
		}
	}

	public void open() throws SenderException {
	}

	public void close() throws SenderException {
	}

	public boolean isSynchronous() {
		return false;
	}

	public String sendMessage(String correlationID, String message) throws SenderException, TimeOutException {
		return sendMessage(correlationID, message, null);
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException {
//		return super.sendMessage(correlationID, message, prc);
		String command = getScript()+" put "+getTransfername() +" "+message;
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
