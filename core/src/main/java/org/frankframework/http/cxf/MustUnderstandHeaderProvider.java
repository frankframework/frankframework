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
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;

import org.frankframework.util.StringUtil;

public class MustUnderstandHeaderProvider extends AbstractSoapInterceptor implements SoapInterceptor {

	private final Set<QName> understandsHeaders;

	public MustUnderstandHeaderProvider(String understoodHeaderNames) {
		super(Phase.PRE_PROTOCOL);
		this.understandsHeaders = StringUtil.splitToStream(understoodHeaderNames)
				.map(this::splitAttributeName)
				.map(this::createQName)
				.collect(Collectors.toUnmodifiableSet());
	}

	private String[] splitAttributeName(String attributeName) {
		String[] split = attributeName.split("\\|", 2);
		if (split.length == 2) {
			return split;
		} else  {
			return new String[]{null, split[0]};
		}
	}

	private QName createQName(String[] parts) {
		String namespaceUri = parts[0];
		String localPart = parts[1];
		return new QName(namespaceUri, localPart);
	}

	@Override
	public void handleMessage(SoapMessage message) throws Fault {
		// Always success
		return;
	}

	@Override
	public Set<QName> getUnderstoodHeaders() {
		return understandsHeaders;
	}
}
