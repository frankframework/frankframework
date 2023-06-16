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
package nl.nn.adapterframework.jdbc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.annotation.Nonnull;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.doc.Default;
import nl.nn.adapterframework.doc.Optional;
import nl.nn.adapterframework.receivers.MessageWrapper;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.StringUtil;

/**
 * Read messages from the IBISSTORE database table previously stored by a
 * {@link MessageStoreSender}.
 *
 * Example configuration:
 * <code><pre>
	&lt;Receiver
		name="03 MessageStoreReceiver"
		numThreads="4"
		transactionAttribute="Required"
		pollInterval="1"
		&gt;
		&lt;MessageStoreListener
			name="03 MessageStoreListener"
			slotId="${instance.name}/TestMessageStore"
			statusValueInProcess="I"
		/&gt;
	&lt;/Receiver&gt;

 * </pre></code>
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
public class MessageStoreListener<M> extends JdbcTableListener<M> {

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

	{
		setTableName(DEFAULT_TABLE_NAME);
		setKeyField(DEFAULT_KEY_FIELD);
		setMessageField(DEFAULT_MESSAGE_FIELD);
		setMessageIdField(DEFAULT_MESSAGEID_FIELD);
		setCorrelationIdField(DEFAULT_CORRELATIONID_FIELD);
		setMessageFieldType(MessageFieldType.BLOB);
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
		if (sessionKeys != null) {
			extractSessionKeyList();
		}
		if (isMoveToMessageLog()) {
			String setClause = "EXPIRYDATE = "+getDbmsSupport().getDateAndOffset(getDbmsSupport().getSysDate(),30);
			setUpdateStatusQuery(ProcessState.DONE, createUpdateStatusQuery(getStatusValue(ProcessState.DONE),setClause));
		} else {
			String query = "DELETE FROM "+getTableName()+" WHERE "+getKeyField()+" = ?";
			setUpdateStatusQuery(ProcessState.DONE, query);
			setUpdateStatusQuery(ProcessState.ERROR, query);
		}
	}

	public void extractSessionKeyList() {
		if (sessionKeys != null) {
			sessionKeysList = new ArrayList<>();
			StringTokenizer stringTokenizer = new StringTokenizer(sessionKeys, ",");
			while (stringTokenizer.hasMoreElements()) {
				sessionKeysList.add(stringTokenizer.nextToken());
			}
		}
	}

	@Override
	public Message extractMessage(@Nonnull RawMessageWrapper<M> rawMessage, @Nonnull Map<String, Object> context) throws ListenerException {
		if (sessionKeys != null) {
			return convertToMessage(rawMessage, context);
		}
		return super.extractMessage(rawMessage, context);
	}

	private Message convertToMessage(@Nonnull RawMessageWrapper<M> rawMessageWrapper, Map<String, Object> threadContext) throws ListenerException {
		Message message;
		String messageData = extractStringData(rawMessageWrapper);
		try(CSVParser parser = CSVParser.parse(messageData, CSVFormat.DEFAULT)) {
			CSVRecord csvRecord = parser.getRecords().get(0);
			message = new Message(csvRecord.get(0));
			for (int i=1; i<csvRecord.size();i++) {
				if (sessionKeysList.size()>=i) {
					threadContext.put(sessionKeysList.get(i-1), csvRecord.get(i));
				}
			}
		} catch (IOException e) {
			throw new ListenerException("cannot convert message",e);
		}
		return message;
	}

	private static String extractStringData(@Nonnull RawMessageWrapper<?> rawMessageWrapper) throws ListenerException {
		if (rawMessageWrapper instanceof MessageWrapper) {
			try {
				return ((MessageWrapper<?>) rawMessageWrapper).getMessage().asString();
			} catch (IOException e) {
				throw new ListenerException("Exception extracting string data from message", e);
			}
		} else {
			return rawMessageWrapper.getRawMessage().toString();
		}
	}

	protected IMessageBrowser<M> augmentMessageBrowser(IMessageBrowser<M> browser) {
		if (browser instanceof JdbcTableMessageBrowser) {
			JdbcTableMessageBrowser<?> jtmb = (JdbcTableMessageBrowser<?>)browser;
			jtmb.setExpiryDateField("EXPIRYDATE");
			jtmb.setHostField("HOST");
		}
		return browser;
	}

	@Override
	public IMessageBrowser<M> getMessageBrowser(ProcessState state) {
		IMessageBrowser<M> browser = super.getMessageBrowser(state);
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
	@Default ("BLOB")
	public void setMessageFieldType(MessageFieldType fieldtype) {
		super.setMessageFieldType(fieldtype);
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
