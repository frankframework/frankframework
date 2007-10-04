/*
 * $Log: GalmMonitorAdapter.java,v $
 * Revision 1.2.2.1  2007-10-04 13:25:56  europe\L190409
 * synchronize with HEAD (4.7.0)
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
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

/**
 * MonitorAdapter that creates log lines for the GALM log adapter.
 * 
 * configuration is done via the AppConstants 'monitor.galm.stage' and 'monitor.galm.source',
 * that in the default implemenation obtain their values from custom properties 'galm.stage' and
 * appConstant 'instance.name'.
 *  
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
public class GalmMonitorAdapter implements IMonitorAdapter {
	protected Logger log = LogUtil.getLogger(this);
	protected Logger galmLog = LogUtil.getLogger("GALM");

	private String DTAP_STAGE_KEY="monitor.galm.stage";
	private String SOURCE_ID_KEY="monitor.galm.source";

	private String hostname;
	private String sourceId;
	private String dtapStage;
	
	private SimpleDateFormat dateTimeFormatter=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public GalmMonitorAdapter() throws ConfigurationException {
		super();
		configure(); 
	}

	public void configure() throws ConfigurationException {
		hostname=Misc.getHostname();
		AppConstants appConstants = AppConstants.getInstance();
		sourceId=appConstants.getResolvedProperty(SOURCE_ID_KEY);
		if (StringUtils.isEmpty(sourceId)) {
			throw new ConfigurationException("cannot read sourceId from ["+SOURCE_ID_KEY+"]");
		}
		if (sourceId.indexOf(' ')>=0) {
			String replacement=Misc.replace(sourceId," ","_");
			log.warn("sourceId ["+sourceId+"] read from ["+SOURCE_ID_KEY+"] contains spaces, replacing them with underscores, resulting in ["+replacement+"]");
			sourceId=replacement;
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
			log.warn("subSource ["+subSource+"] contains spaces, replacing them with underscores, resulting in ["+replacement+"]");
			subSource=replacement;
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

	public void fireEvent(String subSource, EventTypeEnum eventType, SeverityEnum severity, String message) {
		String galmRecord=getGalmRecord(subSource, eventType, severity, message);
		if (log.isDebugEnabled()) {
			log.debug("firing GALM event ["+galmRecord+"]");
		}
		galmLog.warn(galmRecord);
	}

}
