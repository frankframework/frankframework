package org.frankframework.ldap;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.PipeTestBase;

import org.frankframework.stream.Message;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.naming.NamingException;

import java.io.IOException;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@Tag("mytag")
public class LdapFindGroupMembershipsPipeTest extends PipeTestBase<LdapFindGroupMembershipsPipe> {

	private static final String SUCCESS_FORWARD = "success";

	@Override
	public LdapFindGroupMembershipsPipe createPipe() throws ConfigurationException {
		var pipe = spy(new LdapFindGroupMembershipsPipe());
		pipe.registerForward(new PipeForward(SUCCESS_FORWARD, SUCCESS_FORWARD));
		return pipe;
	}

	@Test
	public void testDoPipe() throws NamingException, PipeRunException, IOException {
		var set = new LinkedHashSet<String>();
		set.add("item1");
		set.add("item2");
		set.add("item3");

		doReturn(set).when(pipe).searchRecursivelyViaAttributes(any());

		pipe.setLdapProviderURL("url");
		pipe.setRecursiveSearch(true);

		PipeRunResult result = pipe.doPipeWithException(new Message("searchedDN"), new PipeLineSession());

		final String expectedResult = "<ldap>\n" +
				"\t<entryName>searchedDN</entryName>\n" +
				"\t<attributes>\n" +
				"\t\t<attribute attrID=\"memberOf\">item1</attribute>\n" +
				"\t\t<attribute attrID=\"memberOf\">item2</attribute>\n" +
				"\t\t<attribute attrID=\"memberOf\">item3</attribute>\n" +
				"\t</attributes>\n" +
				"</ldap>";
		assertEquals(expectedResult, result.getResult().asString());
	}

}
