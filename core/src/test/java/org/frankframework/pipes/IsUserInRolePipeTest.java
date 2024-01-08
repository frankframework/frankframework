package org.frankframework.pipes;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ISecurityHandler;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * IsUserInRolePipe unit tests.
 *
 * @author <Laurens Mäkel>
 */
public class IsUserInRolePipeTest extends PipeTestBase<IsUserInRolePipe> {

	public final String ROLE = "MyRole";
	public final String ROLE2 = "MyRole1";
	public final String ROLE3 = "MyRole2";

	public final String NOT_IN_ROLE_FORWARD_NAME = "notInRole";
	public final String NOT_IN_ROLE_FORWARD_PATH = "doSomething";

	@Override
	public IsUserInRolePipe createPipe() throws ConfigurationException {
		return new IsUserInRolePipe();
	}

	@Test
	public void requiresNotInRoleForward() {
		assertThrows(ConfigurationException.class, () -> pipe.configure(), "notInRoleForwardName not found");
	}

	@Test
	public void userIsInSingleRoleAttribute() throws Exception {
		// Given
		ISecurityHandler securityHandler = mock(ISecurityHandler.class);
		PipeLineSession mockedSession = mock(PipeLineSession.class);
		when(mockedSession.getSecurityHandler()).thenReturn(securityHandler);
		when(securityHandler.isUserInRole(any(String.class))).thenReturn(true);

		setNotInRoleForward();
		pipe.setRole(ROLE);
		pipe.configure();

		// When
		PipeRunResult prr = doPipe(pipe, "", mockedSession);

		// Expect
		assertEquals(PipeForward.SUCCESS_FORWARD_NAME, prr.getPipeForward().getName());
	}

	@Test
	public void userIsInSingleRoleInput() throws Exception {
		// Given
		ISecurityHandler securityHandler = mock(ISecurityHandler.class);
		PipeLineSession mockedSession = mock(PipeLineSession.class);
		when(mockedSession.getSecurityHandler()).thenReturn(securityHandler);
		when(securityHandler.isUserInRole(any(String.class))).thenReturn(true);

		setNotInRoleForward();
		pipe.configure();

		// When
		PipeRunResult prr = doPipe(pipe, ROLE, mockedSession);

		// Expect
		assertEquals(PipeForward.SUCCESS_FORWARD_NAME, prr.getPipeForward().getName());
	}

	@Test
	public void userIsNotInSingleRoleAttribute() throws Exception {
		// Given
		ISecurityHandler securityHandler = mock(ISecurityHandler.class);
		PipeLineSession mockedSession = mock(PipeLineSession.class);
		when(mockedSession.getSecurityHandler()).thenReturn(securityHandler);
		when(securityHandler.isUserInRole(any(String.class))).thenReturn(false);

		setNotInRoleForward();
		pipe.setRole(ROLE);
		pipe.configure();

		// When
		PipeRunResult prr = doPipe(pipe, "", mockedSession);

		// Expect
		assertEquals(NOT_IN_ROLE_FORWARD_NAME, prr.getPipeForward().getName());
	}

	@Test
	public void userIsNotInSingleRoleInput() throws Exception {
		// Given
		ISecurityHandler securityHandler = mock(ISecurityHandler.class);
		PipeLineSession mockedSession = mock(PipeLineSession.class);
		when(mockedSession.getSecurityHandler()).thenReturn(securityHandler);
		when(securityHandler.isUserInRole(any(String.class))).thenReturn(false);

		setNotInRoleForward();
		pipe.configure();

		// When
		PipeRunResult prr = doPipe(pipe, ROLE, mockedSession);

		// Expect
		assertEquals(NOT_IN_ROLE_FORWARD_NAME, prr.getPipeForward().getName());
	}

