package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ISecurityHandler;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeRunResult;

/**
 * IsUserInRolePipe unit tests.
 *
 * @author <Laurens Mäkel>
 */
class IsUserInRolePipeTest extends PipeTestBase<IsUserInRolePipe> {

	private final String ROLE = "MyRole";
	private final String ROLE2 = "MyRole1";
	private final String ROLE3 = "MyRole2";
	private final String ROLES = ROLE+","+ROLE2+","+ROLE3;

	private final String NOT_IN_ROLE_FORWARD_NAME = "notInRole";
	private final String NOT_IN_ROLE_FORWARD_PATH = "doSomething";

	private ISecurityHandler securityHandler;

	@Override
	public IsUserInRolePipe createPipe() throws ConfigurationException {
		return new IsUserInRolePipe();
	}

	@BeforeEach
	void beforeEach(){
		securityHandler = mock(ISecurityHandler.class);
		session.setSecurityHandler(securityHandler);
	}

	@Test
	void requiresNotInRoleForward() {
		assertThrows(ConfigurationException.class, () -> pipe.configure(), "notInRoleForwardName [null] not found");
	}

	@Test
	void requiresNotInRoleForwardSetButNotFound() {
		assertThrows(ConfigurationException.class, () -> pipe.configure(), "notInRoleForwardName ["+NOT_IN_ROLE_FORWARD_NAME+"] not found");
	}

	@Test
	void userIsInSingleRoleAttribute() throws Exception {
		// Given
		when(securityHandler.isUserInRole(any(String.class))).thenReturn(true);

		setNotInRoleForward();
		pipe.setRole(ROLE);
		pipe.configure();

		// When
		PipeRunResult prr = doPipe(pipe, "", session);

		// Expect
		assertEquals(PipeForward.SUCCESS_FORWARD_NAME, prr.getPipeForward().getName());
	}

	@Test
	void userIsInSingleRoleInput() throws Exception {
		// Given
		when(securityHandler.isUserInRole(any(String.class))).thenReturn(true);

		setNotInRoleForward();
		pipe.configure();

		// When
		PipeRunResult prr = doPipe(pipe, ROLE, session);

		// Expect
		assertEquals(PipeForward.SUCCESS_FORWARD_NAME, prr.getPipeForward().getName());
	}

	@Test
	void userIsNotInSingleRoleAttribute() throws Exception {
		// Given
		when(securityHandler.isUserInRole(any(String.class))).thenReturn(false);

		setNotInRoleForward();
		pipe.setRole(ROLE);
		pipe.configure();

		// When
		PipeRunResult prr = doPipe(pipe, "", session);

		// Expect
		assertEquals(NOT_IN_ROLE_FORWARD_NAME, prr.getPipeForward().getName());
	}

	@Test
	void userIsNotInSingleRoleInput() throws Exception {
		// Given
		when(securityHandler.isUserInRole(any(String.class))).thenReturn(false);

		setNotInRoleForward();
		pipe.configure();

		// When
		PipeRunResult prr = doPipe(pipe, ROLE, session);

		// Expect
		assertEquals(NOT_IN_ROLE_FORWARD_NAME, prr.getPipeForward().getName());
	}

	@Test
	void userIsInMultiRoleInput() throws Exception {
		// Given
		when(securityHandler.isUserInRole(ROLE)).thenReturn(false);
		when(securityHandler.isUserInRole(ROLE2)).thenReturn(true);
		when(securityHandler.isUserInRole(ROLE3)).thenReturn(true);

		setNotInRoleForward();
		pipe.configure();

		// When
		PipeRunResult prr = doPipe(pipe, ROLES, session);

		// Expect
		assertEquals(PipeForward.SUCCESS_FORWARD_NAME, prr.getPipeForward().getName());
	}

	@Test
	void userIsInMultiRoleInputWithForwards() throws Exception {
		// Given
		when(securityHandler.isUserInRole(ROLE)).thenReturn(false);
		when(securityHandler.isUserInRole(ROLE2)).thenReturn(true);
		when(securityHandler.isUserInRole(ROLE3)).thenReturn(true);

		setNotInRoleForward();
		pipe.addForward(new PipeForward(ROLE, ""));
		pipe.addForward(new PipeForward(ROLE2, ""));
		pipe.addForward(new PipeForward(ROLE3, ""));
		pipe.configure();

		// When
		PipeRunResult prr = doPipe(pipe, ROLES, session);

		// Expect
		assertEquals(ROLE2, prr.getPipeForward().getName());
	}

