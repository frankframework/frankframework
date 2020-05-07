package nl.nn.adapterframework.http.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;

import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.testutil.threading.IsolatedThread;
import nl.nn.adapterframework.testutil.threading.RunInThreadRule;

public class OpenApiTest extends OpenApiTestBase {

	@Rule
	public RunInThreadRule runInThread = new RunInThreadRule();

	@Test
	@IsolatedThread
	public void simpleEndpointTest() throws Exception {
		ApiServiceDispatcher dispatcher = ApiServiceDispatcher.getInstance();
		assertEquals("there are still registered patterns! Threading issue?", 0, dispatcher.getPatternClients().size());

		new AdapterBuilder("myAdapterName", "description4simple-get").setListener("users", "get").setValidator("simple.xsd", null, "user").build(true);

		assertEquals("more then 1 registered pattern found!", 1, dispatcher.getPatternClients().size());
		String result = callOpenApi();

		String expected = TestFileUtils.getTestFile("/OpenApi/simple.json");
		TestAssertions.assertEqualsIgnoreCRLF(expected, result);
	}

	@Test
	@IsolatedThread
	public void petStore() throws Exception {
		//Make sure all adapters have been registered on the dispatcher
		ApiServiceDispatcher dispatcher = ApiServiceDispatcher.getInstance();
		assertEquals("there are still registered patterns! Threading issue?", 0, dispatcher.getPatternClients().size());

		new AdapterBuilder("listPets", "List all pets").setListener("pets", "get").setValidator("petstore.xsd", null, "Pets").build(true);
		new AdapterBuilder("createPets", "Create a pet").setListener("pets", "post").setValidator("petstore.xsd", "Pet", "Pet").build(true);
		new AdapterBuilder("showPetById", "Info for a specific pet").setListener("pets/{petId}", "get").setValidator("petstore.xsd", null, "Pet").build(true);
		//getPets.start(getPets, postPet, getPet); //Async start

		assertNotNull("unable to find DispatchConfig for uri [pets]", dispatcher.findConfigForUri("pets"));
		assertEquals("not all listener uri [pets] are registered on the dispatcher", 2, dispatcher.findConfigForUri("pets").getMethods().size());
		assertNotNull("unable to find DispatchConfig for uri [pets/a]", dispatcher.findConfigForUri("pets/a"));
		assertEquals("listener uri [pets/a] not registered on dispatcher", 1, dispatcher.findConfigForUri("pets/a").getMethods().size());

		String result = callOpenApi();

		String expected = TestFileUtils.getTestFile("/OpenApi/petstore.json");
		TestAssertions.assertEqualsIgnoreCRLF(expected, result);
	}
}
