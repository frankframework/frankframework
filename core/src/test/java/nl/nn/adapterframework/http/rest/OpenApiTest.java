package nl.nn.adapterframework.http.rest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class OpenApiTest extends OpenApiTestBase {

	@Test
	public void simpleEndpointTest() throws Exception {
		AdapterBuilder.create("myAdapterName", "description4simple-get").setListener("users", "get").setValidator("simple.xsd", "user").build(true);

		String result = callOpenApi();

		String expected = TestFileUtils.getTestFile("/OpenApi/simple.json");
		TestAssertions.assertEqualsIgnoreCRLF(expected, result);
	}

	@Test
	public void petStore() throws Exception {
		Adapter getPets = AdapterBuilder.create("listPets", "List all pets").setListener("pets", "get").setValidator("get-pets.xsd", "Pets").build();
		Adapter postPet = AdapterBuilder.create("createPets", "Create a pet").setListener("pets", "post").setValidator("post-pet.xsd", "Pet").build();
		Adapter getPet  = AdapterBuilder.create("showPetById", "Info for a specific pet").setListener("pets/{petId}", "get").setValidator("get-pet.xsd", "Pet").build();
		AdapterBuilder.start(getPets, postPet, getPet); //Async start

		//Make sure all adapters have been registered on the dispatcher
		ApiServiceDispatcher dispatcher = ApiServiceDispatcher.getInstance();
		assertEquals("not all adapters are registered on the dispatcher", 2, dispatcher.findConfigForUri("pets").getMethods().size());
		assertEquals("adapter [showPetById] not registered on the dispatcher", 1, dispatcher.findConfigForUri("pets/a").getMethods().size());

		String result = callOpenApi();

		String expected = TestFileUtils.getTestFile("/OpenApi/petstore.json");
		TestAssertions.assertEqualsIgnoreCRLF(expected, result);
	}
}
