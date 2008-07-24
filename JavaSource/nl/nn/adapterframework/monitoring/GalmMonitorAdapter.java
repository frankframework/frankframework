/*
 * $Log: GalmMonitorAdapter.java,v $
 * Revision 1.7  2008-07-24 12:34:01  europe\L190409
 * rework
 *
 * Revision 1.6  2008/06/24 07:58:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reduce warnings to debug
 *
 * Revision 1.5  2008/05/21 10:52:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified monitorAdapter interface
 *
 * Revision 1.4  2007/12/12 09:09:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * truncated messages after newline
 *
 * Revision 1.3  2007/12/10 10:07:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added removal of special characters from sourceId
 *
 * Revision 1.2  2007/10/01 14:06:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified configuration keys
 *
 * Revision 1.1  2007/09/27 12:55:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of monitoring
 *
 */
package nl.nn.adapterframework.monitoring;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

/**
 * MonitorAdapter that creates log lines for the GALM log adapter.
 * 
 * configuration is done via the AppConstants 'galm.stage' and 'galm.source',
 * that in the default implemenation obtain their values from custom properties 'galm.stage' and
 * appConstant 'instance.name'.
 *  
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
public class GalmMonitorAdapter extends MonitorAdapterBase {
	protected Logger galmLog = LogUtil.getLogger("GALM");

	private String DTAP_STAGE_KEY="galm.stage";
	private String SOURCE_ID_KEY="galm.source";

	private String hostname;
	private String sourceId;
	private String dtapStage;
	
	private SimpleDateFormat dateTimeFormatter=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public GalmMonitorAdapter() throws ConfigurationException {
		super();
		configure(); 
	}

	public void configure() throws ConfigurationException {
		super.configure();
		hostname=Misc.getHostname();
		AppConstants appConstants = AppConstants.getInstance();
		sourceId=appConstants.getResolvedProperty(SOURCE_ID_KEY);
		if (StringUtils.isEmpty(sourceId)) {
			throw new ConfigurationException("cannot read sourceId from ["+SOURCE_ID_KEY+"]");
		}
		if (sourceId.indexOf(' ')>=0) {
			StringBuffer replacement=new StringBuffer();
			boolean replacementsMade=false;
			for (int i=0; i<sourceId.length(); i++) {
				char c=sourceId.charAt(i);
				if (Character.isLetterOrDigit(c)||c=='_') {
					replacement.append(c);
				} else { 
					replacement.append('_');
					replacementsMade=true;
				}
			}
			if (replacementsMade) {
				if (log.isDebugEnabled()) log.debug("sourceId ["+sourceId+"] read from ["+SOURCE_ID_KEY+"] contains spaces, replacing them with underscores, resulting in ["+replacement.toString()+"]");
				sourceId=replacement.toString();
			}
		}
		dtapStage=appConstants.getString(DTAP_STAGE_KEY,null);
		if (StringUtils.isEmpty(dtapStage)) {
			throw new ConfigurationException("cannot read dtapStage from ["+DTAP_STAGE_KEY+"]");
		}
		if (!("DEV".equals(dtapStage)) &&
			!("TEST".equals(dtapStage)) &&
			!("ACCEPT".equals(dtapStage)) &&
			!("PROD".equals(dtapStage))) {
				throw new ConfigurationException("dtapStage ["+dtapStage+"] read from ["+DTAP_STAGE_KEY+"] not equal to one of DEV, TEST, ACCEPT, PROD");
		}
	}

	public String getGalmRecord(String subSource, EventTypeEnum eventType, SeverityEnum severity, String message) {
		if (subSource.indexOf(' ')>=0) {
			String replacement=Misc.replace(subSource," ","_");
			if (log.isDebugEnabled()) log.debug("subSource ["+subSource+"] contains spaces, replacing them with underscores, resulting in ["+replacement+"]");
			subSource=replacement;
		}
		if (message!=null) {
			int npos=message.indexOf('\n');
			if (npos>=0) {
				message=message.substring(0,npos);
			}
			int rpos=message.indexOf('\r');
			if (rpos>=0) {
				message=message.substring(0,rpos);
			}
			message=message.trim();
			if (message.endsWith(":")) {
				message=message.substring(0,message.length()-1);
			}
		}
		String result=
			dateTimeFormatter.format(new Date()) +" "+
			hostname+" "+
			sourceId+" "+
			subSource+" "+
			eventType.getName()+" "+
			severity.getName()+" "+
			dtapStage+" "+
			message;
		return result;
	}

	public void fireEvent(String subSource, EventTypeEnum eventType, SeverityEnum severity, String message, Throwable t) {
		if (t!=null) {
			if (StringUtils.isEmpty(message)) {
				message = ClassUtils.nameOf(t);
			} else
			message += ": "+ ClassUtils.nameOf(t);
		}
		String galmRecord=getGalmRecord(subSource, eventType, severity, message);
		if (log.isDebugEnabled()) {
			log.debug("firing GALM event ["+galmRecord+"]");
		}
		galmLog.warn(galmRecord);
	}

}
