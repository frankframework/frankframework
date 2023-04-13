/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.monitoring;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StringUtil;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * MonitorAdapter that creates log lines for the GALM log adapter.
 *
 * configuration is done via the AppConstants 'galm.stage' and 'galm.source',
 * that in the default implemenation obtain their values from custom properties 'galm.stage' and
 * appConstant 'instance.name'.
 *
 * @author  Gerrit van Brakel
 * @since   4.7
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

	@Override
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

	public String getGalmRecord(String subSource, EventType eventType, Severity severity, String message) {
		if (subSource.indexOf(' ')>=0) {
			String replacement= StringUtil.replace(subSource," ","_");
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
			eventType.name()+" "+
			severity.name()+" "+
			dtapStage+" "+
			message;
		return result;
	}

	@Override
	public String makeXml(String eventSource, EventType eventType, Severity severity, String message, Throwable t) {
		XmlBuilder eventXml = new XmlBuilder("event");
		eventXml.addAttribute("hostname", hostname);
		eventXml.addAttribute("source",sourceId);
		eventXml.addAttribute("subSource",eventSource);
		eventXml.addAttribute("eventType",eventType.name());
		eventXml.addAttribute("severity",severity.name());
		eventXml.addAttribute("message",message);
		return eventXml.toXML();
	}

	@Override
	public void fireEvent(String subSource, EventType eventType, Severity severity, String message, Throwable t) {
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
