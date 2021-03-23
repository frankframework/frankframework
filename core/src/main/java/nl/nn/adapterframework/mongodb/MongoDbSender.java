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

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.stream.StreamingSenderBase;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.Misc;

public class MongoDbSender extends StreamingSenderBase {
	
	private @Getter String host;
	private @Getter int port=27017;
	private @Getter String authAlias;
	private @Getter String username;
	private @Getter String password;
//	private @Getter String authSource;
//	private @Getter String options;
	private @Getter String database;
	private @Getter String collection;
	private MongoAction action;
	private @Getter String filter;
	
	
	private CredentialFactory cf;
	private String url;
	
	private MongoClient mongoClient;
	private MongoDatabase mongoDatabase;
	
	public enum MongoAction {
//		LISTDATABASES,
//		CREATECOLLECTION,
		INSERTONE,
		INSERTMANY,
		FINDONE,
		FINDMANY,
		UPDATEONE,
		UPDATEMANY,
		DELETEONE,
		DELETEMANY;
	}
	
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getHost())) {
			throw new ConfigurationException("attribute host not specified");
		}
		if (StringUtils.isEmpty(getDatabase())) {
			throw new ConfigurationException("attribute database not specified");
		}
		if (getActionEnum()==null) {
			throw new ConfigurationException("attribute action not specified");
		}
		if (StringUtils.isNotEmpty(getAuthAlias()) || StringUtils.isNotEmpty(getUsername()) || StringUtils.isNotEmpty(getPassword())) {
			cf = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
		}
		url = "mongodb://"+ (cf!=null ? cf.getUsername()+":"+cf.getPassword()+"@" : "") + getHost()+":"+getPort();
	}

	@Override
	public void open() throws SenderException {
		mongoClient = MongoClients.create(url);
		mongoDatabase = mongoClient.getDatabase(getDatabase());
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
		try {
			switch (getActionEnum()) {
//			case LISTDATABASES:
//				List<Document> databases = mongoClient.listDatabases().into(new ArrayList<>());
//				databases.forEach(db -> System.out.println(db.toJson()));
//				return Message.nullMessage();
//			case CREATECOLLECTION:
//				mongoDatabase.createCollection(message.asString());
//				return Message.nullMessage();
			case INSERTONE:
				getCollection(message, session).insertOne(getDocument(message));
				break;
			case INSERTMANY:
				getCollection(message, session).insertMany(getDocuments(message));
				break;
			case FINDONE:
				Document findOne = (Document) getCollection(message, session).find(getFilter(message)).first();
				return new PipeRunResult(null, new Message(findOne.toJson()));
			case FINDMANY:
				return renderCollection(getCollection(message, session).find(getFilter(message)), session, next);
			case UPDATEONE:
				getCollection(message, session).updateOne(getFilter(null), getDocument(message));
				break;
			case UPDATEMANY:
				getCollection(message, session).updateMany(getFilter(null), getDocument(message));
				break;
			case DELETEONE:
				getCollection(message, session).deleteOne(getFilter(message));
				break;
			case DELETEMANY:
				getCollection(message, session).deleteMany(getFilter(message));
				break;
			default:
				throw new SenderException("Unknown action ["+getActionEnum()+"]");
			}
			return new PipeRunResult(null, Message.nullMessage());
		} catch (IOException | StreamingException e) {
			throw new SenderException("Cannot execute action ["+getActionEnum()+"]", e);
		}
	}

	protected Document getFilter(Message message) throws IOException {
		if (StringUtils.isNotEmpty(getFilter())) {
			return getDocument(getFilter());
		}
		if (!Message.isEmpty(message)) {
			return getDocument(message);
		}
		return getDocument("");
	}
	
	protected Document getDocument(JsonObject object) {
		return getDocument(object.toString());
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
			documents.add(getDocument(object));
		}
		return documents;
	}

	protected PipeRunResult renderCollection(FindIterable<Document> findResults, IPipeLineSession session, IForwardTarget next) throws StreamingException {
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
	
	protected MongoCollection getCollection(Message message, IPipeLineSession session) {
		String collectionName = getCollection();
		return mongoDatabase.getCollection(collectionName);
	}
	

	public String getUrl() {
		return url;
	}
	
	@IbisDoc({"1", "The MongoDB hostname", ""})
	public void setHost(String host) {
		this.host = host;
	}

	@IbisDoc({"2", "The MongoDB port", "27017"})
	public void setPort(int port) {
		this.port = port;
	}

	@IbisDoc({"3", "AuthAlias to obtain username and password", ""})
	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}

	@IbisDoc({"4", "Default username to authenticate to MongoDB", ""})
	public void setUsername(String username) {
		this.username = username;
	}

	@IbisDoc({"5", "Default password to authenticate to MongoDB", ""})
	public void setPassword(String password) {
		this.password = password;
	}

//	@IbisDoc({"6", "MongoDB database to use as authentication database", ""})
//	public void setAuthSource(String authSource) {
//		this.authSource = authSource;
//	}
//
//	@IbisDoc({"7", "MongoDB options", ""})
//	public void setOptions(String options) {
//		this.options = options;
//	}

	@IbisDoc({"8", "Database to connect to", ""})
	public void setDatabase(String database) {
		this.database = database;
	}

	@IbisDoc({"9", "Collection", ""})
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
	
	@IbisDoc({"11", "Filter", ""})
	public void setFilter(String filter) {
		this.filter = filter;
	}

}