	@Test
	public void userIsInMultiRoleInput() throws Exception {
		// Given
		ISecurityHandler securityHandler = mock(ISecurityHandler.class);
		PipeLineSession mockedSession = mock(PipeLineSession.class);
		when(mockedSession.getSecurityHandler()).thenReturn(securityHandler);

		when(securityHandler.isUserInRole(ROLE)).thenReturn(false);
		when(securityHandler.isUserInRole(ROLE2)).thenReturn(true);
		when(securityHandler.isUserInRole(ROLE3)).thenReturn(true);

		setNotInRoleForward();
		pipe.configure();

		// When
		PipeRunResult prr = doPipe(pipe, ROLE+","+ROLE2+","+ROLE3, mockedSession);

		// Expect
		assertEquals(PipeForward.SUCCESS_FORWARD_NAME, prr.getPipeForward().getName());
	}

	@Test
	public void userIsInMultiRoleInputWithForwards() throws Exception {
		// Given
		ISecurityHandler securityHandler = mock(ISecurityHandler.class);
		PipeLineSession mockedSession = mock(PipeLineSession.class);
		when(mockedSession.getSecurityHandler()).thenReturn(securityHandler);

		when(securityHandler.isUserInRole(ROLE)).thenReturn(false);
		when(securityHandler.isUserInRole(ROLE2)).thenReturn(true);
		when(securityHandler.isUserInRole(ROLE3)).thenReturn(true);

		setNotInRoleForward();
		pipe.registerForward(new PipeForward(ROLE, ""));
		pipe.registerForward(new PipeForward(ROLE2, ""));
		pipe.registerForward(new PipeForward(ROLE3, ""));
		pipe.configure();

		// When
		PipeRunResult prr = doPipe(pipe, ROLE+","+ROLE2+","+ROLE3, mockedSession);

		// Expect
		assertEquals(ROLE2, prr.getPipeForward().getName());
	}

	@Test
	public void userIsInMultiRoleAttribute() throws Exception {
		// Given
		ISecurityHandler securityHandler = mock(ISecurityHandler.class);
		PipeLineSession mockedSession = mock(PipeLineSession.class);
		when(mockedSession.getSecurityHandler()).thenReturn(securityHandler);

		when(securityHandler.isUserInRole(ROLE)).thenReturn(false);
		when(securityHandler.isUserInRole(ROLE2)).thenReturn(true);
		when(securityHandler.isUserInRole(ROLE3)).thenReturn(true);

		setNotInRoleForward();
		pipe.setRole(ROLE+","+ROLE2+","+ROLE3);
		pipe.configure();

		// When
		PipeRunResult prr = doPipe(pipe, "", mockedSession);

		// Expect
		assertEquals(PipeForward.SUCCESS_FORWARD_NAME, prr.getPipeForward().getName());
	}

	@Test
	public void userIsInMultiRoleAttributeWithForwards() throws Exception {
		// Given
		ISecurityHandler securityHandler = mock(ISecurityHandler.class);
		PipeLineSession mockedSession = mock(PipeLineSession.class);
		when(mockedSession.getSecurityHandler()).thenReturn(securityHandler);

		when(securityHandler.isUserInRole(ROLE)).thenReturn(false);
		when(securityHandler.isUserInRole(ROLE2)).thenReturn(true);
		when(securityHandler.isUserInRole(ROLE3)).thenReturn(true);

		setNotInRoleForward();
		pipe.registerForward(new PipeForward(ROLE, ""));
		pipe.registerForward(new PipeForward(ROLE2, ""));
		pipe.registerForward(new PipeForward(ROLE3, ""));
		pipe.setRole(ROLE+","+ROLE2+","+ROLE3);
		pipe.configure();

		// When
		PipeRunResult prr = doPipe(pipe, "", mockedSession);

		// Expect
		assertEquals(ROLE2, prr.getPipeForward().getName());
	}

