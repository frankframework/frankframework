package org.frankframework.extensions.sap;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;

import com.sap.conn.jco.JCoFunctionTemplate;

import org.frankframework.configuration.ConfigurationException;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.extensions.sap.jco3.SapSystemDataProvider;
import org.frankframework.parameters.Parameter;

import org.frankframework.stream.Message;
import org.frankframework.util.GlobalListItem;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;

public class SapSenderTest {

	private final String sapSystemName = "sap-system";
	
	private SapSender pipe;
	private SapSystem sapSystem;

	private MockedStatic<SapSystemDataProvider> sapSystemDataProviderMockedStatic;

	@BeforeEach
	public void setUp() throws SapException, JCoException, SenderException {
		GlobalListItem.clear();

		sapSystemDataProviderMockedStatic = mockStatic(SapSystemDataProvider.class);
		sapSystemDataProviderMockedStatic.when(SapSystemDataProvider::getInstance).thenReturn(mock(SapSystemDataProvider.class));

		pipe = spy(SapSender.class);
		pipe.setName(pipe.getClass().getSimpleName()+" under test");

		doReturn(mock(JCoFunction.class)).when(pipe).getFunction(any(), any());
		doReturn(mock(JCoDestination.class)).when(pipe).getDestination(any(), any());
		doReturn(mock(JCoFunctionTemplate.class)).when(pipe).getFunctionTemplate(any(), any());

		sapSystem = spy(SapSystem.class);
		doNothing().when(sapSystem).openSystem();

		sapSystem.setName(sapSystemName);
		sapSystem.registerItem(pipe);
	}

	@AfterEach
	public void tearDown() {
		sapSystemDataProviderMockedStatic.close();
	}

	@Test
	public void testNoFunctionName() {
		pipe.setFunctionName(null);
		pipe.setSapSystemName(sapSystemName);

		assertThrows(ConfigurationException.class, () -> pipe.configure());
	}

	@Test
	public void testFunctionNameAttributeAndParameter() {
		pipe.setFunctionName("function-name");
		pipe.setSapSystemName(sapSystemName);
		pipe.addParameter(new Parameter("functionName", "function-name"));

		assertThrows(ConfigurationException.class, () -> pipe.configure());
	}

	@Test
	public void testSapSystemDoesntExist() {
		pipe.setFunctionName("function-name");

		assertThrows(ConfigurationException.class, () -> pipe.configure());
	}

	@Test
	public void testSapSystemExists() {
		pipe.setFunctionName("function-name");
		pipe.setSapSystemName(sapSystemName);

		assertDoesNotThrow(() -> pipe.configure());
	}

	@Test
	public void testSendMessage() throws IOException {
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
