/*
   Copyright 2024 WeAreFrank!

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
package nl.nn.adapterframework.extensions.cmis;

import static org.apache.chemistry.opencmis.client.bindings.impl.CmisBindingsHelper.HTTP_INVOKER_OBJECT;

import org.apache.chemistry.opencmis.client.bindings.spi.BindingSession;
import org.apache.chemistry.opencmis.client.bindings.spi.webservices.CmisWebServicesSpi;

public class CmisCustomWebServicesSpi extends CmisWebServicesSpi {
	private final BindingSession bindingSession;

	/**
	 * Constructor.
	 *
	 * @param session
	 */
	public CmisCustomWebServicesSpi(BindingSession session) {
		super(session);
		this.bindingSession = session;
	}

	@Override
	public void close() {
		Object invoker = bindingSession.get(HTTP_INVOKER_OBJECT);
		if (invoker instanceof CmisHttpInvoker) {
			CmisHttpInvoker cmisHttpInvoker = (CmisHttpInvoker) invoker;
			cmisHttpInvoker.close();
		}
		super.close();
	}
}
