/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.http.cxf;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapInterceptor;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;

public class MustUnderstandHeaderExtractor extends AbstractSoapInterceptor implements SoapInterceptor {

	private final Set<QName> understandsHeaders = ConcurrentHashMap.newKeySet();

	public MustUnderstandHeaderExtractor() {
		super(Phase.PRE_PROTOCOL);
	}

	@Override
	public void handleMessage(SoapMessage message) throws Fault {
		// Collect all headers from all messages and pretend to "understand" them all; adapter implementations must be written and configured appropriately. User responsibility, not framework.
		message.getHeaders()
				.stream()
				.filter(header -> header instanceof SoapHeader soapHeader && soapHeader.isMustUnderstand())
				.map(Header::getName)
				.forEach(understandsHeaders::add);
	}

	@Override
	public Set<QName> getUnderstoodHeaders() {
		return understandsHeaders;
	}
}
