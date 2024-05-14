package org.frankframework.mongodb;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
import org.frankframework.mongodb.MongoDbSender.MongoAction;
import org.frankframework.parameters.Parameter;
import org.frankframework.senders.SenderTestBase;
import org.frankframework.stream.Message;
import org.frankframework.stream.document.DocumentFormat;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Log4j2
@Testcontainers(disabledWithoutDocker = true)
@Tag("integration") // Requires Docker; exclude with '-DexcludedGroups=integration'
public class MongoDbSenderTest extends SenderTestBase<MongoDbSender> {

	private static final String MONGO_DOCKER_TAG = "mongo:7.0.9";

	@Container
	private final static MongoDBContainer mongoDBContainer = new MongoDBContainer(MONGO_DOCKER_TAG);

	private final String host = "localhost";
	private final String database = "testdb";
	private final String collection = "Students";
	private Message result;

	private JndiMongoClientFactory mongoClientFactory;

	@BeforeAll
	public static void beforeAll() {
		mongoDBContainer.start();
		int mappedPort = mongoDBContainer.getMappedPort(27017);
		System.setProperty("mongodb.container.port", String.valueOf(mappedPort));
		log.debug("MongoDB container started on port: {}", mappedPort);
	}

	@AfterEach
	@Override
	public void tearDown() throws Exception {
		if (result != null) {
			result.close();
		}
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

	private Message insertStudentRecord() throws Exception {
		sender.setAction(MongoAction.INSERTONE);
		sender.setCollection("Students");
		sender.configure();
		sender.open();

		JsonObject stud = createStudent("Evert", "1c", 4, 4, 3);
		return sendMessage(stud.toString());
	}

	@Override
	public MongoDbSender createSender() {
		MongoDbSender result = new MongoDbSender();
		result.setMongoClientFactory(mongoClientFactory);
		result.setDatabase(database);
		result.setCollection(collection);
		return result;
	}

	@Test
	void testOpen() throws SenderException, ConfigurationException {
		sender.setAction(MongoAction.FINDONE);
		sender.configure();
		sender.open();
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
	void testInsertMany() throws Exception {
		sender.setAction(MongoDbSender.MongoAction.INSERTMANY);
		sender.setCollection("Students");
		sender.configure();
		sender.open();

		JsonArrayBuilder students = Json.createArrayBuilder();
		students.add(createStudent("Harry", "1a", 4, 5, 6));
		students.add(createStudent("Klaas", "1b", 5, 7, 9));
		result = sendMessage(students.build().toString());
		assertThat(result.asString(), StringContains.containsString("\"insertedIds\":"));
	}

	@Test
	void testFindOne() throws Exception {
		insertStudentRecord();

		sender.setAction(MongoAction.FINDONE);
		sender.setCollection("Students");
		sender.configure();
		sender.open();

		result = sendMessage("{ \"student_id\": \"Evert\" }");
		System.out.println("FindOne: [" + result.asString() + "]");
		assertThat(result.asString(), StringContains.containsString("\"student_id\":\"Evert\",\"class_id\":\"1c\""));
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
		assertThat(result.asString(), StringContains.containsString("<student_id>Evert</student_id><class_id>1c</class_id><scores><item>4</item><item>4</item><item>3</item></scores>"));
	}

	@Test
	void testFindMany() throws Exception {
		sender.setAction(MongoAction.FINDMANY);
		sender.setCollection("Students");
		sender.configure();
		sender.open();

		result = sendMessage("{ \"student_id\": \"Evert\" }");
		assertThat(result.asString(), StringContains.containsString("\"student_id\":\"Evert\",\"class_id\":\"1c\""));
	}

	@Test
	void testFindManyXml() throws Exception {
		sender.setAction(MongoAction.FINDMANY);
		sender.setCollection("Students");
		sender.setOutputFormat(DocumentFormat.XML);
		sender.configure();
		sender.open();

		result = sendMessage("{ \"student_id\": \"Evert\" }");
		System.out.println("FindManyXml: [" + result.asString() + "]");
		assertThat(result.asString(), StringContains.containsString("<student_id>Evert</student_id><class_id>1c</class_id><scores><item>4</item><item>4</item><item>3</item></scores><seatno>10</seatno></item><item>"));
	}

	@Test
	void testFindManyUsingParameter() throws Exception {
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
		sender.setAction(MongoAction.FINDMANY);
		sender.setCollection("Students");
		sender.setCountOnly(true);
		sender.configure();
		sender.open();

		result = sendMessage("{ \"student_id\": \"Evert\" }");
		System.out.println("FindMany: [" + result.asString() + "]");
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
		System.out.println("FindMany: [" + result.asString() + "]");
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
		String filter = "{ \"student_id\": \"Evert\" }";
		String update = "{\"$set\": {\"seatno\":" + 10 + "}}";

		sender.setAction(MongoAction.UPDATEMANY);
		sender.setCollection("Students");
		sender.setFilter(filter);
		sender.configure();
		sender.open();

		result = sendMessage(update);
		assertThat(result.asString(), StringContains.containsString("\"modifiedCount\":"));
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

	public JsonObject createStudent(String studentId, String classId, Integer... grades) {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add("student_id", studentId).add("class_id", classId);
		builder.add("scores", getScores(grades));
		return builder.build();
	}

	public JsonArray getScores(Integer... grades) {
		JsonArrayBuilder scores = Json.createArrayBuilder();
		for (int grade : grades) {
			scores.add(grade);
		}
		return scores.build();
	}

}
