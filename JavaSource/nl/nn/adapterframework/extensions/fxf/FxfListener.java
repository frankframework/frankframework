/*
 * $Log: FxfListener.java,v $
 * Revision 1.8  2008-04-17 12:57:45  europe\L190409
 * change transfername to applicationId
 *
 * Revision 1.7  2008/02/26 12:53:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected calculation of messageSelector from transfername
 *
 * Revision 1.6  2008/02/26 09:39:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added transfername attribute
 *
 * Revision 1.5  2008/02/22 14:37:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * store transfername and local filename in threadcontext
 *
 * Revision 1.4  2008/02/21 12:35:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed default of script
 * added signalling of file processed
 *
 * Revision 1.3  2008/02/19 09:39:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.2  2008/02/15 13:58:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import nl.nn.adapterframework.util.JtaUtil;
import nl.nn.adapterframework.util.ProcessUtil;
import nl.nn.adapterframework.util.TransformerPool;

import org.apache.commons.lang.StringUtils;

/**
 * Listener for files transferred using the FxF protocol. Message handed to the pipeline is the local filename.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.extensions.fxf.FxfListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setScript(String) script}</td><td>full pathname to the FXF script to be executed to transfer the file</td><td>/usr/local/bin/FXF_init</td></tr>
 * <tr><td>{@link #setApplicationId(String) applicationId}</td><td>FXF ApplicationID, will be converted into hexadecimal messageselector</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestinationName(String) destinationName}</td><td>name of the JMS destination (queue or topic) to use</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setQueueConnectionFactoryName(String) queueConnectionFactoryName}</td><td>jndi-name of the queueConnectionFactory, used when <code>destinationType<code>=</code>QUEUE</code></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageSelector(String) messageSelector}</td><td>When set, the value of this attribute is used as a selector to filter messages.</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setProcessedDirectory(String) processedDirectory}</td><td>Directory where files are stored after being processed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNumberOfBackups(int) numberOfBackups}</td><td>number of copies held of a file with the same name. Backup files have a dot and a number suffixed to their name. If set to 0, no backups will be kept.</td><td>5</td></tr>
 * <tr><td>{@link #setOverwrite(boolean) overwrite}</td><td>when set <code>true</code>, the destination file will be deleted if it already exists</td><td>false</td></tr>
 * <tr><td>{@link #setDelete(boolean) delete}</td><td>when set <code>true</code>, the file processed will deleted after being processed, and not stored</td><td>false</td></tr>
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

	public static final String TRANSFERNAME_SESSION_KEY="FxfTransferName";
	public static final String LOCALNAME_SESSION_KEY="FxfLocalFile";
	
	private String script="/usr/local/bin/FXF_init";
	private String processedDirectory;
	private int numberOfBackups = 0;
	private boolean overwrite = false;
	private boolean delete = false;
	private String applicationId;

	private TransformerPool extractTransfername;
	private TransformerPool extractLocalname;
	
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getScript())) {
			throw new ConfigurationException("attribute 'script' empty, please specify (e.g. /usr/local/bin/FXF_init)");
		}
		if (StringUtils.isNotEmpty(getApplicationId()) && StringUtils.isNotEmpty(super.getMessageSelector())) {
			throw new ConfigurationException("cannot specify both applicationId and messageSelector");
		}
		if (StringUtils.isEmpty(getApplicationId()) && StringUtils.isEmpty(super.getMessageSelector())) {
			throw new ConfigurationException("either messageSelector or applicationId must be specified");
		}
		extractTransfername=TransformerPool.configureTransformer(getLogPrefix(),EXTRACT_TRANSFERNAME_DXPATH,null,"text",false,null);
		extractLocalname=TransformerPool.configureTransformer(getLogPrefix(),EXTRACT_LOCALNAME_DXPATH,null,"text",false,null);
	}
	
	public String getStringFromRawMessage(Object rawMessage, Map threadContext) throws ListenerException {
		String message=super.getStringFromRawMessage(rawMessage, threadContext);
		log.debug(getLogPrefix()+"retrieved FXF message ["+message+"]");
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
		threadContext.put(TRANSFERNAME_SESSION_KEY,transfername);
		threadContext.put(LOCALNAME_SESSION_KEY,localname);
		return localname;
	}

	public void afterMessageProcessed(PipeLineResult plr, Object rawMessage, Map threadContext) throws ListenerException { 
		super.afterMessageProcessed(plr, rawMessage, threadContext);

		String transfername=(String)threadContext.get(TRANSFERNAME_SESSION_KEY);
		String localname=(String)threadContext.get(LOCALNAME_SESSION_KEY);
		if (StringUtils.isEmpty(transfername)) {
			throw new ListenerException("could not extract FXF transfername from session key ["+TRANSFERNAME_SESSION_KEY+"]");
		}
		if (StringUtils.isEmpty(localname)) {
			throw new ListenerException("could not extract FXF localname from session key ["+LOCALNAME_SESSION_KEY+"]");
		}
	
//		log.debug("FXF transaction status1:"+JtaUtil.displayTransactionStatus());
//
//		TransactionStatus txStatus=null;
//		try {
//			txStatus=TransactionAspectSupport.currentTransactionStatus();
//			log.debug("FXF transaction status2:"+JtaUtil.displayTransactionStatus(txStatus));
//		} catch (NoTransactionException e) {
//			log.debug("not in transaction: "+e.getMessage());
//		}

		if (JtaUtil.isRollbackOnly()) {
			log.info(getLogPrefix()+"transaction status is RollbackOnly, will not confirm processing to FXF");		
		} else {
			// confirm processing of file
			String command = getScript()+" processed "+transfername;
			log.debug(getLogPrefix()+"confirming FXF processing of file ["+localname+"] by executing command ["+command+"]");
			try {
				String execResult=ProcessUtil.executeCommand(command);
				log.debug(getLogPrefix()+"output of command ["+execResult+"]");
			} catch (SenderException e1) {
				throw new ListenerException(e1);
			}
		}

		// delete file or move it to processed directory
		if (isDelete() || StringUtils.isNotEmpty(getProcessedDirectory())) {
			File f=new File(localname);
			try {
				FileUtils.moveFileAfterProcessing(f, getProcessedDirectory(), isDelete(), isOverwrite(), getNumberOfBackups()); 
			} catch (Exception e) {
				throw new ListenerException("Could not move file ["+localname+"]",e);
			}
		}
	}

	public String getMessageSelector() {
		String result=super.getMessageSelector();
		if (StringUtils.isNotEmpty(result) || StringUtils.isEmpty(getApplicationId())) {
			return result;
		}
		String applicationId=getApplicationId();
		result="JMSCorrelationID='ID:";
		int i;
		for (i=0;i<applicationId.length();i++) {
			int c=applicationId.charAt(i);
			result+=Integer.toHexString(c);
		};
		for (;i<24;i++) {
			result+="00";		
		}
		result+="'";
		return result;
	}

	public String getPhysicalDestinationName() {
		String result = super.getPhysicalDestinationName();
		if (StringUtils.isNotEmpty(getApplicationId())) {
			result += " applicationId ["+getApplicationId()+"]";
		}
		return result;
	}

	public void setScript(String string) {
		script = string;
	}
	public String getScript() {
		return script;
	}

	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}
	public String getApplicationId() {
		return applicationId;
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
