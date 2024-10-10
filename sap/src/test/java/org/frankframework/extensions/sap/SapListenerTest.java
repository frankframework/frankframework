package org.frankframework.extensions.sap;

import com.sap.conn.idoc.IDocDocumentList;
import com.sap.conn.jco.JCoFunction;

import com.sap.conn.jco.server.JCoServerContext;

import org.frankframework.configuration.ConfigurationException;

import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.receivers.Receiver;
import org.frankframework.stream.Message;
import org.frankframework.util.GlobalListItem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class SapListenerTest {

	private final String sapSystemName = "sap-system";

	private SapListener pipe;
	private SapSystem sapSystem;
	private Receiver receiver;

	@BeforeEach
	public void setUp() throws SapException {
		GlobalListItem.clear();

		pipe = new SapListener();
		pipe.setName(pipe.getClass().getSimpleName()+" under test");

		receiver = mock(Receiver.class);
		pipe.setHandler(receiver);

		sapSystem = spy(SapSystem.class);
		doNothing().when(sapSystem).openSystem();

		sapSystem.setName(sapSystemName);
		sapSystem.registerItem(pipe);
	}

	@Test
	public void testNoFunctionName() {
		pipe.setProgid(null);

		assertThrows(ConfigurationException.class, () -> pipe.configure());
	}

	@Test
	public void testSapSystemDoesntExist() {
		pipe.setProgid("prog-id");
		pipe.setSapSystemName("doesnt-exist");

		assertThrows(NullPointerException.class, () -> pipe.configure());
	}

	@Test
	public void testSapSystemExists() {
		pipe.setProgid("prog-id");
		pipe.setSapSystemName(sapSystemName);

		assertDoesNotThrow(() -> pipe.configure());
	}

	@Test
	public void testExtractMessage() {
		pipe.setProgid("prog-id");
		pipe.setSapSystemName(sapSystemName);

		assertDoesNotThrow(() -> pipe.configure());

		JCoFunction function = mock(JCoFunction.class);
		RawMessageWrapper<JCoFunction> inputMessage = new RawMessageWrapper<>(function);

		try (Message outputMessage = pipe.extractMessage(inputMessage, Map.of())) {
			assertEquals("<request function=\"null\"></request>", outputMessage.asString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testHandleRequestFunction() {
		pipe.setProgid("prog-id");
		pipe.setSapSystemName(sapSystemName);

		assertDoesNotThrow(() -> pipe.configure());
		assertDoesNotThrow(() -> pipe.handleRequest(mock(JCoServerContext.class), mock(JCoFunction.class)));
	}

	@Test
	@Disabled("JCoIDoc.getIDocFactory().getIDocXMLProcessor() cannot be mocked, even with mockStatic().")
	public void testHandleRequestIDoc() {
		pipe.setProgid("prog-id");
		pipe.setSapSystemName(sapSystemName);

		assertDoesNotThrow(() -> pipe.configure());
		assertDoesNotThrow(() -> pipe.handleRequest(mock(JCoServerContext.class), mock(IDocDocumentList.class)));
	}

}
