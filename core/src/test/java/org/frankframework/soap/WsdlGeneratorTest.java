package org.frankframework.soap;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.net.URL;

import org.junit.jupiter.api.Test;

import org.frankframework.core.Adapter;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.core.PipeLineExit;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.pipes.WsdlXmlValidator;
import org.frankframework.pipes.XmlValidator;
import org.frankframework.testutil.TestAssertions;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TestFileUtils;

public class WsdlGeneratorTest {

	private final TestConfiguration configuration = new TestConfiguration();

	private PipeLine createPipeline() throws Exception {
		EchoPipe pipe = new EchoPipe();
		pipe.addForward(new PipeForward("success",null));
		pipe.setName(pipe.getClass().getSimpleName().concat("4WsdlGeneratorTest"));
		PipeLine pipeline = configuration.createBean(PipeLine.class);
		pipeline.addPipe(pipe);

		PipeLineExit exit = new PipeLineExit();
		exit.setName("exit");
		exit.setState(ExitState.SUCCESS);
		pipeline.addPipeLineExit(exit);

		Adapter adapter = configuration.createBean(Adapter.class);
		adapter.setName(pipe.getClass().getSimpleName().concat("4WsdlGeneratorTest"));
		adapter.setPipeLine(pipeline);
		pipeline.setAdapter(adapter);

		return pipeline;
	}

	//Catch any file-not-found exceptions beforehand
	private String validateResource(String schema) {
		URL url = this.getClass().getResource(schema);
		assertNotNull(url, "File ["+schema+"] not found");
		return schema;
	}

	@Test
	public void testInputValidatorWithSchemaAttribute() throws Exception {
		PipeLine pipeline = createPipeline();

		XmlValidator inputValidator = new XmlValidator();
		inputValidator.setSchema(validateResource("/OpenApi/simple.xsd"));
		pipeline.setInputValidator(inputValidator);

		assertThrows(IllegalStateException.class, () -> new WsdlGenerator(pipeline));
	}

	@Test
	public void testWsdlXmlValidatorWithWsdl() throws Exception {
		PipeLine pipeline = createPipeline();

		WsdlXmlValidator inputValidator = configuration.createBean(WsdlXmlValidator.class);
		inputValidator.setWsdl(validateResource("/WsdlGenerator/HelloWorld.wsdl"));
		inputValidator.setSoapBody("HelloWorld_Request");
		inputValidator.setOutputSoapBody("HelloWorld_Response");
		inputValidator.setSoapBodyNamespace("http://dummy.nl/HelloWorld");
		inputValidator.setThrowException(true);
		pipeline.setInputValidator(inputValidator);
		pipeline.getAdapter().configure();

		WsdlGenerator generator = new WsdlGenerator(pipeline);
		assertNotNull(generator);

		generator.init();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		generator.wsdl(out, "dummyServlet");
		String result = new String(out.toByteArray());
		result = result.replaceAll("[0-9]{4}-.*:[0-9]{2}", "DATETIME");
		TestAssertions.assertEqualsIgnoreCRLF(TestFileUtils.getTestFile("/WsdlGenerator/GeneratedHelloWorld.wsdl"), result);
	}
}
