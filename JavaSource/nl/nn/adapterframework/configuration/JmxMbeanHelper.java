/*
 * $Log: JmxMbeanHelper.java,v $
 * Revision 1.7  2006-09-14 11:44:09  europe\L190409
 * correctede logger definition
 *
 * Revision 1.6  2004/10/25 08:30:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected DisplayName
 *
 * Revision 1.5  2004/08/23 13:07:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated JavaDoc
 *
 */
package nl.nn.adapterframework.configuration;

import javax.management.ObjectName;
import javax.management.modelmbean.RequiredModelMBean;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import javax.management.modelmbean.ModelMBeanOperationInfo;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanInfo;

import nl.nn.adapterframework.core.Adapter;
import org.apache.log4j.Logger;
import java.util.ArrayList;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.JmxUtils;

/**
 * Jmx helper class, to make JMX MBeans from {@link nl.nn.adapterframework.core.Adapter Adapters}. 
 * @author Johan Verrips
 * @version Id
 * @since 4.1.1
 */
public class JmxMbeanHelper {
	public static final String version = "$RCSfile: JmxMbeanHelper.java,v $ $Revision: 1.7 $ $Date: 2006-09-14 11:44:09 $";
 	private static Logger log = Logger.getLogger(JmxMbeanHelper.class);
 	

	/**
	 * Hooks an {@link nl.nn.adapterframework.core.Adapter Adapter} to the MBean server
	 * @param adapter the adapter
	 * @throws ConfigurationException when something goes wrong
	 */
	public static void hookupAdapter(Adapter adapter)
		throws ConfigurationException {
			String objectNameName = "IBIS-"+AppConstants.getInstance().getResolvedProperty("instance.name")+"-"+"Adapters:name=" + adapter.getName();
			
			try {
			ObjectName tpMBeanName = new ObjectName(objectNameName);
			
			RequiredModelMBean modelMbean =
				new RequiredModelMBean(createMBeanInfo(adapter));
			modelMbean.setManagedResource(adapter, "ObjectReference");
			log.debug("modelMBean generated for object " + objectNameName);
			registerMBean(tpMBeanName, modelMbean);
			} catch(Exception e){
				throw new ConfigurationException (e);						
			}
		return;
	}
	

	/**
	 * Registers an mBean at an MBeanServer. If there is already an mbean registered 
	 * under the specified name, it is first de-registered.
	 * @param name	the objectName
	 * @param mbean	the modelMbean to register
	 * @throws ConfigurationException
	 */
	public static void registerMBean(ObjectName name, RequiredModelMBean mbean) throws ConfigurationException {

		ArrayList servers = MBeanServerFactory.findMBeanServer(null);
		if (servers == null) {
			throw new ConfigurationException("no Mbean servers found");
		}
		MBeanServer server = (MBeanServer) servers.get(0);
		log.debug("got an MBean server");
		try {
		if (server.isRegistered(name)) {
				log.debug("unregistering ["+name.getCanonicalName()+"] as it already exists");
				server.unregisterMBean(name);
		}
		server.registerMBean(mbean, name);
		}
			catch(Exception e) {
					throw new ConfigurationException(e);
			}
		log.debug("MBean [" + name.getCanonicalName() + "] registered");
		return;		
	}


	/**
		 * Creates ModelMBeanInfo object of an {@link nl.nn.adapterframework.core.Adapter adapter}
		 * @param adapter
		 * @return the ModelMBeanInfo object
		 */
		public static ModelMBeanInfo createMBeanInfo(Adapter adapter) {
	
	
			ModelMBeanAttributeInfo[] mmbai = new ModelMBeanAttributeInfo[]{
				JmxUtils.buildAttributeInfo("RunState", 
					"RunState",
					"RunState",
					"-",
					"getRunStateAsString",
					"java.lang.String"),
			
				JmxUtils.buildAttributeInfo("NumMessagesProcessed", 
					"NumMessagesProcessed",
					"Number of messages processed",
					"0",
					"getNumOfMessagesProcessed",
					"long"),

				JmxUtils.buildAttributeInfo("StatsUpSince", 
					"UpSince",
					"Up since",
					"-",
					"getStatsUpSince",
					"java.lang.String"),

				JmxUtils.buildAttributeInfo("ConfigurationSucceeded", 
					"ConfigurationSucceeded",
					"Did the configuration succeed",
					"-",
					"configurationSucceeded",
					"boolean"),

	
				JmxUtils.buildAttributeInfo("NumMessagesInProcess", 
					"NumMessagesInProcess",
					"Number of messages currently in process",
					"0",
					"getNumOfMessagesInProcess",
					"int"),
	
				JmxUtils.buildAttributeInfo("NumMessagesInError", 
					"NumMessagesInError",
					"Number of messages that went wrong",
					"0",
					"getNumOfMessagesInError",
					"long"),
	
				JmxUtils.buildAttributeInfo("AdapterDescription", 
					"AdapterDescription",
					"Description of Adapter",
					"none",
					"getDescription",
					"java.lang.String"),
	
				JmxUtils.buildAttributeInfo("LastMessageDate", 
					"LastMessageDate",
					"Date/time the last message was received",
					"-",
					"getLastMessageDate",
					"java.lang.String")
			
			};


			ModelMBeanOperationInfo[] mmboi = new ModelMBeanOperationInfo[]{
	
				new ModelMBeanOperationInfo(
					"startRunning",
					"start the adapter",
					null,
					"void",
					ModelMBeanOperationInfo.ACTION),

				new ModelMBeanOperationInfo(
					"stopRunning",
					"stop the adapter",
					null,
					"void",
					ModelMBeanOperationInfo.ACTION),

				JmxUtils.buildGetterModelMBeanOperationInfo(
					"getNumOfMessagesProcessed",
					adapter.getClass().getName(),
					"get the NumOfMessagesProcessed",
					"long"),

				JmxUtils.buildGetterModelMBeanOperationInfo(
					"getNumOfMessagesInProcess",
					adapter.getClass().getName(),
					"get the NumOfMessagesInProcess",
					"int"),
				
				JmxUtils.buildGetterModelMBeanOperationInfo(
					"getNumOfMessagesInError",
					adapter.getClass().getName(),
					"get the NumOfMessagesInError",
					"long"),

				JmxUtils.buildGetterModelMBeanOperationInfo(
					"getDescription",
					adapter.getClass().getName(),
					"get the description",
					"java.lang.String"),

				JmxUtils.buildGetterModelMBeanOperationInfo(
					"getLastMessageDate",
					adapter.getClass().getName(),
					"get the date/time of the last message",
					"java.lang.String"),
				
				JmxUtils.buildGetterModelMBeanOperationInfo(
					"getStatsUpSince",
					adapter.getClass().getName(),
					"Up since",
					"java.lang.String"),
				
				JmxUtils.buildGetterModelMBeanOperationInfo(
					"getRunStateAsString",
					adapter.getClass().getName(),
					"RunState",
					"java.lang.String"),

				JmxUtils.buildGetterModelMBeanOperationInfo(
					"configurationSucceeded",
					adapter.getClass().getName(),
					"configurationSucceeded",
					"boolean")

				};

			return new ModelMBeanInfoSupport(
				adapter.getClass().getName(),
				"adapter [" + adapter.getName() + "]" +(adapter.getDescription()==null? "":adapter.getDescription()),
				mmbai,
				null,
				mmboi,
				null);
		}

}