	@Test
	public void userIsNotMultiRoleInput() throws Exception {
		// Given
		ISecurityHandler securityHandler = mock(ISecurityHandler.class);
		PipeLineSession mockedSession = mock(PipeLineSession.class);
		when(mockedSession.getSecurityHandler()).thenReturn(securityHandler);

		when(securityHandler.isUserInRole(ROLE)).thenReturn(false);
		when(securityHandler.isUserInRole(ROLE2)).thenReturn(false);
		when(securityHandler.isUserInRole(ROLE3)).thenReturn(false);

		setNotInRoleForward();
		pipe.configure();

		// When
		PipeRunResult prr = doPipe(pipe, ROLE+","+ROLE2+","+ROLE3, mockedSession);

		// Expect
		assertEquals(NOT_IN_ROLE_FORWARD_NAME, prr.getPipeForward().getName());
	}

	@Test
	public void userIsNotMultiRoleInputWithForwards() throws Exception {
		// Given
		ISecurityHandler securityHandler = mock(ISecurityHandler.class);
		PipeLineSession mockedSession = mock(PipeLineSession.class);
		when(mockedSession.getSecurityHandler()).thenReturn(securityHandler);

		when(securityHandler.isUserInRole(ROLE)).thenReturn(false);
		when(securityHandler.isUserInRole(ROLE2)).thenReturn(false);
		when(securityHandler.isUserInRole(ROLE3)).thenReturn(false);

		setNotInRoleForward();
		pipe.registerForward(new PipeForward(ROLE, ""));
		pipe.registerForward(new PipeForward(ROLE2, ""));
		pipe.registerForward(new PipeForward(ROLE3, ""));
		pipe.configure();

		// When
		PipeRunResult prr = doPipe(pipe, ROLE+","+ROLE2+","+ROLE3, mockedSession);

		// Expect
		assertEquals(NOT_IN_ROLE_FORWARD_NAME, prr.getPipeForward().getName());
	}

	@Test
	public void userIsNotMultiRoleAttribute() throws Exception {
		// Given
		ISecurityHandler securityHandler = mock(ISecurityHandler.class);
		PipeLineSession mockedSession = mock(PipeLineSession.class);
		when(mockedSession.getSecurityHandler()).thenReturn(securityHandler);

		when(securityHandler.isUserInRole(ROLE)).thenReturn(false);
		when(securityHandler.isUserInRole(ROLE2)).thenReturn(false);
		when(securityHandler.isUserInRole(ROLE3)).thenReturn(false);

		setNotInRoleForward();
		pipe.setRole(ROLE+","+ROLE2+","+ROLE3);
		pipe.configure();

		// When
		PipeRunResult prr = doPipe(pipe, "", mockedSession);

		// Expect
		assertEquals(NOT_IN_ROLE_FORWARD_NAME, prr.getPipeForward().getName());
	}

	@Test
	public void userIsNotMultiRoleAttributeWithForwards() throws Exception {
		// Given
		ISecurityHandler securityHandler = mock(ISecurityHandler.class);
		PipeLineSession mockedSession = mock(PipeLineSession.class);
		when(mockedSession.getSecurityHandler()).thenReturn(securityHandler);

		when(securityHandler.isUserInRole(ROLE)).thenReturn(false);
		when(securityHandler.isUserInRole(ROLE2)).thenReturn(false);
		when(securityHandler.isUserInRole(ROLE3)).thenReturn(false);

		setNotInRoleForward();
		pipe.registerForward(new PipeForward(ROLE, ""));
		pipe.registerForward(new PipeForward(ROLE2, ""));
		pipe.registerForward(new PipeForward(ROLE3, ""));
		pipe.setRole(ROLE+","+ROLE2+","+ROLE3);
		pipe.configure();

		// When
		PipeRunResult prr = doPipe(pipe, "", mockedSession);

		// Expect
		assertEquals(NOT_IN_ROLE_FORWARD_NAME, prr.getPipeForward().getName());
	}

	protected void setNotInRoleForward() throws ConfigurationException {
		PipeForward notInRole = new PipeForward(NOT_IN_ROLE_FORWARD_NAME, NOT_IN_ROLE_FORWARD_PATH);
		pipe.registerForward(notInRole);
	}


}
