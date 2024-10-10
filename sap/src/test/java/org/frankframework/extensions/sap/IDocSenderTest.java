package org.frankframework.extensions.sap;

import com.sap.conn.idoc.IDocDocument;
import com.sap.conn.idoc.IDocRepository;
import com.sap.conn.idoc.jco.JCoIDoc;
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

public class IDocSenderTest {

	private final String sapSystemName = "sap-system";

	private IdocSender pipe;
	private SapSystem sapSystem;

	@BeforeEach
	public void setUp() {
		GlobalListItem.clear();

		pipe = new IdocSender();
		pipe.setName(pipe.getClass().getSimpleName()+" under test");

		sapSystem = new SapSystem();
		sapSystem.setName(sapSystemName);
		sapSystem.registerItem(pipe);
	}

	@Test
	public void testSendMessage() {
		pipe.setSapSystemName(sapSystemName);

		assertDoesNotThrow(() -> pipe.configure());
	}

}
