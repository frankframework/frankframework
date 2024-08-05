package org.frankframework.mongodb;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import lombok.extern.log4j.Log4j2;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.mongodb.MongoDbSender.MongoAction;
import org.frankframework.parameters.Parameter;
import org.frankframework.senders.SenderTestBase;
import org.frankframework.stream.Message;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Log4j2
@Testcontainers(disabledWithoutDocker = true)
@Tag("integration") // Requires Docker; exclude with '-DexcludedGroups=integration'
public class MongoDbSenderTest extends SenderTestBase<MongoDbSender> {

	private static final String MONGO_DOCKER_TAG = "mongo:7.0.9";

	@Container
	private static final MongoDBContainer mongoDBContainer = new MongoDBContainer(MONGO_DOCKER_TAG);

	private final String host = "localhost";
	private final String database = "testdb";
	private final String collection = "Students";
	private Message result;

	private JndiMongoClientFactory mongoClientFactory;

	@AfterEach
	@Override
	public void tearDown() {
		try {
			if (result != null) {
				result.close();
			}
		} catch (IOException e) {
			log.warn("Error when closing MongoDB connection", e);
		}
		super.tearDown();
	}

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		String url = "mongodb://" + host + ":" + mongoDBContainer.getMappedPort(27017); // Required for testcontainers, since the default port is randomized

		MongoClientSettings settings = MongoClientSettings.builder()
				.applyToClusterSettings(builder -> builder.serverSelectionTimeout(2, TimeUnit.SECONDS))
				.applyToSocketSettings(builder -> builder.connectTimeout(3, TimeUnit.SECONDS).readTimeout(3, TimeUnit.SECONDS))
				.applyConnectionString(new com.mongodb.ConnectionString(url)).build();

