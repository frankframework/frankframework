/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2021 WeAreFrank!

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
package org.frankframework.jms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.Logger;

import org.frankframework.doc.FrankDocGroup;
import org.frankframework.doc.FrankDocGroupValue;
import org.frankframework.util.LogUtil;

/**
 * Singleton that has the different jmsRealms.<br/>
 * Typical use: JmsRealmFactory.getInstance().&lt;method to execute&gt;
 * <br/>
 * @author Johan Verrips IOS
 * @see JmsRealm
 */
@FrankDocGroup(FrankDocGroupValue.OTHER)
public class JmsRealmFactory {
	private final Logger log = LogUtil.getLogger(this);

	private static JmsRealmFactory self = null;
	private Map<String, JmsRealm> jmsRealms = new LinkedHashMap<>();

	/**
	 * Private constructor to prevent breaking of the singleton pattern
	 */
	private JmsRealmFactory() {
		super();
	}

	/**
	 * Get a hold of the singleton JmsRealmFactory
	 */
	public static synchronized JmsRealmFactory getInstance() {
		if (self == null) {
			self = new JmsRealmFactory();
		}
		return self;

	}

	/**
	 * Test method to cleanup the static references
	 */
	public void clear() {
		jmsRealms = new LinkedHashMap<>();
	}

	/**
	 * Get a requested JmsRealm with the given name, null is returned if no realm
	 * under given name
	 */
	public JmsRealm getJmsRealm(String jmsRealmName) {
		JmsRealm jmsRealm = jmsRealms.get(jmsRealmName);
		if (jmsRealm == null) {
			log.error("no JmsRealm found under name [{}], factory contents [{}]", jmsRealmName, this);
		}
		return jmsRealm;
	}

	public List<String> getConnectionFactoryNames() {
		List<String> list = new ArrayList<>();
		for (JmsRealm jmsRealm: jmsRealms.values()) {
			String connectionFactory = jmsRealm.retrieveConnectionFactoryName();
			if(StringUtils.isNotEmpty(connectionFactory)) {
				list.add(connectionFactory);
			}
		}
		return list;
	}

	/**
	 * Get the realmNames as an Iterator, in the order that they were declared
	 */
	public Iterator<String> getRegisteredRealmNames() {
		return jmsRealms.keySet().iterator();
	}

	/**
	 * Get the names as a list
	 */
	public List<String> getRegisteredRealmNamesAsList() {
		Iterator<String> it = getRegisteredRealmNames();
		List<String> result = new ArrayList<>();
		while (it.hasNext()) {
			result.add(it.next());
		}
		return result;
	}

	/**
	 * Register a Realm
	 */
	public void addJmsRealm(JmsRealm jmsRealm) {
		String realmName = jmsRealm.getRealmName();
		if(jmsRealms.containsKey(realmName)) {
			log.warn("overwriting JmsRealm [{}]. Realm with name [{}] already exists", jmsRealm, realmName);
		}
		jmsRealms.put(realmName, jmsRealm);
		log.debug("JmsRealmFactory registered realm [{}]", () -> jmsRealm.toString());
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
