package nl.nn.adapterframework.soap;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.net.URL;

import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLine.ExitState;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.pipes.EchoPipe;
import nl.nn.adapterframework.pipes.WsdlXmlValidator;
import nl.nn.adapterframework.pipes.XmlValidator;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class WsdlGeneratorTest {

	private TestConfiguration configuration = new TestConfiguration();

	private PipeLine createPipeline() throws Exception {
		EchoPipe pipe = new EchoPipe();
		pipe.registerForward(new PipeForward("success",null));
		pipe.setName(pipe.getClass().getSimpleName().concat("4WsdlGeneratorTest"));
		PipeLine pipeline = new PipeLine();
		pipeline.addPipe(pipe);

		PipeLineExit exit = new PipeLineExit();
		exit.setPath("exit");
		exit.setState(ExitState.SUCCESS);
		pipeline.registerPipeLineExit(exit);

		Adapter adapter = new Adapter();
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
