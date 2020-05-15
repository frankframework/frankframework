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
package nl.nn.adapterframework.extensions.sap;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.logging.log4j.Logger;

import com.sap.conn.jco.JCoException;

/**
 * Singleton that has the different sapSystems. <br/>
 * Typical use: SapSystemFactory.getInstance().&lt;method to execute&gt;
 */
public class SapSystemFactory {
	private Logger log;

	private static SapSystemFactory self = null;
	private Hashtable<String, Object> sapSystems = new Hashtable<String, Object>();

	private SapSystemFactory() {
		super();
		log = LogUtil.getLogger(this);
	}

	public static synchronized SapSystemFactory getInstance() {
		if (self == null) {
			self = new SapSystemFactory();
		}
		return (self);
	}

	public String getSapSystemInfo(String sapSystemName) {
		Object sapSystem = sapSystems.get(sapSystemName);
		if (sapSystem == null) {
			log.error("no SapSystem found under name [" + sapSystem	+ "], factory contents [" + toString() + "]");
			return null;
		}
		if (sapSystem instanceof nl.nn.adapterframework.extensions.sap.jco3.SapSystem) {
			nl.nn.adapterframework.extensions.sap.jco3.SapSystem sapSystem3 = (nl.nn.adapterframework.extensions.sap.jco3.SapSystem) sapSystem;
			try {
				return sapSystem3.getDestination().toString();
			} catch (JCoException e) {
				log.warn("Exception determining sapsytem info", e);
				return null;
			}
		} else {
			nl.nn.adapterframework.extensions.sap.jco2.SapSystem sapSystem2 = (nl.nn.adapterframework.extensions.sap.jco2.SapSystem) sapSystem;
			return sapSystem2.toString();
		}
	}

	public Iterator<String> getRegisteredSapSystemNames() {
		SortedSet<String> sortedKeys = new TreeSet<String>(sapSystems.keySet());
		return sortedKeys.iterator();
	}

	public List<String> getRegisteredSapSystemsNamesAsList() {
		Iterator<String> it = getRegisteredSapSystemNames();
		List<String> result = new ArrayList<String>();
		while (it.hasNext()) {
			result.add((String) it.next());
		}
		return result;
	}

	public void registerSapSystem(Object sapSystem, String name) {
		sapSystems.put(name, sapSystem);
		log.debug("SapSystemFactory registered sapSystem ["
				+ sapSystem.toString() + "]");
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
