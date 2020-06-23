/*
   Copyright 2018 Nationale-Nederlanden

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
package nl.nn.ibistesttool.filter;

import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.ibistesttool.tibet2.Storage;
import nl.nn.testtool.echo2.BeanParent;
import nl.nn.testtool.echo2.Echo2Application;
import nl.nn.testtool.filter.View;

public class TibetView extends View {
	private static final String AUTHORISATION_CHECK_ADAPTER = "AuthorisationCheck";
	protected IbisManager ibisManager;

	/**
	 * Loaded via bean, see springIbisTestToolTibet2.xml
	 */
	public void setIbisManager(IbisManager ibisManager) {
		this.ibisManager = ibisManager;
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	@Override
	public void initBean(BeanParent beanParent) {
		super.initBean(beanParent);
		Storage storage = (Storage)getStorage();
		storage.setSecurityContext(getEcho2Application());
	}

	@Override
	public String isOpenReportAllowed(Object StorageId) {
		return isOpenReportAllowedViaAdapter(StorageId);
	}

	public String isOpenReportAllowedViaAdapter(Object StorageId) {
		Echo2Application app = getEcho2Application();
		IAdapter adapter = ibisManager.getRegisteredAdapter(AUTHORISATION_CHECK_ADAPTER);
		if(adapter == null) {
			return "Not allowed. Could not find adapter " + AUTHORISATION_CHECK_ADAPTER;
		} else {
			IPipeLineSession pipeLineSession = new PipeLineSessionBase();
			if(app.getUserPrincipal() != null)
				pipeLineSession.put("principal", app.getUserPrincipal().getName());
			pipeLineSession.put("StorageId", StorageId);
			pipeLineSession.put("View", getName());
			PipeLineResult processResult = adapter.processMessage(null, new Message("<dummy/>"), pipeLineSession);
			if ((processResult.getState().equalsIgnoreCase("success"))) {
				return "Allowed";
			} else {
				return "Not allowed. Result of adapter "
						+ AUTHORISATION_CHECK_ADAPTER + ": "
						+ processResult.getResult();
			}
		}
	}

}
