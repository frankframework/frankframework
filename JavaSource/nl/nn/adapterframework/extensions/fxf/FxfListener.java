/*
 * $Log: FxfListener.java,v $
 * Revision 1.14  2009-03-04 15:56:57  L190409
 * added support for FXF 2.0
 *
 * Revision 1.13  2008/09/04 12:05:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * test for version of fxf
 *
 * Revision 1.12  2008/07/24 14:10:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid trying to delete file twice
 *
 * Revision 1.11  2008/07/24 12:31:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fix
 *
 * Revision 1.10  2008/06/30 08:55:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * only commit file reception in case of success
 * otherwise only delete local file
 *
 * Revision 1.9  2008/05/14 09:34:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * documented session variables set
 *
 * Revision 1.8  2008/04/17 12:57:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import java.util.List;
import java.util.Map;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.Session;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IBulkDataListener;
import nl.nn.adapterframework.core.IListenerConnector;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jms.JmsListener;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.ProcessUtil;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;


/**
 * Listener for files transferred using the FxF protocol. The message handed to the pipeline is the local filename.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.extensions.fxf.FxfListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setScript(String) script}</td><td>full pathname to the FXF script to be executed to transfer the file</td><td>/usr/local/bin/FXF_init</td></tr>
 * <tr><td>{@link #setApplicationId(String) applicationId}</td><td>(FXF 1 only) FXF ApplicationID, will be converted into hexadecimal messageselector</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestinationName(String) destinationName}</td><td>name of the JMS queue where trigger messages are received from</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFxfDestinationName(String) fxfDestinationName}</td><td>(FXF 2 only) name of the JMS queue used to send processedGetFile messages</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setQueueConnectionFactoryName(String) queueConnectionFactoryName}</td><td>jndi-name of the queueConnectionFactory, used when <code>destinationType<code>=</code>QUEUE</code></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageSelector(String) messageSelector}</td><td>When set, the value of this attribute is used as a selector to filter messages.</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setWorkDirectory(String) workDirectory}</td><td>(FXF 2 only) Directory where files are stored while being processed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTransferTimeout(int) transferTimeout}</td><td>(FXF 2 only) maximum time in seconds allowed for file transfer</td><td>120</td></tr>
 * <tr><td>{@link #setFxf2Compatibility(boolean) fxf2Compatibility}</td><td>when set <code>true</code>, attributes only required for FXF 2.0 will be mandatory for FXF 1.3 too</td><td>true</td></tr>
 * <tr><td>{@link #setProcessedDirectory(String) processedDirectory}</td><td>Directory where files are stored after being processed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNumberOfBackups(int) numberOfBackups}</td><td>number of copies held of a file with the same name. Backup files have a dot and a number suffixed to their name. If set to 0, no backups will be kept.</td><td>5</td></tr>
 * <tr><td>{@link #setOverwrite(boolean) overwrite}</td><td>when set <code>true</code>, the destination file will be deleted if it already exists</td><td>false</td></tr>
 * <tr><td>{@link #setDelete(boolean) delete}</td><td>when set <code>true</code>, the file processed will deleted after being processed, and not stored</td><td>false</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setForceMQCompliancy(String) forceMQCompliancy}</td><td>If the MQ destination is not a JMS receiver, format errors occur.
	 To prevent this, settting <code>forceMQCompliancy</code> to MQ will inform
	 MQ that the replyto queue is not JMS compliant. Setting <code>forceMQCompliancy</code>
	 to "JMS" will cause that on mq the destination is identified as jms-compliant.</td><td>MQ</td></tr>
 * <tr><td>{@link #setXmlSchema(String) xmlSchema}</td><td>(FXF 2 only) XML Schema to validate trigger messages</td><td>Fxf 2.0.xsd</td></tr>
 * </table>
 * </p>
 * <b>session variables set:</b>
 * <table border="1">
 * <tr><th>name</th><th>description</th></tr>
 * <tr><td>FxfTransferName</td><td>transfername of the file received</td></tr>
 * </table>
 * <h2>fxf_init returncodes (FXF 1.3)</h2>
 * <table>
<tr><th>Returncode</th><th>Reason</th><th>Action required</th></tr>
<tr><td>0</td><td>all went well</td><td>none</td></tr>
<tr><td>1</td><td>remote file not deleted</td><td>delete the file manually</td></tr>
<tr><td>2</td><td>local file not deleted</td><td>delete the file manually</td></tr>
<tr><td>3</td><td>both local- and remote file not deleted</td><td>delete the files manually</td></tr>
<tr><td>5</td><td>function/transfer_name combination not found in FXF configuration</td><td>check function/transfer_name combination provided and FXF configuration</td></tr>
<tr><td>6</td><td>for indirect transfer: MQ put failed</td><td>Check QManager and Queue as found in FXF configuration</td></tr>
<tr><td>7</td><td>for direct transfer: xcomtcp command failed</td><td>Check XCOM configuration as found in FXF configuration</td></tr>
</table>
 * </p>
 * @author  Gerrit van Brakel
 * @since   4.8
 * @version Id
 */
