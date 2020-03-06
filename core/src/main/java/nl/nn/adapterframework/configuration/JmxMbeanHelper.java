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
package nl.nn.adapterframework.configuration;

import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.JmxUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanOperationInfo;
import javax.management.modelmbean.RequiredModelMBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Jmx helper class, to make JMX MBeans from {@link nl.nn.adapterframework.core.Adapter Adapters}.
 *
 * @author Johan Verrips
 * @since 4.1.1
 */
public class JmxMbeanHelper {
    private static final Logger LOG = LogManager.getLogger(JmxMbeanHelper.class);

	private static MBeanServer mbServer = null;
	private static Map<IAdapter, ObjectName> registeredAdapters = new HashMap<IAdapter, ObjectName>();

    /**
     * Hooks an {@link nl.nn.adapterframework.core.Adapter Adapter} to the MBean server
     */
    public static void hookupAdapter(IAdapter adapter) throws ConfigurationException {
        MBeanServer server = getMBeanServer();
        if (server != null) {
        	synchronized(registeredAdapters) {
		    	try {
		            ObjectName objectName = getObjectName(adapter);
		
		            RequiredModelMBean modelMbean =
		                    new RequiredModelMBean(createMBeanInfo(adapter));
		            modelMbean.setManagedResource(adapter, "ObjectReference");
		            LOG.debug("modelMBean generated for object " + objectName);
		            ObjectInstance objectInstance = registerMBean(server, objectName, modelMbean);
		            if (objectInstance!=null) {
		            	registeredAdapters.put(adapter, objectInstance.getObjectName());
		            }
		        } catch (Exception e) {
		            throw new ConfigurationException(e);
		        }
        	}
        } else {
            LOG.warn("No MBean server found");
        }
    }

    public static void unhookAdapter(IAdapter adapter) {
        MBeanServer server = getMBeanServer();
        if (server != null) {
        	synchronized(registeredAdapters) {
	            ObjectName objectName = null;
	            try {
	                objectName = getObjectName(adapter);
	            } catch (MalformedObjectNameException e) {
	                LOG.error(e.getMessage(), e);
	            }
	            if (objectName!=null) {
	            	try {
	                    server.unregisterMBean(objectName);
	                    registeredAdapters.remove(adapter);
	                    LOG.debug("Unregistered mbean [" + objectName.getCanonicalName() + "]");
	            	} catch (InstanceNotFoundException e1) {
	            		LOG.error("Could not unregister mbean [" + objectName.getCanonicalName() + "]:" + e1.getMessage());
	            	} catch (MBeanRegistrationException e1) {
	                    LOG.error(e1.getMessage(), e1);
	                }
	            }
        	}
        } else {
            LOG.warn("No MBean server found");
        }
    }

    protected static ObjectName getObjectName(IAdapter adapter) throws MalformedObjectNameException {
		// If the mbean is registered it gets another ObjectName than the one
		// which is determined in this method. Therefore we use the HashMap
		// 'registeredAdapters'
		if (registeredAdapters.containsKey(adapter)) {
			return registeredAdapters.get(adapter);
		}
    	Configuration config = adapter == null?null:adapter.getConfiguration();
		String configString = null;
		if (config != null) {
			configString = config.getName();
			if (StringUtils.isNotEmpty(config.getVersion())) {
				configString = configString + "-" + config.getVersion();
			}
		}
		String name = "IBIS-" + AppConstants.getInstance().getResolvedProperty("instance.name") + "-" + (StringUtils.isNotEmpty(configString) ? configString + "-" : "") + "Adapters:name=";
        if (adapter == null) {
            name = name + "*";
        } else {
        	// name should be unique because same configuration can be reloaded (and we first do a load new and then the unload old)
            name = name + adapter.getName() + '#' + Integer.toHexString(adapter.hashCode());
        }
        return new ObjectName(name);
    }

    /**
     * Registers an mBean at an MBeanServer. If there is already an mbean registered
     * under the specified name, it is first de-registered.
     */
    private static ObjectInstance registerMBean(MBeanServer server, ObjectName name, RequiredModelMBean mbean) throws ConfigurationException {
        try {
            if (server.isRegistered(name)) {
                LOG.debug("unregistering [" + name.getCanonicalName() + "] as it already exists");
                server.unregisterMBean(name);
            }
            ObjectInstance objectInstance = server.registerMBean(mbean, name);
            LOG.debug("MBean [" + name.getCanonicalName() + "] registered as [" + objectInstance.getObjectName().getCanonicalName()+"]");
    		return objectInstance;
        } catch (InstanceAlreadyExistsException iae) {
            LOG.warn("Could not register mbean [" + name.getCanonicalName() + "]:" + iae.getMessage());
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
        return null;
    }

	private static synchronized MBeanServer getMBeanServer() {
		if (mbServer == null) {
			List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
			if (servers != null && !servers.isEmpty()) {
				mbServer = servers.get(0);
			}
		}
		return mbServer;
	}

    public static Set getMBeans() {
        MBeanServer server = getMBeanServer();
        if (server != null) {
            try {
                return server.queryMBeans(getObjectName(null), null);
            } catch (MalformedObjectNameException e) {
                LOG.warn("Could not create object name", e);
            }
        }
        return null;
    }

    /**
     * Creates ModelMBeanInfo object of an {@link nl.nn.adapterframework.core.Adapter adapter}
     */
    private static ModelMBeanInfo createMBeanInfo(IAdapter adapter) {


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
                "adapter [" + adapter.getName() + "]" + (adapter.getDescription() == null ? "" : adapter.getDescription()),
                mmbai,
                null,
                mmboi,
                null);
    }

}
