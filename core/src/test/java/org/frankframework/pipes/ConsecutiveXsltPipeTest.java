package org.frankframework.pipes;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeRunException;
import org.frankframework.processors.CorePipeLineProcessor;
import org.frankframework.processors.CorePipeProcessor;
import org.frankframework.stream.Message;
import org.frankframework.testutil.MatchUtils;

public class ConsecutiveXsltPipeTest extends PipeTestBase<XsltPipe> {

	@Override
	public XsltPipe createPipe() throws ConfigurationException {
		return new XsltPipe();
	}

	@Test
	public void testConsecutiveXsltPipes() throws ConfigurationException, PipeRunException, IOException {
		XsltPipe first = createBeanInAdapter(XsltPipe.class);
		first.setName("XsltPipe");
		first.setStyleSheetName("/Xslt/extract.xslt");
		first.addForward(new PipeForward("success", "nextPipe"));
		pipeline.addPipe(first);
		pipeline.setFirstPipe("XsltPipe");

		pipe.setName("nextPipe");
		pipe.setStyleSheetName("/Xslt/map.xslt");
		//second.setStoreResultInSessionKey("test");
		pipeline.addPipe(pipe);


		CorePipeLineProcessor cplp = new CorePipeLineProcessor();
		pipeline.setPipeLineProcessor(cplp);
		CorePipeProcessor cpp = new CorePipeProcessor();
		cplp.setPipeProcessor(cpp);

		configureAdapter();
		first.start();
		pipe.start();

		PipeLineResult pipeLineResult=pipeline.process("", new Message("<result><field>&lt;Document&gt;&lt;Header&gt;HeaderValue&lt;/Header&gt;&lt;Action&gt;ActionValue&lt;/Action&gt;&lt;/Document&gt;</field></result>"), session);

		MatchUtils.assertXmlEquals("<Document><Header>HeaderValue</Header><ActionFound>ActionValue</ActionFound></Document>", pipeLineResult.getResult().asString());
	}
}
