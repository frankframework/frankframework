package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class TensorflowPipeTest extends PipeTestBase<TensorflowPipe> {
	private static final String BASE_PATH = "src/test/resources/tensorflow/";
	private String modelPath, inputShape, inputTensor, outputTensor, inputType, outputType, inputPath, expectedOutput;

	@Override
	public TensorflowPipe createPipe() {
		pipe = new TensorflowPipe();
		pipe.registerForward(new PipeForward("success", null));
		pipe.setName(pipe.getClass().getSimpleName() + " under test");
		return pipe;
	}

	@Parameterized.Parameters(name = "{index} - {7}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][]{
				{"first/model", "first/input.txt", "float", "784", "input_layer_input", "float", "output_layer/Identity:0", "0.0"},
		});
	}

	public TensorflowPipeTest(String modelPath, String inputPath, String inputType, String inputShape, String inputTensor, String outputType, String outputTensor, String expectedOutput) {
		this.modelPath = modelPath;
		this.inputShape = inputShape;
		this.inputTensor = inputTensor;
		this.inputType = inputType;
		this.outputTensor = outputTensor;
		this.outputType = outputType;
		this.inputPath = inputPath;
		this.expectedOutput = expectedOutput;
	}

	@Before
	public void setup() {
		File folder = new File(BASE_PATH + modelPath);
		for (File f : folder.listFiles()) {
			System.out.println(f.getName());
		}
		createPipe();
		pipe.setModelPath(BASE_PATH + modelPath);
		pipe.setInputShape(inputShape);
		pipe.setInputTensor(inputTensor);
		pipe.setOutputTensor(outputTensor);
		pipe.setInputType(inputType);
		pipe.setOutputType(outputType);
	}

	@Test
	public void doTest() throws Throwable {
		try {
			configurePipe();
			String input = new String(Files.readAllBytes(Paths.get(BASE_PATH + inputPath)), StandardCharsets.UTF_8);
//			System.out.println(input);
			PipeRunResult result = doPipe(pipe, input, session);

			String out = (String) result.getResult();
			Assert.assertEquals(expectedOutput, out);
		} catch (Exception e) {
			if (checkExceptionClass(e, expectedOutput)) {
				Assert.assertTrue(true);
			}else {
				throw e;
			}
		}
	}

	/**
	 * Recursively check if the exception thrown is equal to the exception expected.
	 *
	 * @param t Throwable to be checked.
	 * @param c Class to be checked
	 * @return True if one of the causes of the exception is the given class, false otherwise.
	 * @throws Throwable Input t when class is not found.
	 */
	private boolean checkExceptionClass(Throwable t, String c) throws Throwable {
		try {
			return checkExceptionClass(t, Class.forName(c));
		} catch (ClassNotFoundException e) {
			throw t;
		}
	}

	/**
	 * Recursively check if the exception thrown is equal to the exception expected.
	 *
	 * @param t Throwable to be checked.
	 * @param c Class to be checked
	 * @return True if one of the causes of the exception is the given class, false otherwise.
	 */
	private boolean checkExceptionClass(Throwable t, Class c) {
		if (c.isInstance(t)) {
			return true;
		} else if (t.getCause() != null) {
			return checkExceptionClass(t.getCause(), c);
		}
		return false;
	}
}
