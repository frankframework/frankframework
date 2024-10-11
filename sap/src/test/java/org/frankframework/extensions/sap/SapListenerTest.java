package org.frankframework.extensions.sap;

import com.sap.conn.idoc.IDocDocumentIterator;
import com.sap.conn.idoc.IDocDocumentList;
import com.sap.conn.idoc.IDocXMLProcessor;
import com.sap.conn.idoc.jco.JCoIDocHandler;
import com.sap.conn.idoc.jco.JCoIDocServerContext;
import com.sap.conn.jco.JCoFunction;

import com.sap.conn.jco.server.JCoServerContext;

import org.frankframework.configuration.ConfigurationException;

import org.frankframework.core.IListener;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.extensions.sap.jco3.SapSystemImpl;
import org.frankframework.monitoring.Severity;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.receivers.Receiver;
import org.frankframework.stream.Message;
import org.frankframework.util.GlobalListItem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class SapListenerTest {

	private final String sapSystemName = "sap-system";

	private SapListener listener;
	private SapSystem sapSystem;
	private Receiver receiver;

	@BeforeEach
	public void setUp() throws SapException {
		GlobalListItem.clear();

		listener = spy(new SapListener());
		listener.setName(listener.getClass().getSimpleName()+" under test");

		receiver = mock(Receiver.class);
		listener.setHandler(receiver);

		sapSystem = spy(SapSystem.class);
		doNothing().when(sapSystem).openSystem();

		sapSystem.setName(sapSystemName);
		sapSystem.registerItem(listener);
	}

	@Test
	public void testNoFunctionName() {
		listener.setProgid(null);

		assertThrows(ConfigurationException.class, () -> listener.configure());
	}

	@Test
	public void testSapSystemDoesntExist() {
		listener.setProgid("prog-id");
		listener.setSapSystemName("doesnt-exist");

		assertThrows(NullPointerException.class, () -> listener.configure());
	}

	@Test
	public void testSapSystemExists() {
		listener.setProgid("prog-id");
		listener.setSapSystemName(sapSystemName);

		assertDoesNotThrow(() -> listener.configure());
	}

	@Test
	public void testExtractMessage() {
		listener.setProgid("prog-id");
		listener.setSapSystemName(sapSystemName);

		assertDoesNotThrow(() -> listener.configure());

		JCoFunction function = mock(JCoFunction.class);
		RawMessageWrapper<JCoFunction> inputMessage = new RawMessageWrapper<>(function);

		try (Message outputMessage = listener.extractMessage(inputMessage, Map.of())) {
			assertEquals("<request function=\"null\"></request>", outputMessage.asString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testHandleRequestFunction() throws ListenerException {
		listener.setProgid("prog-id");
		listener.setSapSystemName(sapSystemName);

		ArgumentCaptor<RawMessageWrapper> messageCaptor = ArgumentCaptor.forClass(RawMessageWrapper.class);
		doNothing().when(receiver).processRawMessage(any(IListener.class), messageCaptor.capture(), any(PipeLineSession.class), anyBoolean());

		JCoFunction function = mock(JCoFunction.class);

		assertDoesNotThrow(() -> listener.configure());
		assertDoesNotThrow(() -> listener.handleRequest(mock(JCoServerContext.class), function));

		assertEquals(function, messageCaptor.getValue().getRawMessage());
	}

	@Test
	public void testHandleRequestIDoc() throws ListenerException {
		listener.setProgid("prog-id");
		listener.setSapSystemName(sapSystemName);

		IDocDocumentList documentList = mock(IDocDocumentList.class);
		when(documentList.iterator()).thenReturn(new IDocDocumentIteratorDummy());

		doReturn(mock(IDocXMLProcessor.class)).when(listener).getIDocXMLProcessor();

		ArgumentCaptor<RawMessageWrapper> messageCaptor = ArgumentCaptor.forClass(RawMessageWrapper.class);
		doReturn(mock(Message.class)).when(receiver).processRequest(any(IListener.class), messageCaptor.capture(), any(Message.class), any(PipeLineSession.class));

		assertDoesNotThrow(() -> listener.configure());
		assertDoesNotThrow(() -> listener.handleRequest(mock(JCoServerContext.class), documentList));

		assertEquals(2, messageCaptor.getAllValues().size());
		assertNull(messageCaptor.getAllValues().get(0).getRawMessage());
		assertNull(messageCaptor.getAllValues().get(1).getRawMessage());
	}

}
