package nl.nn.adapterframework.pipes;

import java.io.IOException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.processors.CorePipeLineProcessor;
import nl.nn.adapterframework.processors.CorePipeProcessor;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.MatchUtils;

import org.junit.jupiter.api.Test;

public class ConsecutiveXsltPipeTest extends PipeTestBase<XsltPipe> {

	@Override
	public XsltPipe createPipe() throws ConfigurationException {
		return new XsltPipe();
	}

	@Test
	public void testConsecutiveXsltPipes() throws ConfigurationException, PipeRunException, IOException, PipeStartException {
		XsltPipe first = createPipe();
		first.setName("XsltPipe");
		first.setStyleSheetName("/Xslt/extract.xslt");
		first.registerForward(new PipeForward("success", "nextPipe"));
		autowireByType(first);
		autowireByName(first);
		pipeline.addPipe(first);
		pipeline.setFirstPipe("XsltPipe");

		pipe.setName("nextPipe");
		pipe.setStyleSheetName("/Xslt/map.xslt");
		//second.setStoreResultInSessionKey("test");
		autowireByType(pipe);
		autowireByName(pipe);
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
