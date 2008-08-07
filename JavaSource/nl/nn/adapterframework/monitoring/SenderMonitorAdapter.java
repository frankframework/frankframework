/*
 * $Log: SenderMonitorAdapter.java,v $
 * Revision 1.1  2008-08-07 11:31:27  europe\L190409
 * rework
 *
 * Revision 1.3  2008/07/24 12:34:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework
 *
 * Revision 1.2  2008/05/21 10:52:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified monitorAdapter interface
 *
 * Revision 1.1  2007/09/27 12:55:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of monitoring
 *
 */
package nl.nn.adapterframework.monitoring;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.http.WebServiceSender;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;

/**
 * IMonitorAdapter that uses a {@link ISender} to send its message.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class SenderMonitorAdapter extends MonitorAdapterBase {

	private ISender sender;
	
	private boolean senderConfigured=false;

	public void configure() throws ConfigurationException {
		if (getSender()==null) {
			throw new ConfigurationException("must have sender configured");
		}
		if (StringUtils.isEmpty(getSender().getName())) {
			getSender().setName("sender of "+getName()); 
		}
		super.configure();
		if (!senderConfigured) {
			getSender().configure();
			senderConfigured=true;
		} else {
			try {
				getSender().close();
			} catch (SenderException e) {
				log.error("cannot close sender",e);
			}
		}
		try {
			getSender().open();
		} catch (SenderException e) {
			throw new ConfigurationException("cannot open sender",e);
		}
	}

	public void fireEvent(String subSource, EventTypeEnum eventType, SeverityEnum severity, String message, Throwable t) {
		try {
			getSender().sendMessage(null,makeXml(subSource,eventType,severity,message,t));
		} catch (Exception e) {
			log.error("Could not signal event",e);
		}
	}

	public void addNonDefaultAttribute(XmlBuilder senderXml, ISender sender, String attribute) {
		try {
			PropertyUtilsBean pub = new PropertyUtilsBean();

			if (pub.isReadable(sender,attribute) && pub.isWriteable(sender,attribute)) {
				String value = BeanUtils.getProperty(sender,attribute);

				Object defaultSender;
				Class[] classParm = null;
				Object[] objectParm = null;

				Class cl = sender.getClass();
				java.lang.reflect.Constructor co = cl.getConstructor(classParm);
				defaultSender = co.newInstance(objectParm);

				String defaultValue = BeanUtils.getProperty(defaultSender,attribute);				
				if (value!=null && !value.equals(defaultValue)) {
					senderXml.addAttribute(attribute,value);
				}
			}
		} catch (Exception e) {
			log.error("cannot retrieve attribute ["+attribute+"] from sender ["+ClassUtils.nameOf(sender)+"]");
		}
	}

	public XmlBuilder toXml() {
		XmlBuilder result=super.toXml();
		XmlBuilder senderXml=new XmlBuilder("sender");
		senderXml.addAttribute("className",getSender().getClass().getName());
		try {
			Map properties = BeanUtils.describe(sender);
			for (Iterator it=properties.keySet().iterator();it.hasNext();) {
				String property = (String)it.next();
				if (!property.equals("name")) {
					addNonDefaultAttribute(senderXml,sender,property);
				}
			}
		} catch (Exception e) {
			log.error("cannot set attributes of sender ["+ClassUtils.nameOf(sender)+"]");
		}
		result.addSubElement(senderXml);
		return result;
	}


	public void setSender(ISender sender) {
		this.sender = sender;
	}
	public ISender getSender() {
		return sender;
	}


}
