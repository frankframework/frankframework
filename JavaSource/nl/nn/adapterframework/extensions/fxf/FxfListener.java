/*
 * $Log: FxfListener.java,v $
 * Revision 1.2  2008-02-15 13:58:25  europe\L190409
 * added attributes processedDirectory, numberOfBackups, overwrite and delete
 *
 * Revision 1.1  2008/02/13 12:53:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of FxF components
 *
 */
package nl.nn.adapterframework.extensions.fxf;

import java.io.File;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jms.JmsListener;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.ProcessUtil;
import nl.nn.adapterframework.util.TransformerPool;

import org.apache.commons.lang.StringUtils;

/**
 * Listener for files transferred using the FxF protocol. Message handed to the pipeline is the local filename.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.extensions.fxf.FxfListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setScript(String) script}</td><td>full pathname to the FXF script to be executed to transfer the file</td><td>/usr/local/bin/FXF_get</td></tr>
 * <tr><td>{@link #setDestinationName(String) destinationName}</td><td>name of the JMS destination (queue or topic) to use</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProcessedDirectory(String) processedDirectory}</td><td>Directory where files are stored after being processed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNumberOfBackups(String) numberOfBackups}</td><td>number of copies held of a file with the same name. Backup files have a dot and a number suffixed to their name. If set to 0, no backups will be kept.</td><td>0</td></tr>
 * <tr><td>{@link #setOverwrite(boolean) overwrite}</td><td>overwrite the file in the output directory if it already exist</td><td>false</td></tr>
 * <tr><td>{@link #setDelete(boolean) delete}</td><td>when set <code>true</code>the file processed will deleted after being processed, and not stored</td><td>false</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.8
 * @version Id
 */
public class FxfListener extends JmsListener {

	public static final String EXTRACT_TRANSFERNAME_DXPATH="FXF/Transfer_name";
	public static final String EXTRACT_LOCALNAME_DXPATH="FXF/Local_File";
	
	private String script;
	private String processedDirectory;
	private int numberOfBackups = 0;
	private boolean overwrite = false;
	private boolean delete = false;

	private TransformerPool extractTransfername;
	private TransformerPool extractLocalname;
	
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getScript())) {
			throw new ConfigurationException("attribute 'script' empty, please specify (e.g. /usr/local/bin/FXF_init)");
		}
		extractTransfername=TransformerPool.configureTransformer(getLogPrefix(),EXTRACT_TRANSFERNAME_DXPATH,null,"text",false,null);
		extractLocalname=TransformerPool.configureTransformer(getLogPrefix(),EXTRACT_LOCALNAME_DXPATH,null,"text",false,null);
	}
	
	public String getStringFromRawMessage(Object rawMessage, Map threadContext) throws ListenerException {
		String message=super.getStringFromRawMessage(rawMessage, threadContext);
		String transfername;
		String localname;
		try {
			transfername=extractTransfername.transform(message,null);
			localname=extractLocalname.transform(message,null);
		} catch (Exception e) {
			throw new ListenerException("could not extract name from message ["+message+"]");
		}
		String command = getScript()+" get "+transfername+" "+localname;
		log.debug(getLogPrefix()+"retrieving local file ["+localname+"] by executing command ["+command+"]");
		try {
			String execResult=ProcessUtil.executeCommand(command);
			log.debug(getLogPrefix()+"output of command ["+execResult+"]");
		} catch (SenderException e1) {
			throw new ListenerException(e1);
		}
		return localname;
	}

	public void afterMessageProcessed(PipeLineResult plr, Object rawMessage, Map threadContext) throws ListenerException { 
		super.afterMessageProcessed(plr, rawMessage, threadContext);
		if (isDelete() || StringUtils.isNotEmpty(getProcessedDirectory())) {
			String message=super.getStringFromRawMessage(rawMessage, threadContext);
			String localname;
			try {
				localname=extractLocalname.transform(message,null);
			} catch (Exception e) {
				throw new ListenerException("could not extract name from message ["+message+"]");
			}
			File f=new File(localname);
			try {
				FileUtils.moveFileAfterProcessing(f, getProcessedDirectory(), isDelete(), isOverwrite(), getNumberOfBackups()); 
			} catch (Exception e) {
				throw new ListenerException("Could not move file ["+localname+"]",e);
			}
		}
	}


	public void setScript(String string) {
		script = string;
	}
	public String getScript() {
		return script;
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

}
