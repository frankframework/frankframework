package org.frankframework.ldap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.util.LinkedHashSet;

import javax.naming.NamingException;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.PipeTestBase;
import org.frankframework.stream.Message;

public class LdapFindGroupMembershipsPipeTest extends PipeTestBase<LdapFindGroupMembershipsPipe> {

	@Override
	public LdapFindGroupMembershipsPipe createPipe() throws ConfigurationException {
		var pipe = spy(new LdapFindGroupMembershipsPipe());
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
