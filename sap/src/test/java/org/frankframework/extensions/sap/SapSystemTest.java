package org.frankframework.extensions.sap;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;

import org.frankframework.stream.Message;

import org.frankframework.util.GlobalListItem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class SapSystemTest {

	private final String sapSystemName = "sap-system";

	private SapSender pipe;
	private SapSystem sapSystem;

	@BeforeEach
	public void setUp() throws SapException, JCoException, SenderException {
		GlobalListItem.clear();

		pipe = spy(SapSender.class);
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		doNothing().when(pipe).openFacade();

		doReturn(mock(JCoFunction.class)).when(pipe).getFunction(any(), any());
		doReturn(mock(JCoDestination.class)).when(pipe).getDestination(any(), any());

		sapSystem = new SapSystem();
		sapSystem.setName(sapSystemName);
		sapSystem.registerItem(pipe);
	}

	@Test
	public void test() throws IOException {
		pipe.setFunctionName("function-name");
		pipe.setSapSystemName(sapSystemName);

		assertDoesNotThrow(() -> pipe.configure());
		assertDoesNotThrow(() -> pipe.open());

		PipeLineSession session = new PipeLineSession();
		Message message = assertDoesNotThrow(() -> pipe.sendMessageOrThrow(Message.asMessage(""), session));

		assertEquals("<response function=\"null\"></response>", message.asString());

		session.close();
	}

}
