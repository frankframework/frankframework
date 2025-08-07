/*
   Copyright 2022 WeAreFrank!

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
package org.frankframework.extensions.sap.jco3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import com.sap.conn.jco.ext.DataProviderException;
import com.sap.conn.jco.ext.Environment;
import com.sap.conn.jco.ext.ServerDataEventListener;
import com.sap.conn.jco.ext.ServerDataProvider;

import lombok.Getter;

import org.frankframework.extensions.sap.SapException;
import org.frankframework.util.LogUtil;

public class SapServerDataProvider implements ServerDataProvider {
	protected static Logger log = LogUtil.getLogger(SapFunctionFacade.class);

	private @Getter ServerDataEventListener serverDataEventListener;

	private static SapServerDataProvider self=null;

	private final Map<String, SapListenerImpl> listeners = new LinkedHashMap<>();

	private SapServerDataProvider() {
	}

	public static SapServerDataProvider getInstance() {
		if (self==null) {
			self=new SapServerDataProvider();
			log.debug("register ServerDataProvider");
			Environment.registerServerDataProvider(self);
		}
		return self;
	}

	public void registerListener(SapListenerImpl listener) {
		listeners.put(listener.getName(), listener);
	}


	@Override
	public Properties getServerProperties(String servername) throws DataProviderException {
		log.info("providing server properties for [{}]", servername);
		Properties serverProperties = new Properties();
		SapListenerImpl listener = listeners.get(servername);
		if (listener==null) {
			throw new DataProviderException(DataProviderException.Reason.INVALID_CONFIGURATION, "listener ["+servername+"] not registered", null);
		}
		SapSystemImpl sapSystem;
		try {
			sapSystem = listener.getSapSystem();
		} catch (SapException e) {
			throw new DataProviderException(DataProviderException.Reason.INVALID_CONFIGURATION, "cannot find SapSystem for listener ["+servername+"] ", e);
		}
		serverProperties.setProperty(ServerDataProvider.JCO_GWHOST, sapSystem.getGwhost());
		serverProperties.setProperty(ServerDataProvider.JCO_GWSERV, sapSystem.getGwserv());
		serverProperties.setProperty(ServerDataProvider.JCO_PROGID, listener.getProgid());
		serverProperties.setProperty(ServerDataProvider.JCO_REP_DEST, sapSystem.getName());
		serverProperties.setProperty(ServerDataProvider.JCO_CONNECTION_COUNT, listener.getConnectionCount());
		serverProperties.setProperty(ServerDataProvider.JCO_APPLICATION, listener.getName());

		if(sapSystem.isSncEnabled()) {
			serverProperties.setProperty(ServerDataProvider.JCO_SNC_MODE, "1");
			serverProperties.setProperty(ServerDataProvider.JCO_SNC_LIBRARY, sapSystem.getSncLibrary());
			serverProperties.setProperty(ServerDataProvider.JCO_SNC_MYNAME, sapSystem.getMyName());
			serverProperties.setProperty(ServerDataProvider.JCO_SNC_QOP, sapSystem.getSncQop()+"");
		}

		return serverProperties;
	}

	@Override
	public void setServerDataEventListener(ServerDataEventListener serverDataEventListener) {
		log.debug("setting new serverDataEventListener [{}]", serverDataEventListener);
		this.serverDataEventListener = serverDataEventListener;
	}

	@Override
	public boolean supportsEvents() {
		return true;
	}

}