		MongoClient mongoClient = MongoClients.create(settings);
		mongoClientFactory = new JndiMongoClientFactory();
		mongoClientFactory.add(mongoClient, JndiMongoClientFactory.GLOBAL_DEFAULT_DATASOURCE_NAME);
		super.setUp();
	}

	@Override
	public MongoDbSender createSender() {
		MongoDbSender mongoDbSender = new MongoDbSender();
		mongoDbSender.setMongoClientFactory(mongoClientFactory);
		mongoDbSender.setDatabase(database);
		mongoDbSender.setCollection(collection);
		return mongoDbSender;
	}

	@Test
	void testInsertOne() throws Exception {
		result = insertStudentRecord();
		assertThat(result.asString(), StringContains.containsString("\"insertedId\":"));
	}

	@Test
	void testInsertOneNoObjectId() throws ConfigurationException, SenderException, TimeoutException, IOException {
		sender.setAction(MongoDbSender.MongoAction.INSERTONE);
		sender.setCollection("Students");
		sender.configure();
		sender.open();

		result = sendMessage("{ \"student_id\": \"KarelV\", \"class_id\": \"first\", \"grades\": [ 4, 5, 6] }");
		assertThat(result.asString(), StringContains.containsString("\"insertedId\":"));
	}

	@Test
	void testInsertManyAndDeleteMany() throws Exception {
		// Arrange: insert 4 students
		sender.setAction(MongoDbSender.MongoAction.INSERTMANY);
		sender.setCollection("Students");
		sender.configure();
		sender.open();

		JsonArrayBuilder students = Json.createArrayBuilder();
		createStudent(List.of("Harry", "Klaas", "Bruinsma"), "1a", 4, 5, 6)
				.forEach(students::add);
		students.add(createStudent(List.of("Ridouan"), "1c", 4, 4, 3).get(0));
		// Act & assert: insert 4 students
		result = sendMessage(students.build().toString());
		assertThat(result.asString(), StringContains.containsString("\"insertedIds\":"));
		assertThat(result.asString(), StringContains.containsString("\"3\"")); // 4 IDs should be inserted

		// Act & assert: delete 3 students, based on class_id
		sender.setAction(MongoAction.DELETEMANY);
		result = sendMessage("{ \"class_id\": \"1a\" }");
		assertThat(result.asString(), StringContains.containsString("\"acknowledged\":true,\"deleteCount\":3"));
	}

	@Test
	void testFindOne() throws Exception {
		// Arrange: 1 student
		insertStudentRecord();
		sender.setAction(MongoAction.FINDONE);
		sender.setCollection("Students");
		sender.configure();
		sender.open();

		// Act & assert 1: find the student
		result = sendMessage("{ \"student_id\": \"Evert\" }");
		assertThat(result.asString(), StringContains.containsString("\"student_id\":\"Evert\",\"class_id\":\"1c\""));

		// Act & assert 2: delete student
		sender.setAction(MongoAction.DELETEONE);
		result = sendMessage("{ \"student_id\": \"Evert\" }");
		assertThat(result.asString(), StringContains.containsString("\"acknowledged\":true,\"deleteCount\":1"));
	}

	@Test
	void testFindOneXml() throws Exception {
		insertStudentRecord();
		
		sender.setAction(MongoAction.FINDONE);
		sender.setCollection("Students");
		sender.setOutputFormat(DocumentFormat.XML);
		sender.configure();
		sender.open();

		result = sendMessage("{ \"student_id\": \"Evert\" }");
		System.out.println("FindOne: [" + result.asString() + "]");
		assertThat(result.asString(), StringContains.containsString("<student_id>Evert</student_id><class_id>1c</class_id><classes><item>4</item>" +
				"<item>4</item><item>3</item></classes><scores><item><grade>4</grade></item><item><grade>4</grade></item><item><grade>3</grade></item></scores>"));
	}

	@Test
	void testFindMany() throws Exception {
		insertStudentRecord();

		sender.setAction(MongoAction.FINDMANY);
		sender.setCollection("Students");
		sender.configure();
		sender.open();

		result = sendMessage("{ \"student_id\": \"Evert\" }");
		assertThat(result.asString(), StringContains.containsString("\"student_id\":\"Evert\",\"class_id\":\"1c\""));
	}

	@Test
	void testFindManyXml() throws Exception {
		insertStudentRecord();
		sender.setAction(MongoAction.FINDMANY);
		sender.setCollection("Students");
		sender.setOutputFormat(DocumentFormat.XML);
		sender.configure();
		sender.open();

		result = sendMessage("{ \"student_id\": \"Evert\" }");
		System.out.println("FindManyXml: [" + result.asString() + "]");
		assertThat(result.asString(), StringContains.containsString("<student_id>Evert</student_id><class_id>1c</class_id><classes><item>4</item>" +
				"<item>4</item><item>3</item></classes><scores><item><grade>4</grade></item><item><grade>4</grade></item><item><grade>3</grade></item></scores>" +
				"<cities><item><houses>40</houses><girls><item>20</item><item>24</item></girls></item><item><houses>40</houses><girls><item>20</item>" +
				"<item>24</item></girls></item><item><houses>30</houses><girls><item>15</item><item>18</item></girls></item></cities><seatno>10</seatno>"));
	}

	@Test
	void testFindManyUsingParameter() throws Exception {
		insertStudentRecord();
		sender.setAction(MongoAction.FINDMANY);
		sender.setCollection("Students");
		Parameter param = new Parameter();
		param.setName("searchTarget");
		param.setValue("Evert");
		sender.addParameter(param);
		sender.configure();
		sender.open();

		result = sendMessage("{ \"student_id\": \"?{searchTarget}\" }");
		assertThat(result.asString(), StringContains.containsString("\"student_id\":\"Evert\",\"class_id\":\"1c\""));
	}

	@Test
	void testFindManyCountOnly() throws Exception {
		insertStudentRecord();
		sender.setAction(MongoAction.FINDMANY);
		sender.setCollection("Students");
		sender.setCountOnly(true);
		sender.configure();
		sender.open();

		result = sendMessage("{ \"student_id\": \"Evert\" }");
		int count = Integer.parseInt(result.asString());
		assertTrue(count > 0);
	}

	@Test
	void testFindManyLimit() throws Exception {
		insertStudentRecord();

		sender.setAction(MongoAction.FINDMANY);
		sender.setCollection("Students");
		sender.setCountOnly(true);
		sender.setLimit(1);
		sender.configure();
		sender.open();

		result = sendMessage("{ \"student_id\": \"Evert\" }");
		int count = Integer.parseInt(result.asString());
		assertEquals(1, count);
	}

	@Test
	void testUpdateOne() throws Exception {
		String filter = "{ \"student_id\": \"Evert\" }";
		String update = "{\"$set\": {\"seatno\":" + 10 + "}}";

		sender.setAction(MongoAction.UPDATEONE);
		sender.setCollection("Students");
		sender.setFilter(filter);
		sender.configure();
		sender.open();

		result = sendMessage(update);
		assertThat(result.asString(), StringContains.containsString("\"modifiedCount\":"));
	}

	@Test
	void testUpdateMany() throws Exception {
		// Arrange: clean up first
		sender.setCollection("Students");
		sender.setAction(MongoAction.DELETEMANY);
		sender.configure();
		sender.open();
		result = sendMessage("{ \"student_id\": \"Evert\" }");

		// Arrange: insert student
		insertStudentRecord();
		String filter = "{ \"student_id\": \"Evert\" }";
		String update = "{\"$set\": {\"seatno\":" + 10 + "}}";

		// Act
		sender.setAction(MongoAction.UPDATEMANY);
		sender.setFilter(filter);
		result = sendMessage(update);

		// Assert
		String returnMessage = result.asString();
		log.debug("UpdateMany: {}", returnMessage);
		assertThat(returnMessage, StringContains.containsString("\"acknowledged\":true,\"matchedCount\":1,\"modifiedCount\":1"));
	}

	@Test
	void testUpdateManyXml() throws Exception {
		String filter = "{ \"student_id\": \"Evert\" }";
		String update = "{\"$set\": {\"seatno\":" + 10 + "}}";

		sender.setAction(MongoAction.UPDATEMANY);
		sender.setCollection("Students");
		sender.setFilter(filter);
		sender.setOutputFormat(DocumentFormat.XML);
		sender.configure();
		sender.open();

		result = sendMessage(update);
		assertThat(result.asString(), StringContains.containsString("<modifiedCount>"));
	}

	private Message insertStudentRecord() throws Exception {
		sender.setAction(MongoAction.INSERTONE);
		sender.setCollection("Students");
		sender.configure();
		sender.open();

		List<JsonObject> stud = createStudent(Collections.singletonList("Evert"), "1c", 4, 4, 3);
		return sendMessage(stud.get(0).toString());
	}

	public List<JsonObject> createStudent(List<String> studentId, String classId, Integer... grades) {
		List<JsonObject> students = new ArrayList<>();
		studentId.forEach(id -> {
			JsonObjectBuilder builder = Json.createObjectBuilder();
			builder.add("student_id", id).add("class_id", classId);
			builder.add("classes", getArrayOfIntegers(grades));
			builder.add("scores", getScores(grades)); // array of objects
			builder.add("cities", getCities(grades)); // array of objects with an array
			students.add(builder.build());
		});
		return students;
	}

	private static JsonArray getArrayOfIntegers(Integer... classes) {
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		for (int grade : classes) {
			arrayBuilder.add(grade);
		}
		return arrayBuilder.build();
	}

	private static JsonArray getScores(Integer[] grades) {
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		for (Integer grade : grades) {
			arrayBuilder.add(Json.createObjectBuilder().add("grade", grade));
		}
		return arrayBuilder.build();
	}

	private static JsonArray getCities(Integer[] grades) {
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		for (Integer grade : grades) {
			JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
			objectBuilder.add("houses", grade * 10);
			objectBuilder.add("girls", getArrayOfIntegers(grade * 5, grade * 6));
			arrayBuilder.add(objectBuilder);
		}
		return arrayBuilder.build();
	}

}
