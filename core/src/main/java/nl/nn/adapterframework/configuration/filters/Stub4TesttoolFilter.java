/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.configuration.filters;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.ContentHandler;

import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.TransformerFilter;

public class Stub4TesttoolFilter extends TransformerFilter {

	private static final String STUB4TESTTOOL_XSLT = "/xml/xsl/stub4testtool.xsl";
	private static final String STUB4TESTTOOL_XSLT_VALIDATORS_PARAM = "disableValidators";
	
	private static final String STUB4TESTTOOL_CONFIGURATION_KEY = "stub4testtool.configuration";
	private static final String STUB4TESTTOOL_VALIDATORS_DISABLED_KEY = "validators.disabled";

	private Stub4TesttoolFilter(TransformerHandler transformerHandler, ContentHandler handler) {
		super(null, transformerHandler, null, null, false, handler);
	}
	
	public static ContentHandler getStub4TesttoolContentHandler(ContentHandler resolver, Properties properties) throws IOException, TransformerConfigurationException {
		if (Boolean.parseBoolean(properties.getProperty(STUB4TESTTOOL_CONFIGURATION_KEY,"false"))) {
			Resource xslt = Resource.getResource(STUB4TESTTOOL_XSLT);
			TransformerPool tp = TransformerPool.getInstance(xslt);
			
			Stub4TesttoolFilter filter = new Stub4TesttoolFilter(tp.getTransformerHandler(), resolver);
			
			Map<String,Object> parameters = new HashMap<String,Object>();
			parameters.put(STUB4TESTTOOL_XSLT_VALIDATORS_PARAM, Boolean.parseBoolean(properties.getProperty(STUB4TESTTOOL_VALIDATORS_DISABLED_KEY,"false")));
			
			XmlUtils.setTransformerParameters(filter.getTransformer(), parameters);
			
			return filter;
		} else {
			return resolver;
		}
	}
}