public class FxfListener extends JmsListener implements IBulkDataListener {

	public static final String FXF1_EXTRACT_TRANSFERNAME_XPATH="FXF/Transfer_name";
	public static final String FXF1_EXTRACT_LOCALNAME_XPATH="FXF/Local_File";
	public static final String FXF1_EXTRACT_REMOTENAME_XPATH="FXF/Remote_File";

	public static final String TRANSFERNAME_SESSION_KEY="FxfTransferName";
	public static final String LOCALNAME_SESSION_KEY="FxfLocalFile";
	public static final String REMOTENAME_SESSION_KEY="FxfRemoteFile";
	public static final String TRIGGER_SESSION_KEY="FxfTrigger";
	
	public static final String FXF2_SCHEMA_DEFAULT="Fxf 2.0.xsd";
	
	
	private String script="/usr/local/bin/FXF_init";
	private String processedDirectory;
	private int numberOfBackups = 0;
	private boolean overwrite = false;
	private boolean delete = false;
	private String applicationId;
	private String workDirectory;
	private int transferTimeout=120;
	private String fxfDestinationName;
	private String xmlSchema=FXF2_SCHEMA_DEFAULT;
	private boolean fxf2Compatibility=true;

	private TransformerPool extractTransfername=null;
	private TransformerPool extractLocalname=null;
	private TransformerPool extractRemotename=null;
	private Destination fxfDestination;

	private boolean version1=true;

	public FxfListener() {
		super();
		setForceMQCompliancy("MQ");
	}

	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getScript())) {
			throw new ConfigurationException("attribute script must be specified");
		}
