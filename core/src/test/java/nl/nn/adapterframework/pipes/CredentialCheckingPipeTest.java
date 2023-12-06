package nl.nn.adapterframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;


import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunException;

import org.junit.jupiter.api.Test;

public class CredentialCheckingPipeTest extends PipeTestBase<CredentialCheckingPipe> {

    @Override
    public CredentialCheckingPipe createPipe() {
        return new CredentialCheckingPipe();
    }

    @Test
    public void getterSetterAuthAlias() {
        String dummyString = "dummyString";
        pipe.setAuthAlias(dummyString);
        assertEquals(pipe.getAuthAlias(), dummyString);
    }

    @Test
    public void getterSetterTargetUserId() {
        String dummyString = "dummyString";
        pipe.setTargetUserid(dummyString);
        assertEquals(pipe.getTargetUserid(), dummyString);
    }

    @Test
    public void getterSetterTargetPassword() {
        String dummyString = "dummyString";
        pipe.setTargetPassword(dummyString);
        assertEquals(pipe.getTargetPassword(), dummyString);
    }

    @Test
    public void getterSetterDefaultPassword() {
        String dummyString = "dummyString";
        pipe.setDefaultPassword(dummyString);
        assertEquals(pipe.getDefaultPassword(), dummyString);
    }

    @Test
    public void getterSetterDefaultUserId() {
        String dummyString = "dummyString";
        pipe.setDefaultUserid(dummyString);
        assertEquals(pipe.getDefaultUserid(), dummyString);
    }

    @Test
    public void testNoTargetUserId() throws ConfigurationException {
        pipe.setTargetPassword("dummyPassword");
        pipe.configure();
    }

    @Test
    public void testNoTargetUserPassword() throws ConfigurationException {
        pipe.setTargetUserid("dummyId");
		assertThrows(ConfigurationException.class, () -> {
			pipe.configure();
		});
    }

    @Test
    public void testExistingTarget() throws ConfigurationException {
        try {
            pipe.setTargetPassword("dummyPassword");
            pipe.setTargetUserid("dummyId");
            pipe.configure();
        } catch (Exception e) {
            fail("This should not throw an exception");
        }

    }

    @Test
    public void testNonExisitingTarget() throws ConfigurationException {
		assertThrows(ConfigurationException.class, () -> {
			pipe.configure();
		});
    }

    @Test
    public void testWrongUserName() throws PipeRunException {
        String expectedResult = "username does not match target";
        String result = resultLogIn("dummyUserPassword", "otherUserId", "dummyUserPassword","dummyUserId");
        assertEquals(expectedResult, result);
    }

    @Test
    public void testWrongPassword() throws PipeRunException {
        String expectedResult = "password does not match target";
        String result = resultLogIn("otherUserPassword", "dummyUserId", "dummyUserPassword","dummyUserId");
        assertEquals(expectedResult, result);
    }

    @Test
    public void testWrongUserNameAndPassword() throws PipeRunException {
        String expectedResult = "username does not match targetpassword does not match target";
        String result = resultLogIn("otherUserPassword", "otherUserId", "dummyUserPassword","dummyUserId");
        assertEquals(expectedResult, result);
    }

    @Test
    public void testRightUserNameAndPassword() throws PipeRunException {
        String expectedResult = "OK";
        String result = resultLogIn("dummyUserPassword", "dummyUserId", "dummyUserPassword","dummyUserId");
        assertEquals(expectedResult, result);
    }

    public String resultLogIn(String defaultPassword, String defaultId, String targetPassword, String targetId) throws PipeRunException {
        Object input = "dummyInput";
        pipe.setTargetPassword(targetPassword);
        pipe.setDefaultPassword(defaultPassword);

        pipe.setTargetUserid(targetId);
        pipe.setDefaultUserid(defaultId);

        try {
			return doPipe(pipe, input, session).getResult().asString();
		} catch (IOException e) {
			throw new PipeRunException(pipe, "cannot convert results", e);
		}
    }


}
