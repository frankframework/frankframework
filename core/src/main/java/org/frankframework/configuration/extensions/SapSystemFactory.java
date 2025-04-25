/*
   Copyright 2013 Nationale-Nederlanden, 2022 WeAreFrank!

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
package org.frankframework.configuration.extensions;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.Logger;

import org.frankframework.util.LogUtil;

/**
 * Singleton that has the different sapSystems. <br/>
 * Typical use: SapSystemFactory.getInstance().&lt;method to execute&gt;
 */
public class SapSystemFactory {
	private final Logger log;

	private static SapSystemFactory self = null;
	private final Hashtable<String, Object> sapSystems = new Hashtable<>();

	private SapSystemFactory() {
		super();
		log = LogUtil.getLogger(this);
	}

	public static synchronized SapSystemFactory getInstance() {
		if (self == null) {
			self = new SapSystemFactory();
		}
		return self;
	}

	public String getSapSystemInfo(String sapSystemName) {
		Object sapSystem = sapSystems.get(sapSystemName);
		if (sapSystem == null) {
			log.error("no SapSystem found under name [{}], factory contents [{}]", sapSystem, this);
			return null;
		}
		ISapSystem sapSystem3 = (ISapSystem) sapSystem;
		return sapSystem3.getDestinationAsString();
	}

	public Iterator<String> getRegisteredSapSystemNames() {
		SortedSet<String> sortedKeys = new TreeSet<>(sapSystems.keySet());
		return sortedKeys.iterator();
	}

	public List<String> getRegisteredSapSystemsNamesAsList() {
		Iterator<String> it = getRegisteredSapSystemNames();
		List<String> result = new ArrayList<>();
		while (it.hasNext()) {
			result.add(it.next());
		}
		return result;
	}

	public void addSapSystem(Object sapSystem, String name) {
		sapSystems.put(name, sapSystem);
		log.debug("SapSystemFactory registered sapSystem [{}]", sapSystem);
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
