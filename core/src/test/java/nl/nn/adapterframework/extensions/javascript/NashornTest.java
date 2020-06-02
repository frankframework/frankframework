package nl.nn.adapterframework.extensions.javascript;

import nl.nn.adapterframework.extensions.graphviz.ResultHandler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NashornTest {
	Nashorn engine;

	@Before
	public void setup() {
		engine = new Nashorn();
		engine.startRuntime();
	}

	@Test
	public void testGetEngine() {
		Assert.assertNotNull(engine.getEngine());
	}

	@Test
	public void testResultHandler() throws Exception {
		ResultHandler resultHandler = new ResultHandler();
		engine.setResultHandler(resultHandler);

		engine.executeScript("result('somerandomtext');");
		Assert.assertEquals("somerandomtext", resultHandler.waitFor());

		engine.executeScript("error('somerandomtext22');");
		try {
			resultHandler.waitFor();
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals("somerandomtext22", e.getMessage());
		}
	}

	@Test
	public void testPromise() throws Exception {
		String script =
				"var promise = new Promise(function(resolve, reject) { \n" +
						"    resolve('Success') \n" +
						"}) \n" +
						"  \n" +
						"promise \n" +
						"    .then(function(successMessage) { \n" +
						"        result(successMessage); \n" +
						"    }) \n" +
						"    .catch(function(errorMessage) { \n" +
						"        error(errorMessage); \n" +
						"    }); ";

		ResultHandler resultHandler = new ResultHandler();
		engine.setResultHandler(resultHandler);
		engine.executeScript(script);
		Assert.assertEquals("Success", resultHandler.waitFor());
	}

	@Test
	public void testExecuteFunction() {
		String script = "function plusTwo(x) { return x + 2;}";
		engine.executeScript(script);
		Double out = (Double) engine.executeFunction("plusTwo", 5);
		System.err.println("out: " + out);
		Assert.assertEquals(7.0, out, 0.0);
	}

	@Test
	public void testExecuteFunctionNoParam() {
		String script = "function plusTwo(x) { return x + 2;}";
		engine.executeScript(script);
		Double out = (Double) engine.executeFunction("plusTwo");
		Assert.assertTrue(out.isNaN());
	}

	@Test
	public void testExecuteFunctionUnknownFunc() {
		Double out = (Double) engine.executeFunction("plusTwo");
		Assert.assertNull(out);
	}
}
