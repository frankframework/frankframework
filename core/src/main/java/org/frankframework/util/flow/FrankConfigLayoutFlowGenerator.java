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
package org.frankframework.util.flow;

public class FrankConfigLayoutFlowGenerator extends MermaidFlowGenerator {

	private static final String ADAPTER2CONFIGLAYOUT_XSLT = "/xml/xsl/adapter2configlayout.xsl";
	private static final String CONFIGURATION2CONFIGLAYOUT_XSLT = "/xml/xsl/configuration2configlayout.xsl";

	public FrankConfigLayoutFlowGenerator() {
		super(ADAPTER2CONFIGLAYOUT_XSLT, CONFIGURATION2CONFIGLAYOUT_XSLT);
	}

}
