/*
   Copyright 2015-2017 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
package org.frankframework.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nonnull;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IMessageBrowser;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.ProcessState;
import org.frankframework.dbms.JdbcException;
import org.frankframework.doc.Default;
import org.frankframework.doc.Optional;
import org.frankframework.doc.Protected;
import org.frankframework.receivers.MessageWrapper;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.stream.Message;
import org.frankframework.util.JdbcUtil;
import org.frankframework.util.RenamingObjectInputStream;
import org.frankframework.util.StringUtil;

/**
 * Read messages from the IBISSTORE database table previously stored by a {@link MessageStoreSender}.
 * <p>
 * Example configuration:
 * <pre>{@code
 * 	<Receiver
 * 		name="03 MessageStoreReceiver"
 * 		numThreads="4"
 * 		transactionAttribute="Required"
 * 		pollInterval="1">
 * 		<MessageStoreListener
 * 			name="03 MessageStoreListener"
 * 			slotId="${instance.name}/TestMessageStore"
 * 			statusValueInProcess="I" />
 * 	</Receiver>
 * }</pre>
 *
 * If you have a <code>MessageStoreListener</code>, failed messages are automatically kept in database
 * table IBISSTORE. Messages are also kept after successful processing. The state of a message
 * is distinguished by the <code>TYPE</code> field, as follows:
 * <ul>
 * <li> <code>M</code>: The message is new. From a functional perspective, it is in the message store.
 * <li> <code>E</code>: There was an error processing the message. From a functional perspective, it is in the error store.
 * <li> <code>A</code>: The message was successfully processed. From a functional perspective, it is in the message log.
 * </ul>
 * Another way to say this is that a <code>MessageStoreListener</code> acts as a message log and as an error store.
 * If you have it, you do not need to add
 * a <code>JdbcErrorStorage</code> or <code>JdbcMessageLog</code> within the same receiver.
 * <br/><br/>
 * See /IAF_util/IAF_DatabaseChangelog.xml for the structure of table IBISSTORE.
 *
 * @author Jaco de Groot
 */
public class MessageStoreListener extends JdbcTableListener<Serializable> {

	private static final String DEFAULT_TABLE_NAME="IBISSTORE";
	private static final String DEFAULT_KEY_FIELD="MESSAGEKEY";
	private static final String DEFAULT_MESSAGE_FIELD="MESSAGE";
	private static final String DEFAULT_MESSAGEID_FIELD="MESSAGEID";
	private static final String DEFAULT_CORRELATIONID_FIELD="CORRELATIONID";
	private static final String DEFAULT_STATUS_FIELD="TYPE";
	private static final String DEFAULT_TIMESTAMP_FIELD="MESSAGEDATE";
	private static final String DEFAULT_COMMENT_FIELD="COMMENTS";

	private static final String DEFAULT_STATUS_VALUE_AVAILABLE=IMessageBrowser.StorageType.MESSAGESTORAGE.getCode();
	private static final String DEFAULT_STATUS_VALUE_PROCESSED=IMessageBrowser.StorageType.MESSAGELOG_RECEIVER.getCode();
	private static final String DEFAULT_STATUS_VALUE_ERROR=IMessageBrowser.StorageType.ERRORSTORAGE.getCode();

	private @Getter String slotId;
	private @Getter String sessionKeys = null;
	private @Getter boolean moveToMessageLog = true;

	private List<String> sessionKeysList;

	public MessageStoreListener() {
		setTableName(DEFAULT_TABLE_NAME);
		setKeyField(DEFAULT_KEY_FIELD);
		setMessageField(DEFAULT_MESSAGE_FIELD);
		setMessageIdField(DEFAULT_MESSAGEID_FIELD);
		setCorrelationIdField(DEFAULT_CORRELATIONID_FIELD);
		super.setMessageFieldType(MessageFieldType.BLOB);
		setBlobSmartGet(true);
		setStatusField(DEFAULT_STATUS_FIELD);
		setTimestampField(DEFAULT_TIMESTAMP_FIELD);
		setCommentField(DEFAULT_COMMENT_FIELD);
		setStatusValueAvailable(DEFAULT_STATUS_VALUE_AVAILABLE);
		setStatusValueProcessed(DEFAULT_STATUS_VALUE_PROCESSED);
		setStatusValueError(DEFAULT_STATUS_VALUE_ERROR);
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		extractSessionKeyList();
		if (isMoveToMessageLog()) {
			String setClause = "EXPIRYDATE = "+getDbmsSupport().getDateAndOffset(getDbmsSupport().getSysDate(),30);
			setUpdateStatusQuery(ProcessState.DONE, createUpdateStatusQuery(getStatusValue(ProcessState.DONE),setClause));
		} else {
			String query = "DELETE FROM "+getTableName()+" WHERE "+getKeyField()+" = ?";
			setUpdateStatusQuery(ProcessState.DONE, query);
		}
	}

