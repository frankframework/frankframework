/*
   Copyright 2015-2017 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.Misc;

/**
 * Read messages from the ibisstore previously stored by a
 * {@link MessageStoreSender}.
 *
 * Example configuration:
 * <code><pre>
		&lt;listener
			name="MyListener"
			className="nl.nn.adapterframework.jdbc.MessageStoreListener"
			datasourceName="${jdbc.datasource.default}"
			slotId="${instance.name}/ServiceName"
			sessionKeys="key1,key2"
		/>
		&lt;!-- On error the message is moved to the errorStorage. And when moveToMessageLog="true" also to the messageLog (after manual resend the messageLog doesn't change). -->
		&lt;errorStorage
			className="nl.nn.adapterframework.jdbc.JdbcTransactionalStorage"
			datasourceName="${jdbc.datasource.default}"
			slotId="${instance.name}/ServiceName"
		/>
 * </pre></code>
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
			sessionKeysList = new ArrayList<>();
			StringTokenizer stringTokenizer = new StringTokenizer(sessionKeys, ",");
			while (stringTokenizer.hasMoreElements()) {
				sessionKeysList.add(stringTokenizer.nextToken());
			}
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

	@Override
	public M getRawMessage(Map<String,Object> threadContext) throws ListenerException {
		M rawMessage = super.getRawMessage(threadContext);
		if (rawMessage != null && sessionKeys != null) {
			MessageWrapper<?> messageWrapper = (MessageWrapper<?>)rawMessage;
			try {
				CSVParser parser = CSVParser.parse(messageWrapper.getMessage().asString(), CSVFormat.DEFAULT);
				CSVRecord record = parser.getRecords().get(0);
				messageWrapper.setMessage(new Message(record.get(0)));
				for (int i=1; i<record.size();i++) {
					if (sessionKeysList.size()>=i) {
						threadContext.put(sessionKeysList.get(i-1), record.get(i));
					}
				}
			} catch (IOException e) {
				throw new ListenerException("cannot convert message",e);
			}
		}
		return rawMessage;
	}

	protected IMessageBrowser<M> augmentMessageBrowser(IMessageBrowser<M> browser) {
		if (browser!=null && browser instanceof JdbcTableMessageBrowser) {
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
		return Misc.concatStrings(slotIdClause, " AND ", conditionClause);
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
