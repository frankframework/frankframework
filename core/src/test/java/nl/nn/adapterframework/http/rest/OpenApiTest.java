package nl.nn.adapterframework.http.rest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class OpenApiTest extends OpenApiTestBase {

	@Test
	public void simpleEndpointTest() throws Exception {
		new AdapterBuilder("myAdapterName", "description4simple-get").setListener("users", "get").setValidator("simple.xsd", null, "user").build(true);

		String result = callOpenApi();

		String expected = TestFileUtils.getTestFile("/OpenApi/simple.json");
		TestAssertions.assertEqualsIgnoreCRLF(expected, result);
	}

	@Test
	public void petStore() throws Exception {
		new AdapterBuilder("listPets", "List all pets").setListener("pets", "get").setValidator("petstore.xsd", null, "Pets").build(true);
		new AdapterBuilder("createPets", "Create a pet").setListener("pets", "post").setValidator("petstore.xsd", "Pet", "Pet").build(true);
		new AdapterBuilder("showPetById", "Info for a specific pet").setListener("pets/{petId}", "get").setValidator("petstore.xsd", null, "Pet").build(true);
		//getPets.start(getPets, postPet, getPet); //Async start

		//Make sure all adapters have been registered on the dispatcher
		ApiServiceDispatcher dispatcher = ApiServiceDispatcher.getInstance();
		assertEquals("not all adapters are registered on the dispatcher", 2, dispatcher.findConfigForUri("pets").getMethods().size());
		assertEquals("adapter [showPetById] not registered on the dispatcher", 1, dispatcher.findConfigForUri("pets/a").getMethods().size());

		String result = callOpenApi();

		String expected = TestFileUtils.getTestFile("/OpenApi/petstore.json");
		TestAssertions.assertEqualsIgnoreCRLF(expected, result);
	}
}
