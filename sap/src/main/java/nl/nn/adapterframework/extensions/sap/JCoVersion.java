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

import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * Determine the JCo version to use (2 or 3). Attempt to find a JCo 3 class on
 * the classpath. When found getVersion() will return 3. When not found, try to
 * find a JCo 2 class and make getVersion() return 2, otherwise make it return
 * -1 and make getErrorMessage() return a proper error message.
 * 
 * @author  Jaco de Groot
 * @since   5.0
 * @version $Id$
 */
public class JCoVersion {
	private final static Logger LOG = LogUtil.getLogger(JCoVersion.class);
	private static JCoVersion self=null;
	private final static String JCO3_CLASS = "com.sap.conn.jco.JCo";
	private final static String JCO2_CLASS = "com.sap.mw.jco.JCO";
	private static int jcoVersion = -1;
	private static String errorMessage = null;

	private JCoVersion() {
		super();
	}

	public static synchronized JCoVersion getInstance() {
		if (self==null) {
			try {
				ClassUtils.loadClass(JCO3_CLASS);
				jcoVersion = 3;
			} catch (ClassNotFoundException e) {
				LOG.debug("Could not find JCo 3 class (" + JCO3_CLASS + "), switching back to JCo 2");
				try {
					ClassUtils.loadClass(JCO2_CLASS);
					jcoVersion = 2;
				} catch (ClassNotFoundException e2) {
					errorMessage = "Could not find JCo 3 class (" + JCO3_CLASS + ") or JCo 2 class (" + JCO2_CLASS + ")";
					LOG.error(errorMessage);
				}
			}
			self=new JCoVersion();
		}
		return self;
	}

	public int getJCoVersion() {
		return jcoVersion;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

}