//		File f = new File(getScript());
//		if (!f.exists()) {
//			throw new ConfigurationException("script ["+getScript()+"] does not exist");
//		}
		version1=!FxfUtil.isAtLeastVersion2(getScript());
		super.configure();
		if (StringUtils.isEmpty(getScript())) {
			throw new ConfigurationException("attribute 'script' empty, please specify (e.g. /usr/local/bin/FXF_init)");
		}
		if (version1) {
			if (StringUtils.isNotEmpty(getApplicationId()) && StringUtils.isNotEmpty(super.getMessageSelector())) {
				throw new ConfigurationException("FxF 1: cannot specify both applicationId and messageSelector");
			}
			if (StringUtils.isEmpty(getApplicationId()) && StringUtils.isEmpty(super.getMessageSelector())) {
				throw new ConfigurationException("FxF 1: either messageSelector or applicationId must be specified, or switch to FxF 2 by configuring a FxF 2 script");
			}
			if (isFxf2Compatibility() && StringUtils.isEmpty(getWorkDirectory())) {
				throw new ConfigurationException("please specify workDirectory for FxF 2 Compatibility, or set fxf2Compatibility to false");
			}
			if (isFxf2Compatibility() && StringUtils.isEmpty(getFxfDestinationName())) {
				throw new ConfigurationException("please specify fxfDestinationName for FxF 2 Compatibility, or set fxf2Compatibility to false");
			}
			extractTransfername=TransformerPool.configureTransformer(getLogPrefix(),FXF1_EXTRACT_TRANSFERNAME_XPATH,null,"text",false,null);
			extractLocalname=TransformerPool.configureTransformer(getLogPrefix(),FXF1_EXTRACT_LOCALNAME_XPATH,null,"text",false,null);
			extractRemotename=TransformerPool.configureTransformer(getLogPrefix(),FXF1_EXTRACT_REMOTENAME_XPATH,null,"text",false,null);
		} else {
			if (StringUtils.isNotEmpty(getApplicationId())) {
				log.warn(getLogPrefix()+"applicationId ["+getApplicationId()+"] is not used in this version of FXF");
			}
			if (StringUtils.isEmpty(getWorkDirectory())) {
				throw new ConfigurationException("workDirectory must be specified");
			}
			if (StringUtils.isEmpty(getFxfDestinationName())) {
				throw new ConfigurationException("fxfDestinationName must be specified");
			}
			try {
				fxfDestination=getDestination(getFxfDestinationName());
			} catch (Exception e) {
				throw new ConfigurationException("cannot obtain fxf queue" ,e);
			}
		}
	}
	
	public String retrieveBulkData(Object rawMessage, String message, Map context) throws ListenerException {
		String transfername;
		String localname;
		if (version1) {
			String remotename;
			try {
				transfername=extractTransfername.transform(message,null);
			} catch (Exception e) {
				throw new ListenerException("could not transfername name from message ["+message+"]");
			}
			try {
					localname=extractLocalname.transform(message,null);
			} catch (Exception e) {
				throw new ListenerException("could not extract localname from message ["+message+"]");
			}
			try {
				remotename=extractRemotename.transform(message,null);
			} catch (Exception e) {
				throw new ListenerException("could not extract remotename from message ["+message+"]");
			}
			if (context!=null) {
				context.put(TRANSFERNAME_SESSION_KEY,transfername);
				context.put(LOCALNAME_SESSION_KEY,localname);
				context.put(REMOTENAME_SESSION_KEY,remotename);
			} else {
				log.warn(getLogPrefix()+"cannot store transfername ["+transfername+"], localname ["+localname+"], remotename ["+remotename+"], as context is null");
			}

			String command = getScript()+" get "+transfername+" "+localname;
			if (StringUtils.isNotEmpty(remotename)) {
				command += " "+remotename;
			}
			log.debug(getLogPrefix()+"retrieving local file ["+localname+"] by executing command ["+command+"]");
			try {
				String execResult=ProcessUtil.executeCommand(command);
				log.debug(getLogPrefix()+"output of command ["+execResult+"]");
			} catch (SenderException e1) {
				throw new ListenerException(e1);
			}
		} else {
			if (StringUtils.isNotEmpty(getXmlSchema())) {
				log.debug("asserting FXF trigger message ["+message+"] to schema ["+getXmlSchema()+"]");
				XmlUtils.assertValidToSchema(message,getXmlSchema(),false,"FXF");
			}
			Trigger trigger = FxfUtil.parseTrigger(message);
			if (context!=null) {
				context.put(TRIGGER_SESSION_KEY,trigger);
			} else {
				throw new ListenerException("cannot store trigger, as context is null");
			}
			String transporthandle=trigger.getTransporthandle();
			if (StringUtils.isEmpty(transporthandle)) {
				throw new ListenerException("cannot find transporthandle from message ["+message+"]");
			}
			List transfers=trigger.getTransfers();
			if (transfers==null || transfers.size()<1) {
				throw new  ListenerException("no transfers in trigger message ["+message+"]");
			}
			if (transfers.size()>1) {
				throw new  ListenerException("multiple transfers in trigger message ["+message+"] not yet implemented");
			}
			Transfer transfer=trigger.getTransfer(0);
			transfername=transfer.getName();
			String command = getScript()+" get "+transporthandle +" "+ transfername+" "+getWorkDirectory() +" "+ getTimeOut();
			log.debug(getLogPrefix()+"retrieving file for transport ["+transporthandle+"] by executing command ["+command+"]");
			try {
				localname=ProcessUtil.executeCommand(command);
				log.debug(getLogPrefix()+"output of command ["+localname+"]");
				if (localname!=null) {
					localname=localname.trim();
				}
				context.put(LOCALNAME_SESSION_KEY,localname);
			} catch (SenderException e1) {
				throw new ListenerException(e1);
			}
		}

		return localname;
	}

	public void afterMessageProcessed(PipeLineResult plr, Object rawMessage, Map threadContext) throws ListenerException { 
		super.afterMessageProcessed(plr, rawMessage, threadContext);

		String transfername=(String)threadContext.get(TRANSFERNAME_SESSION_KEY);
		String localname=(String)threadContext.get(LOCALNAME_SESSION_KEY);
	
		if (plr==null || !"success".equals(plr.getState())) {
			if (StringUtils.isNotEmpty(localname)) {
				log.warn(getLogPrefix()+"pipeLineExitState not equal to success, will not confirm processing to FXF, only delete local file");
				File f=new File(localname);
				f.delete();		
			} else {
				log.warn(getLogPrefix()+"pipeLineExitState not equal to success, don't know local filename, so assume that it does not exist");
			}
		} else {
			if (version1) {
				String remotename=(String)threadContext.get(REMOTENAME_SESSION_KEY);
				if (StringUtils.isEmpty(transfername)) {
					throw new ListenerException("could not extract FXF transfername from session key ["+TRANSFERNAME_SESSION_KEY+"]");
				}
				if (StringUtils.isEmpty(localname)) {
					throw new ListenerException("could not extract FXF localname from session key ["+LOCALNAME_SESSION_KEY+"]");
				}
				// confirm processing of file
				String command = getScript()+" processed "+transfername+ " "+ localname;
				if (StringUtils.isNotEmpty(remotename)) {
					command+=" "+remotename;
				}
				log.debug(getLogPrefix()+"confirming FXF processing of file ["+localname+"] by executing command ["+command+"]");
				try {
					String execResult=ProcessUtil.executeCommand(command);
					log.debug(getLogPrefix()+"output of command ["+execResult+"]");
				} catch (SenderException e1) {
					throw new ListenerException(e1);
				}
			} else {
				String cid     = (String) threadContext.get(PipeLineSession.technicalCorrelationIdKey);
				Trigger trigger=(Trigger)threadContext.get(TRIGGER_SESSION_KEY);
				String processedMsg=FxfUtil.makeProcessedGetFileMessage(trigger.getTransporthandle());
				Session session= (Session) threadContext.get(IListenerConnector.THREAD_CONTEXT_SESSION_KEY); 
				// session is/must be saved in threadcontext by JmsConnector.
				// However, if this is a retry, no session is present....
				boolean sessionNotPresent=session==null;
				try {
					if (sessionNotPresent) {
						session=createSession();
					}
					send(session, fxfDestination, cid, processedMsg, getReplyMessageType(), 0, DeliveryMode.PERSISTENT, getReplyPriority());
				} catch (Exception e) {
					throw new ListenerException("cannot send processed message ["+processedMsg+"]",e);
				} finally {
					if (sessionNotPresent) {
						closeSession(session);
					}
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
	}

	public String getMessageSelector() {
		String result=super.getMessageSelector();
		if (StringUtils.isNotEmpty(result) || !version1 || StringUtils.isEmpty(getApplicationId())) {
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
		if (version1 && StringUtils.isNotEmpty(getApplicationId())) {
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

	public void setWorkDirectory(String string) {
		workDirectory = string;
	}
	public String getWorkDirectory() {
		return workDirectory;
	}

	public void setTransferTimeout(int i) {
		transferTimeout = i;
	}
	public int getTransferTimeout() {
		return transferTimeout;
	}

	public void setFxfDestinationName(String string) {
		fxfDestinationName = string;
	}
	public String getFxfDestinationName() {
		return fxfDestinationName;
	}

	public void setXmlSchema(String string) {
		xmlSchema = string;
	}
	public String getXmlSchema() {
		return xmlSchema;
	}

	public void setFxf2Compatibility(boolean b) {
		fxf2Compatibility = b;
	}
	public boolean isFxf2Compatibility() {
		return fxf2Compatibility;
	}

}