	public void extractSessionKeyList() {
		sessionKeysList = StringUtil.split(sessionKeys);
	}

	@Override
	protected RawMessageWrapper<Serializable> extractRawMessage(ResultSet rs) throws JdbcException {
		try (InputStream blobStream = JdbcUtil.getBlobInputStream(getDbmsSupport(), rs, getMessageField(), isBlobsCompressed());
			ObjectInputStream ois = new RenamingObjectInputStream(blobStream)) {

			// After creating the BlobInputStream, it should be read before accessing any other fields of the RecordSet
			Object rawMessage = ois.readObject();

			String key = getStringFieldOrNull(rs, getKeyField());
			String cid = getStringFieldOrNull(rs, getCorrelationIdField());
			String mid = getStringFieldOrNull(rs, getMessageIdField());

			RawMessageWrapper<Serializable> rawMessageWrapper;
			if (rawMessage instanceof RawMessageWrapper<?>) {
				//noinspection unchecked
				rawMessageWrapper = (RawMessageWrapper<Serializable>) rawMessage;
			} else {
				rawMessageWrapper = new RawMessageWrapper<>((Serializable) rawMessage, mid != null ? mid : key, cid);
			}
			if (key != null) {
				rawMessageWrapper.getContext().put(PipeLineSession.STORAGE_ID_KEY, key);
			}
			return rawMessageWrapper;
		} catch (Exception e) {
			throw new JdbcException(e);
		}
	}

	private String getStringFieldOrNull(ResultSet rs, String columnLabel) throws SQLException {
		int columnIdx;
		try {
			columnIdx = rs.findColumn(columnLabel);
		} catch (SQLException e) {
			return null;
		}
		return rs.getString(columnIdx);
	}

	@Override
	public Message extractMessage(@Nonnull RawMessageWrapper<Serializable> rawMessageWrapper, @Nonnull Map<String, Object> context) throws ListenerException {
		// If sessionKeys were set to be stored with message by the MessageStoreSender, they'll be in the context of
		// the (Raw)MessageWrapper.
		// If not, then the RawMessageWrapper context still contains some info we want to retain, such as MID, CID and Storage Key.
		// So copying it here to thread context is always the right thing.
		context.putAll(rawMessageWrapper.getContext());

		// Now get or create the Message
		if (rawMessageWrapper instanceof MessageWrapper<?> messageWrapper) {
			return messageWrapper.getMessage();
		}
		Serializable rawMessage = rawMessageWrapper.getRawMessage();
		if (rawMessage instanceof Message message) {
			return message;
		}
		// Handle the Legacy CSV format
		if (sessionKeysList != null && !sessionKeysList.isEmpty() && rawMessage instanceof String messageData) {
			return convertFromCsv(messageData, context);
		}
		return Message.asMessage(rawMessage);
	}

	private Message convertFromCsv(@Nonnull String messageData, Map<String, Object> threadContext) throws ListenerException {
		Message message;
		try(CSVParser parser = CSVParser.parse(messageData, CSVFormat.DEFAULT)) {
			CSVRecord csvRecord = parser.getRecords().get(0);
			message = new Message(csvRecord.get(0));
			for (int i = 1; i < csvRecord.size(); i++) {
				if (sessionKeysList.size() >= i) {
					threadContext.put(sessionKeysList.get(i - 1), csvRecord.get(i));
				}
			}
		} catch (IOException e) {
			throw new ListenerException("cannot convert message",e);
		}
		return message;
	}

	protected IMessageBrowser<Serializable> augmentMessageBrowser(IMessageBrowser<Serializable> browser) {
		if (browser instanceof JdbcTableMessageBrowser<?> jtmb) {
			jtmb.setExpiryDateField("EXPIRYDATE");
			jtmb.setHostField("HOST");
		}
		return browser;
	}