	@Test
	void userIsInMultiRoleAttribute() throws Exception {
		// Given
		when(securityHandler.isUserInRole(ROLE)).thenReturn(false);
		when(securityHandler.isUserInRole(ROLE2)).thenReturn(true);
		when(securityHandler.isUserInRole(ROLE3)).thenReturn(true);

		setNotInRoleForward();
		pipe.setRole(ROLES);
		pipe.configure();

		// When
		PipeRunResult prr = doPipe(pipe, "", session);

		// Expect
		assertEquals(PipeForward.SUCCESS_FORWARD_NAME, prr.getPipeForward().getName());
	}

	@Test
	void userIsInMultiRoleAttributeWithForwards() throws Exception {
		// Given
		when(securityHandler.isUserInRole(ROLE)).thenReturn(false);
		when(securityHandler.isUserInRole(ROLE2)).thenReturn(true);
		when(securityHandler.isUserInRole(ROLE3)).thenReturn(true);

		setNotInRoleForward();
		pipe.addForward(new PipeForward(ROLE, ""));
		pipe.addForward(new PipeForward(ROLE2, ""));
		pipe.addForward(new PipeForward(ROLE3, ""));
		pipe.setRole(ROLES);
		pipe.configure();

		// When
		PipeRunResult prr = doPipe(pipe, "", session);

		// Expect
		assertEquals(ROLE2, prr.getPipeForward().getName());
	}

	@Test
	void userIsNotMultiRoleInput() throws Exception {
		// Given
		when(securityHandler.isUserInRole(ROLE)).thenReturn(false);
		when(securityHandler.isUserInRole(ROLE2)).thenReturn(false);
		when(securityHandler.isUserInRole(ROLE3)).thenReturn(false);

		setNotInRoleForward();
		pipe.configure();

		// When
		PipeRunResult prr = doPipe(pipe, ROLES, session);

		// Expect
		assertEquals(NOT_IN_ROLE_FORWARD_NAME, prr.getPipeForward().getName());
	}

	@Test
	void userIsNotMultiRoleInputWithForwards() throws Exception {
		// Given
		when(securityHandler.isUserInRole(ROLE)).thenReturn(false);
		when(securityHandler.isUserInRole(ROLE2)).thenReturn(false);
		when(securityHandler.isUserInRole(ROLE3)).thenReturn(false);

		setNotInRoleForward();
		pipe.addForward(new PipeForward(ROLE, ""));
		pipe.addForward(new PipeForward(ROLE2, ""));
		pipe.addForward(new PipeForward(ROLE3, ""));
		pipe.configure();

		// When
		PipeRunResult prr = doPipe(pipe, ROLES, session);

		// Expect
		assertEquals(NOT_IN_ROLE_FORWARD_NAME, prr.getPipeForward().getName());
	}

	@Test
	void userIsNotMultiRoleAttribute() throws Exception {
		// Given
		when(securityHandler.isUserInRole(ROLE)).thenReturn(false);
		when(securityHandler.isUserInRole(ROLE2)).thenReturn(false);
		when(securityHandler.isUserInRole(ROLE3)).thenReturn(false);

		setNotInRoleForward();
		pipe.setRole(ROLES);
		pipe.configure();

		// When
		PipeRunResult prr = doPipe(pipe, "", session);

		// Expect
		assertEquals(NOT_IN_ROLE_FORWARD_NAME, prr.getPipeForward().getName());
	}

	@Test
	void userIsNotMultiRoleAttributeWithForwards() throws Exception {
		// Given
		when(securityHandler.isUserInRole(ROLE)).thenReturn(false);
		when(securityHandler.isUserInRole(ROLE2)).thenReturn(false);
		when(securityHandler.isUserInRole(ROLE3)).thenReturn(false);

		setNotInRoleForward();
		pipe.addForward(new PipeForward(ROLE, ""));
		pipe.addForward(new PipeForward(ROLE2, ""));
		pipe.addForward(new PipeForward(ROLE3, ""));
		pipe.setRole(ROLES);
		pipe.configure();

		// When
		PipeRunResult prr = doPipe(pipe, "", session);

		// Expect
		assertEquals(NOT_IN_ROLE_FORWARD_NAME, prr.getPipeForward().getName());
	}

	protected void setNotInRoleForward() throws ConfigurationException {
		PipeForward notInRole = new PipeForward(NOT_IN_ROLE_FORWARD_NAME, NOT_IN_ROLE_FORWARD_PATH);
		pipe.addForward(notInRole);
	}


}
