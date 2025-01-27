/*
   Copyright 2018 Nationale-Nederlanden, 2021-2024 WeAreFrank!

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
package org.frankframework.ladybug.tibet2;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import nl.nn.testtool.echo2.BeanParent;
import nl.nn.testtool.echo2.Echo2Application;
import nl.nn.testtool.echo2.reports.ReportsComponent;
import nl.nn.testtool.filter.View;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.ladybug.LadybugDebugger;
import org.frankframework.stream.Message;

@Log4j2
public class TibetView extends View {
	private static final String AUTHORISATION_CHECK_ADAPTER_NAME = "AuthorisationCheck";
	private static final String AUTHORISATION_CHECK_ADAPTER_CONFIG = "main";
	protected @Setter LadybugDebugger ibisDebugger;

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	@Override
	public void initBean(BeanParent beanParent) {
		super.initBean(beanParent);
		Tibet2DatabaseStorage storage = (Tibet2DatabaseStorage) getDebugStorage();
		try {
			storage.configure();
		} catch (ConfigurationException e) {
			log.fatal("Could not configure TibetView storage", e);
		}
		storage.setSecurityContext(getEcho2Application());
	}

	@Override
	public String isOpenReportAllowed(Object storageId) {
		return isOpenReportAllowedViaAdapter(storageId);
	}

	public String isOpenReportAllowedViaAdapter(Object storageId) {
		Echo2Application app = getEcho2Application();
		Configuration config = ibisDebugger.getIbisManager().getConfiguration(AUTHORISATION_CHECK_ADAPTER_CONFIG);
		if(config == null) {
			return "Not allowed. Could not find config " + AUTHORISATION_CHECK_ADAPTER_CONFIG;
		}
		Adapter adapter = config.getRegisteredAdapter(AUTHORISATION_CHECK_ADAPTER_NAME);
		if(adapter == null) {
			return "Not allowed. Could not find adapter " + AUTHORISATION_CHECK_ADAPTER_NAME;
		}

		try (PipeLineSession pipeLineSession = new PipeLineSession()) {
			if (app.getUserPrincipal() != null) {
				pipeLineSession.put("principal", app.getUserPrincipal().getName());
			}
			pipeLineSession.put("StorageId", storageId);
			pipeLineSession.put("View", getName());
			PipeLineResult processResult = adapter.processMessageDirect(null, new Message("<dummy/>"), pipeLineSession);
			if (processResult.isSuccessful()) {
				return ReportsComponent.OPEN_REPORT_ALLOWED;
			}
			return "Not allowed. Result of adapter " + AUTHORISATION_CHECK_ADAPTER_NAME + ": " + processResult.getResult();
		}
	}
}
