package org.frankframework.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.frankframework.core.IMessageBrowser;
import org.frankframework.core.IMessageBrowsingIterator;
import org.frankframework.core.IMessageBrowsingIteratorItem;
import org.frankframework.core.ProcessState;
import org.frankframework.core.SenderException;
import org.frankframework.management.bus.dto.StorageItemDTO;
import org.frankframework.management.bus.dto.StorageItemsDTO;
import org.frankframework.receivers.Receiver;
import org.frankframework.testutil.junit.DatabaseTest;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.DatabaseTestOptions;
import org.frankframework.testutil.junit.WithLiquibase;
import org.frankframework.util.MessageBrowsingFilter;
import org.frankframework.util.TimeProvider;

@WithLiquibase(tableName = JdbcTableMessageBrowserTest.TEST_TABLE_NAME)
class JdbcTableMessageBrowserTest {
	static final String TEST_TABLE_NAME = "JDBCMSGBROWSERTEST";
	private static final String SLOT_ID = "slot";
	private static final String MESSAGE_ID_FIELD = "MESSAGEID";

	private JdbcTransactionalStorage<String> storage;
	private MessageStoreListener listener;


	@BeforeEach
	public void setup(DatabaseTestEnvironment env) throws Exception {
		Receiver<Serializable> receiver = mock(Receiver.class);
		when(receiver.isTransacted()).thenReturn(false);

		listener = env.createBean(MessageStoreListener.class);
		listener.setTableName(TEST_TABLE_NAME);
		listener.setMessageIdField(MESSAGE_ID_FIELD);
		listener.setSlotId(SLOT_ID);
		listener.setReceiver(receiver);
		listener.configure();

		//noinspection unchecked
		storage = env.createBean(JdbcTransactionalStorage.class);
		storage.setTableName(TEST_TABLE_NAME);
		storage.setIdField(MESSAGE_ID_FIELD);
		storage.setSlotId(SLOT_ID);
		storage.setType("M");
		storage.configure();
	}

	@AfterEach
	public void teardown() {
		if (listener != null) {
			listener.stop(); // does this trigger an exception
		}
	}

	@DatabaseTest
	@DatabaseTestOptions(additionalDataSources = { "H2-MSSQL-Mode", "H2-Oracle-Mode" })
	public void testMessageBrowserIterator() throws Exception {
		// Arrange
		listener.start();
		storage.start();
		createTestMessages(150);

		JdbcTableMessageBrowser<Serializable> messageBrowser = (JdbcTableMessageBrowser<Serializable>)listener.getMessageBrowser(ProcessState.AVAILABLE);
		messageBrowser.configure();

		// Act
		int messageCount = messageBrowser.getMessageCount();
		IMessageBrowsingIterator iterator = messageBrowser.getIterator();
		List<String> messages = new ArrayList<>();
		while (iterator.hasNext()) {
			IMessageBrowsingIteratorItem item = iterator.next();
			messages.add(item.getOriginalId());
		}

		// Assert
		assertEquals(150, messageCount);
		assertEquals(150, messages.size());

		messages.sort(Comparator.naturalOrder());

		assertEquals("mid00000", messages.getFirst());
		assertEquals("mid00149", messages.getLast());
	}

	@DatabaseTest
	@DatabaseTestOptions(additionalDataSources = { "H2-MSSQL-Mode", "H2-Oracle-Mode" })
	public void testMessageBrowserWithStorageItemDTO() throws Exception {
		// Arrange
		listener.start();
		storage.start();
		createTestMessages(250);

		JdbcTableMessageBrowser<Serializable> messageBrowser = (JdbcTableMessageBrowser<Serializable>)listener.getMessageBrowser(ProcessState.AVAILABLE);
		messageBrowser.configure();

		MessageBrowsingFilter filter = new MessageBrowsingFilter(100, 200);
		filter.setSortOrder(IMessageBrowser.SortOrder.ASC);
		StorageItemsDTO dto = new StorageItemsDTO(messageBrowser, filter);

		// Act
		List<StorageItemDTO> messages = dto.getMessages();

		// Assert
		assertEquals(50, messages.size());
		messages.sort(Comparator.comparing(StorageItemDTO::getOriginalId));
		StorageItemDTO first = messages.getFirst();
		StorageItemDTO last = messages.getLast();

		assertEquals("mid00200", first.getOriginalId());
		assertEquals("mid00249", last.getOriginalId());
	}

	private void createTestMessages(int nrOfMessages) throws SenderException {
		long millisNow = TimeProvider.nowAsMillis();
		for (int i = 0; i < nrOfMessages; i++) {
			String formatted = String.format("%05d", i);
			Date receivedDate = new Date(millisNow + 1_000L * i); // Message iterator always order by receivedDate so make sure that it is unique and incrementing. The JVM might loop too fast for now() to actually increment enough for the SQL date-time resolution.
			storage.storeMessage("mid" + formatted, "cid" + formatted, receivedDate, null, null, formatted);
		}
	}
}
