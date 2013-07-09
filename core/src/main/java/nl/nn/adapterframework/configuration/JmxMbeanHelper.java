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
import nl.nn.adapterframework.util.LogUtil;

import javax.management.*;
import javax.management.modelmbean.*;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Jmx helper class, to make JMX MBeans from {@link nl.nn.adapterframework.core.Adapter Adapters}.
 *
 * @author Johan Verrips
 * @since 4.1.1
 */
public class JmxMbeanHelper {
    private static final Logger LOG = LogUtil.getLogger(JmxMbeanHelper.class);


    /**
     * Hooks an {@link nl.nn.adapterframework.core.Adapter Adapter} to the MBean server
     *
     * @param adapter the adapter
     * @throws ConfigurationException when something goes wrong
     */
    public static void hookupAdapter(IAdapter adapter) throws ConfigurationException {

        try {
            ObjectName objectName = getObjectName(adapter);

            RequiredModelMBean modelMbean =
                    new RequiredModelMBean(createMBeanInfo(adapter));
            modelMbean.setManagedResource(adapter, "ObjectReference");
            LOG.debug("modelMBean generated for object " + objectName);
            registerMBean(objectName, modelMbean);
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }

    public static void unhookAdapter(IAdapter adapter) {


        MBeanServer server = getMBeanServer();
        if (server != null) {
            try {
                ObjectName objectName = getObjectName(adapter);
                server.unregisterMBean(objectName);
            } catch (InstanceNotFoundException e1) {
                LOG.error(e1.getMessage(), e1);
            } catch (MBeanRegistrationException e1) {
                LOG.error(e1.getMessage(), e1);
            } catch (MalformedObjectNameException e) {
                LOG.error(e.getMessage(), e);
            }

        }

    }

    protected static ObjectName getObjectName(IAdapter adapter) throws MalformedObjectNameException {
        return new ObjectName("IBIS-" + AppConstants.getInstance().getResolvedProperty("instance.name") + "-" + "Adapters:name=" + adapter.getName());
    }


    /**
     * Registers an mBean at an MBeanServer. If there is already an mbean registered
     * under the specified name, it is first de-registered.
     *
     * @param name  the objectName
     * @param mbean the modelMbean to register
     * @throws ConfigurationException
     */
    private static void registerMBean(ObjectName name, RequiredModelMBean mbean) throws ConfigurationException {
        MBeanServer server = getMBeanServer();
        if (server != null) {
            LOG.debug("got an MBean server");
            try {
                if (server.isRegistered(name)) {
                    LOG.debug("unregistering [" + name.getCanonicalName() + "] as it already exists");
                    server.unregisterMBean(name);
                }
                server.registerMBean(mbean, name);
            } catch (Exception e) {
                throw new ConfigurationException(e);
            }
            LOG.debug("MBean [" + name.getCanonicalName() + "] registered");
        } else {
            LOG.warn("No MBean server found");
        }
    }

    private static MBeanServer getMBeanServer() {
        List servers = MBeanServerFactory.findMBeanServer(null);
        return servers == null || servers.size() == 0 ? null : (MBeanServer) servers.get(0);
    }


    /**
     * Creates ModelMBeanInfo object of an {@link nl.nn.adapterframework.core.Adapter adapter}
     *
     * @param adapter
     * @return the ModelMBeanInfo object
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
