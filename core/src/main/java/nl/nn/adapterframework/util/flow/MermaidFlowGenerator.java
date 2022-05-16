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
package nl.nn.adapterframework.util.flow;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.TransformerPool;

/**
 * Flow generator to create MERMAID files
 */
public class MermaidFlowGenerator implements IFlowGenerator {
	protected static Logger log = LogUtil.getLogger(MermaidFlowGenerator.class);

	private static final String ADAPTER2MERMAID_XSLT = "/xml/xsl/adapter2mermaid.xsl";
	private static final String CONFIGURATION2MERMAID_XSLT = "/xml/xsl/configuration2mermaid.xsl";

	private TransformerPool transformerPoolAdapter, transformerPoolConfig;

	@Override
	public void afterPropertiesSet() throws Exception {
		Resource xsltSourceAdapter = Resource.getResource(ADAPTER2MERMAID_XSLT);
		transformerPoolAdapter = TransformerPool.getInstance(xsltSourceAdapter, 2);

		Resource xsltSourceConfig = Resource.getResource(CONFIGURATION2MERMAID_XSLT);
		transformerPoolConfig = TransformerPool.getInstance(xsltSourceConfig, 2);
	}

	@Override
	public void generateFlow(String xml, OutputStream outputStream) throws FlowGenerationException {
		try {
			String flow = generateMermaid(xml);

			outputStream.write(flow.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new FlowGenerationException(e);
		}
	}

	protected String generateMermaid(String xml) throws FlowGenerationException {
		try {
			if(xml.startsWith("<adapter")) {
				return transformerPoolAdapter.transform(xml, null);
			} else {
				return transformerPoolConfig.transform(xml, null);
			}
		} catch (IOException | TransformerException | SAXException e) {
			throw new FlowGenerationException("error transforming [xml] to [mermaid]", e);
		}
	}

	@Override
	public String getFileExtension() {
		return "mmd";
	}

	@Override
	public MediaType getMediaType() {
		return MediaType.TEXT_PLAIN;
	}

	@Override
	public void destroy() {
		if(transformerPoolAdapter != null) {
			transformerPoolAdapter.close();
		}

		if(transformerPoolConfig != null) {
			transformerPoolConfig.close();
		}
	}
}
