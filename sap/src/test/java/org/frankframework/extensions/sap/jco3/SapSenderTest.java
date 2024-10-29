package org.frankframework.extensions.sap.jco3;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoFunctionTemplate;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.extensions.sap.SapException;
import org.frankframework.parameters.Parameter;
import org.frankframework.stream.Message;
import org.frankframework.util.SapSystemListItem;

public class SapSenderTest {

	private final String sapSystemName = "sap-system";
	
	private SapSender sender;
	private SapSystem sapSystem;

	private MockedStatic<SapSystemDataProvider> sapSystemDataProviderMockedStatic;

	@BeforeEach
	public void setUp() throws SapException, JCoException, SenderException {
		SapSystemListItem.clear();

		sapSystemDataProviderMockedStatic = mockStatic(SapSystemDataProvider.class);
		sapSystemDataProviderMockedStatic.when(SapSystemDataProvider::getInstance).thenReturn(mock(SapSystemDataProvider.class));

		sender = spy(SapSender.class);
		sender.setName(sender.getClass().getSimpleName()+" under test");

		doReturn(mock(JCoFunction.class)).when(sender).getFunction(any(), any());
		doReturn(mock(JCoDestination.class)).when(sender).getDestination(any(), any());
		doReturn(mock(JCoFunctionTemplate.class)).when(sender).getFunctionTemplate(any(), any());

		sapSystem = spy(SapSystem.class);
		doNothing().when(sapSystem).openSystem();

		sapSystem.setName(sapSystemName);
		SapSystemListItem.registerItem(sapSystem);
	}

	@AfterEach
	public void tearDown() {
		sapSystemDataProviderMockedStatic.close();
	}

	@Test
	public void testNoFunctionName() {
		sender.setFunctionName(null);
		sender.setSapSystemName(sapSystemName);

		assertThrows(ConfigurationException.class, () -> sender.configure());
	}

	@Test
	public void testFunctionNameAttributeAndParameter() {
		sender.setFunctionName("function-name");
		sender.setSapSystemName(sapSystemName);
		sender.addParameter(new Parameter("functionName", "function-name"));

		assertThrows(ConfigurationException.class, () -> sender.configure());
	}

	@Test
	public void testSapSystemDoesntExist() {
		sender.setFunctionName("function-name");

		assertThrows(ConfigurationException.class, () -> sender.configure());
	}

	@Test
	public void testSapSystemExists() {
		sender.setFunctionName("function-name");
		sender.setSapSystemName(sapSystemName);

		assertDoesNotThrow(() -> sender.configure());
	}

	@Test
	public void testSendMessage() throws IOException {
		sender.setFunctionName("function-name");
		sender.setSapSystemName(sapSystemName);

		assertDoesNotThrow(() -> sender.configure());
		assertDoesNotThrow(() -> sender.start());

		PipeLineSession session = new PipeLineSession();
		Message message = assertDoesNotThrow(() -> sender.sendMessageOrThrow(Message.asMessage(""), session));

		assertEquals("<response function=\"null\"></response>", message.asString());

		session.close();
	}

}
