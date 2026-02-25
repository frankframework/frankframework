package org.frankframework.util.flow;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import org.frankframework.core.Resource;
import org.frankframework.util.TransformerPool;

public class FrankConfigLayoutGlowGenerator extends MermaidFlowGenerator implements IFlowGenerator {

	private static final String ADAPTER2CONFIGLAYOUT_XSLT = "/xml/xsl/adapter2configlayout.xsl";
	private static final String CONFIGURATION2CONFIGLAYOUT_XSLT = "/xml/xsl/configuration2configlayout.xsl";

	public FrankConfigLayoutGlowGenerator() {
		super();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();

		Resource xsltSourceAdapter = Resource.getResource(ADAPTER2CONFIGLAYOUT_XSLT);
		assert xsltSourceAdapter != null;
		transformerPoolAdapter = TransformerPool.getInstance(xsltSourceAdapter, 2);

		Resource xsltSourceConfig = Resource.getResource(CONFIGURATION2CONFIGLAYOUT_XSLT);
		assert xsltSourceConfig != null;
		transformerPoolConfig = TransformerPool.getInstance(xsltSourceConfig, 2);
	}

	@Override
	public void generateFlow(String xml, OutputStream outputStream) throws FlowGenerationException {
		try {
			String flow = generateConfigLayout(xml);

			outputStream.write(flow.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new FlowGenerationException(e);
		}
	}

	private String generateConfigLayout(String xml) throws FlowGenerationException {
		try {
			Map<String, Object> xsltParams = new HashMap<>(1);// frankElements
			xsltParams.put("frankElements", frankElements);
			if (xml.startsWith("<adapter")) {
				return transformerPoolAdapter.transformToString(xml, xsltParams);
			} else {
				return transformerPoolConfig.transformToString(xml, xsltParams);
			}
		} catch (IOException | TransformerException | SAXException e) {
			throw new FlowGenerationException("error transforming [xml] to [frank config layout]", e);
		}
	}

}