	@Override
	public IMessageBrowser<Serializable> getMessageBrowser(ProcessState state) {
		IMessageBrowser<Serializable> browser = super.getMessageBrowser(state);
		if (browser!=null) {
			return augmentMessageBrowser(browser);
		}
		return null;
	}

	@Override
	public String getSelectCondition() {
		String conditionClause = super.getSelectCondition();
		if (StringUtils.isNotEmpty(conditionClause)) {
			conditionClause = "("+conditionClause+")";
		}
		String slotIdClause = StringUtils.isNotEmpty(getSlotId()) ? "SLOTID='"+slotId+"'" : null;
		return StringUtil.concatStrings(slotIdClause, " AND ", conditionClause);
	}


	/**
	 * Identifier for this service
	 */
	public void setSlotId(String slotId) {
		this.slotId = slotId;
	}

	/**
	 * Comma separated list of sessionKey's to be read together with the message. Please note: corresponding {@link MessageStoreSender} must have the same value for this attribute
	 */
	public void setSessionKeys(String sessionKeys) {
		this.sessionKeys = sessionKeys;
	}

	@Override
	@Default (DEFAULT_TABLE_NAME)
	@Optional
	public void setTableName(String string) {
		super.setTableName(string);
	}

	@Override
	@Default (DEFAULT_KEY_FIELD)
	public void setKeyField(String fieldname) {
		super.setKeyField(fieldname);
	}

	@Override
	@Default (DEFAULT_MESSAGE_FIELD)
	public void setMessageField(String fieldname) {
		super.setMessageField(fieldname);
	}

	@Override
	@Default (DEFAULT_MESSAGEID_FIELD)
	public void setMessageIdField(String fieldname) {
		super.setMessageIdField(fieldname);
	}

	@Override
	@Default (DEFAULT_CORRELATIONID_FIELD)
	public void setCorrelationIdField(String fieldname) {
		super.setCorrelationIdField(fieldname);
	}

	@Override
	@Protected
	public void setMessageFieldType(MessageFieldType fieldtype) {
		throw new UnsupportedOperationException("MessageFieldType is always BLOB for the MessageStoreListener, use a JdbcTableListener instead if you need CLOB of VARCHAR support");
	}

	@Override
	@Default ("<code>true</code>")
	public void setBlobSmartGet(boolean b) {
		super.setBlobSmartGet(b);
	}

	@Override
	@Default (DEFAULT_STATUS_FIELD)
	@Optional
	public void setStatusField(String fieldname) {
		super.setStatusField(fieldname);
	}

	@Override
	@Default (DEFAULT_TIMESTAMP_FIELD)
	public void setTimestampField(String fieldname) {
		super.setTimestampField(fieldname);
	}

	@Override
	@Default (DEFAULT_COMMENT_FIELD)
	public void setCommentField(String commentField) {
		super.setCommentField(commentField);
	}

	/**
	 * Value of statusField indicating row is available to be processed. If set empty, any row not having any of the other status values is considered available.
	 *
	 * @ff.default <code>M</code>
	 */
	@Override
	public void setStatusValueAvailable(String string) {
		super.setStatusValueAvailable(string);
	}

	/**
	 * Value of status field indicating is being processed. Set to <code>I</code> if database has no SKIP LOCKED functionality, the Receiver cannot be set to <code>Required</code> or <code>RequiresNew</code>, or to support programmatic retry.
	 */
	@Override
	public void setStatusValueInProcess(String string) {
		super.setStatusValueInProcess(string);
	}

	@Override
	@Default ("<code>E</code>")
	@Optional
	public void setStatusValueError(String string) {
		super.setStatusValueError(string);
	}

	@Override
	@Default ("<code>A</code>")
	@Optional
	public void setStatusValueProcessed(String string) {
		super.setStatusValueProcessed(string);
	}

	/**
	 * Value of status field indicating message is on Hold, temporarily. If required, suggested value is <code>H</code>.
	 */
	@Override
	public void setStatusValueHold(String string) {
		super.setStatusValueHold(string);
	}

	/**
	 * Move to messageLog after processing, as the message is already stored in the ibisstore only some fields need to be updated. When set <code>false</code>, messages are deleted after being processed
	 * @ff.default <code>true</code>
	 */
	public void setMoveToMessageLog(boolean moveToMessageLog) {
		this.moveToMessageLog = moveToMessageLog;
	}

}
