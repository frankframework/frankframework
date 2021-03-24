/*
   Copyright 2021 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.mongodb;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;
import org.bson.BsonValue;
import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.stream.StreamingSenderBase;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StringResolver;

public class MongoDbSender extends StreamingSenderBase {

	public final String PARAM_DATABASE="database";
	public final String PARAM_COLLECTION="collection";
	public final String PARAM_FILTER="filter";
	public final String PARAM_LIMIT="limit";

	public final String NAMED_PARAM_START="?{";
	public final String NAMED_PARAM_END="}";


	private @Getter String datasourceName;
	private @Getter String url;
	private @Getter String database;
	private @Getter String collection;
	private MongoAction action;
	private @Getter String filter;
	private @Getter int limit=0;
	private @Getter boolean countOnly=false;
//	private OutputFormat outputFormat=OutputFormat.JSON;

	private @Setter @Getter IMongoClientFactory mongoClientFactory = null; // Spring should wire this!

	private MongoClient mongoClient;
	private ConcurrentHashMap<String,MongoDatabase> mongoDatabases = new ConcurrentHashMap<>();
	
	public enum MongoAction {
		INSERTONE,
		INSERTMANY,
		FINDONE,
		FINDMANY,
		UPDATEONE,
		UPDATEMANY,
		DELETEONE,
		DELETEMANY;
	}
	
//	public enum OutputFormat {
//		JSON,
//		XML
//	}
	
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getDatasourceName())) {
			setDatasourceName(AppConstants.getInstance(getConfigurationClassLoader()).getResolvedProperty(JndiMongoClientFactory.DEFAULT_DATASOURCE_NAME_PROPERTY));
		}
		if (mongoClientFactory==null && StringUtils.isEmpty(getUrl())) {
			throw new ConfigurationException("no mongoClientFactory available, and attribute url not specified");
		}
		checkStringAttributeOrParameter("database", getDatabase(), PARAM_DATABASE);
		checkStringAttributeOrParameter("collection", getCollection(), PARAM_COLLECTION);
		if (getActionEnum()==null) {
			throw new ConfigurationException("attribute action not specified");
		}
	}

	@Override
	public void open() throws SenderException {
		if (mongoClientFactory!=null) {
			try {
				mongoClient = mongoClientFactory.getMongoClient(getDatasourceName());
			} catch (NamingException e) {
				throw new SenderException("cannot open MongoDB datasource ["+getDatasourceName()+"]", e);
			}
		} else {
			mongoClient = MongoClients.create(getUrl());
		}
		super.open();
	}

	@Override
	public void close() throws SenderException {
		try {
			super.close();
		} finally {
			if (mongoClient!=null) {
				mongoClient.close();
				mongoClient=null;
				mongoDatabases.clear();
			}
		}
	}

	@Override
	public MessageOutputStream provideOutputStream(IPipeLineSession session, IForwardTarget next) throws StreamingException {
		return null;
	}


	@Override
	public PipeRunResult sendMessage(Message message, IPipeLineSession session, IForwardTarget next) throws SenderException, TimeOutException {
		message.closeOnCloseOf(session);
		JsonObject result;
		try {
			ParameterValueList pvl = ParameterValueList.get(getParameterList(), message, session);
			MongoDatabase mongoDatabase = getDatabase(pvl);
			MongoCollection<Document> mongoCollection = getCollection(mongoDatabase, pvl);
			switch (getActionEnum()) {
			case INSERTONE:
				result = renderResult(mongoCollection.insertOne(getDocument(message)));
				break;
			case INSERTMANY:
				result = renderResult(mongoCollection.insertMany(getDocuments(message)));
				break;
			case FINDONE:
				Document findOne = mongoCollection.find(getFilter(pvl, message)).first();
				return new PipeRunResult(null, new Message(findOne.toJson()));
			case FINDMANY:
				return renderResult(mongoCollection.find(getFilter(pvl, message)).limit(getLimit(pvl)), session, next);
			case UPDATEONE:
				result = renderResult(mongoCollection.updateOne(getFilter(pvl, null), getDocument(message)));
				break;
			case UPDATEMANY:
				result = renderResult(mongoCollection.updateMany(getFilter(pvl, null), getDocument(message)));
				break;
			case DELETEONE:
				result = renderResult(mongoCollection.deleteOne(getFilter(pvl, message)));
				break;
			case DELETEMANY:
				result = renderResult(mongoCollection.deleteMany(getFilter(pvl, message)));
				break;
			default:
				throw new SenderException("Unknown action ["+getActionEnum()+"]");
			}
			return new PipeRunResult(null, new Message(result.toString()));
		} catch (IOException | StreamingException | ParameterException e) {
			throw new SenderException("Cannot execute action ["+getActionEnum()+"]", e);
		}
	}

	protected JsonObject renderResult(InsertOneResult insertOneResult) {
		JsonObjectBuilder resultBuilder = Json.createObjectBuilder().add("acknowledged", insertOneResult.wasAcknowledged());
		if (insertOneResult.wasAcknowledged()) {
			resultBuilder.add("insertedId", insertOneResult.getInsertedId().asString().getValue());
		}
		return resultBuilder.build();
	}

	protected JsonObject renderResult(InsertManyResult insertManyResult) {
		JsonObjectBuilder resultBuilder = Json.createObjectBuilder().add("acknowledged", insertManyResult.wasAcknowledged());
		JsonObjectBuilder insertedId = Json.createObjectBuilder();
		if (insertManyResult.wasAcknowledged()) {
			Map<Integer, BsonValue> insertedIds = insertManyResult.getInsertedIds();
			insertedIds.forEach((k,v)->insertedId.add(Integer.toString(k), v.asString().getValue()));
		}
		resultBuilder.add("insertedId", insertedId.build());
		return resultBuilder.build();
	}

	protected PipeRunResult renderResult(FindIterable<Document> findResults, IPipeLineSession session, IForwardTarget next) throws StreamingException {
		if (isCountOnly()) {
			int count=0;
			for (Document doc : findResults) {
				count++;
			}
			return new PipeRunResult(null, new Message(Integer.toString(count)));
		}
		try (MessageOutputStream target = MessageOutputStream.getTargetStream(this, session, next)) {
			try (Writer writer = target.asWriter()) {
				writer.write("[");
				boolean firstElementSeen = false;
				for (Document doc : findResults) {
					if (firstElementSeen) {
						writer.write(",");
					}
					writer.write(doc.toJson());
					}
				writer.write("]");
			}
			return target.getPipeRunResult();
		} catch (Exception e) {
			throw new StreamingException("Could not render collection", e);
		}
	}
	
	protected JsonObject renderResult(UpdateResult updateResult) {
		JsonObjectBuilder resultBuilder = Json.createObjectBuilder().add("acknowledged", updateResult.wasAcknowledged());
		if (updateResult.wasAcknowledged()) {
			resultBuilder.add("matchedCount", updateResult.getMatchedCount());
			resultBuilder.add("modifiedCount", updateResult.getModifiedCount());
			addOptionalValue(resultBuilder, "upsertedId", updateResult.getUpsertedId());
		}
		return resultBuilder.build();
	}
	
	protected JsonObject renderResult(DeleteResult deleteResult) {
		JsonObjectBuilder resultBuilder = Json.createObjectBuilder().add("acknowledged", deleteResult.wasAcknowledged());
		if (deleteResult.wasAcknowledged()) {
			resultBuilder.add("deleteCount", deleteResult.getDeletedCount());
		}
		return resultBuilder.build();
	}
	
	protected JsonObjectBuilder addOptionalValue(JsonObjectBuilder builder, String name, BsonValue bsonValue) {
		return bsonValue==null ? builder : builder.add(name, bsonValue.asString().getValue());
	}
	

	protected Document getDocument(Message message) throws IOException {
		return getDocument(message.asString());
	}

	protected Document getDocument(String message) {
		return Document.parse(message);
	}
	
	protected List<Document> getDocuments(Message message) throws IOException {
		JsonArray array = Json.createReader(message.asReader()).readArray();
		List<Document> documents = new ArrayList<>();
		for (JsonObject object:array.getValuesAs(JsonObject.class)) {
			documents.add(getDocument(object.toString()));
		}
		return documents;
	}

	protected MongoDatabase getDatabase(ParameterValueList pvl) throws SenderException {
		String databaseName = getParameterOverriddenAttributeValue(pvl, PARAM_DATABASE, getDatabase());
		if (StringUtils.isEmpty(databaseName)) {
			throw new SenderException("no database found from attribute or parameter");
		}
		return mongoDatabases.computeIfAbsent(databaseName, (d)->mongoClient.getDatabase(databaseName));
	}

	protected MongoCollection<Document> getCollection(MongoDatabase mongoDatabase, ParameterValueList pvl) throws SenderException {
		String collectionName = getParameterOverriddenAttributeValue(pvl, PARAM_COLLECTION, getCollection());
		if (StringUtils.isEmpty(collectionName)) {
			throw new SenderException("no collection found from attribute or parameter");
		}
		return mongoDatabase.getCollection(collectionName);
	}
	
	protected Document getFilter(ParameterValueList pvl, Message message) throws IOException, ParameterException, IllegalArgumentException {
		String filterSpec = getParameterOverriddenAttributeValue(pvl, PARAM_FILTER, getFilter());
		if (StringUtils.isEmpty(filterSpec)) {
			filterSpec = Message.isEmpty(message) ? "" : message.asString();
		}
		if (filterSpec.contains(NAMED_PARAM_START) && filterSpec.contains(NAMED_PARAM_END)) {
			filterSpec = StringResolver.substVars(filterSpec, pvl.getValueMap(), null, null, NAMED_PARAM_START, NAMED_PARAM_END);
		}
		return getDocument(filterSpec);
	}
	
	protected int getLimit(ParameterValueList pvl) {
		return getParameterOverriddenAttributeValue(pvl, PARAM_LIMIT, getLimit());
	}


	@IbisDoc({"1", "The MongoDB datasource", "${"+JndiMongoClientFactory.DEFAULT_DATASOURCE_NAME_PROPERTY+"}"})
	public void setDatasourceName(String datasourceName) {
		this.datasourceName = datasourceName;
	}

	@IbisDoc({"2", "mongodb-url, only used when no datasourceName is specified", ""})
	public void setUrl(String url) {
		this.url = url;
	}

	@IbisDoc({"8", "Database to connect to. Can be overridden by parameter '"+PARAM_DATABASE+"'", ""})
	public void setDatabase(String database) {
		this.database = database;
	}

	@IbisDoc({"9", "Collection to act upon. Can be overridden by parameter '"+PARAM_COLLECTION+"'", ""})
	public void setCollection(String collection) {
		this.collection = collection;
	}

	@IbisDoc({"10", "Action", ""})
	public void setAction(String action) {
		this.action = Misc.parse(MongoAction.class, action);
	}
	public MongoAction getActionEnum() {
		return action;
	}
	
	@IbisDoc({"11", "Filter. Can contain references to parameters between '"+NAMED_PARAM_START+"' and '"+NAMED_PARAM_END+"'. Can be overridden by parameter '"+PARAM_FILTER+"'", ""})
	public void setFilter(String filter) {
		this.filter = filter;
	}

	@IbisDoc({"12", "Limit to number of results returned. A value of 0 means 'no limit'. Can be overridden by parameter '"+PARAM_LIMIT+"'", "0"})
	public void setLimit(int limit) {
		this.limit = limit;
	}

	@IbisDoc({"13", "Only for find operation: return only the count and not the full document(s)", "false"})
	public void setCountOnly(boolean countOnly) {
		this.countOnly = countOnly;
	}

//	@IbisDoc({"14", "OutputFormat", "JSON"})
//	public void setOutputFormat(String outputFormat) {
//		this.outputFormat = Misc.parse(OutputFormat.class, outputFormat);
//	}
//	public OutputFormat getOutputFormatEnum() {
//		return outputFormat;
//	}
//	

}
