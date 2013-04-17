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
package nl.nn.adapterframework.util;

import java.util.List;

import javax.management.Descriptor;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;
import javax.management.modelmbean.RequiredModelMBean;

import org.apache.log4j.Logger;
/**
 * Utility classes for JMX.
 * 
 * @author Johan Verrips
 * @version $Id$
 */
public class JmxUtils {
	public static final String version = "$RCSfile: JmxUtils.java,v $ $Revision: 1.7 $ $Date: 2011-11-30 13:51:49 $";
	protected static Logger log = LogUtil.getLogger(JmxUtils.class);

	static MBeanServer mbeanServer=null;
	
	public static MBeanServer getMBeanServer() throws Exception {
		if (mbeanServer==null) {
			List servers = MBeanServerFactory.findMBeanServer(null);
			if (servers == null) {
				throw new Exception("no MBean servers found");
			}
			mbeanServer = (MBeanServer) servers.get(0);
			log.debug("got an MBean server, domain ["+mbeanServer.getDefaultDomain()+"]");
		}
		return mbeanServer;
	}

	/**
	 * Registers an mBean at an MBeanServer. If there is already an mbean registered 
	 * under the specified name, it is first de-registered.
	 * @param name	the objectName
	 * @param mbean	the modelMbean to register
	 * @throws ConfigurationException
	 */
	public static void registerMBean(ObjectName name, RequiredModelMBean mbean) throws Exception {

		MBeanServer server = getMBeanServer(); 
//		if (server.isRegistered(name)) {
//				log.debug("unregistering ["+name.getCanonicalName()+"] as it already exists");
//				server.unregisterMBean(name);
//		}
		server.registerMBean(mbean, name);
	
		log.debug("MBean [" + name.getCanonicalName() + "] registered");
		return;		
	}


	

	/**
	 * Builds a default Descriptor object for getter operations
	 * @param name the name of the getter
	 * @param klass
	 * @return the descriptor
	 */
	private static Descriptor buildGetterDescriptor(
		String name,
		String klass) {
		Descriptor resultDesc = new DescriptorSupport();
		resultDesc.setField("name", name);
		resultDesc.setField("descriptorType", "operation");
		resultDesc.setField("class", klass);
		resultDesc.setField("role", "getter");
		return resultDesc;
	}
	
	/**
	 * Builds an operationInfor for getter purposes.
	 * @param name
	 * @param klass 
	 * @param description
	 * @param signature
	 */
	public static ModelMBeanOperationInfo buildGetterModelMBeanOperationInfo(
		String name,
		String klass,
		String description,
		String signature) {
			
		Descriptor theDescriptor = buildGetterDescriptor(name, klass);
		return new ModelMBeanOperationInfo(
			name,
			description,
			null,
			signature,
			ModelMBeanOperationInfo.INFO,
			theDescriptor);

	}
	/**
	 * Builds an AttributeInfo in a default way
	 * @param name
	 * @param displayName
	 * @param description
	 * @param deflt
	 * @param operName
	 * @param signature
	 * @return the default modelMBeanAttributeInfo object
	 */
	public static ModelMBeanAttributeInfo buildAttributeInfo(String name, String displayName, String description, String deflt, String operName, String signature){
		Descriptor attrDesc = new DescriptorSupport();
		attrDesc.setField("name", name);
		attrDesc.setField("descriptorType", "attribute");
		attrDesc.setField("default", deflt);
		attrDesc.setField("displayName", displayName);
		attrDesc.setField(
			"getMethod",
			operName);

		ModelMBeanAttributeInfo result=new ModelMBeanAttributeInfo(name,
		signature,
		description,
		true,
		false,
		false,
		attrDesc);
		return result;
	}

}
