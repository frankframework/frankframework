package org.frankframework.ldap;

import static org.frankframework.testutil.MatchUtils.assertXmlEquals;
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
		return spy(new LdapFindGroupMembershipsPipe());
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
		pipe.setMaxRecursion(3);
		pipe.setRecursionFilter("dummy");

		PipeRunResult result = pipe.doPipeWithException(new Message("searchedDN"), new PipeLineSession());

		final String expectedResult = """
				<ldap>
					<entryName>searchedDN</entryName>
					<attributes>
						<attribute attrID="memberOf">item1</attribute>
						<attribute attrID="memberOf">item2</attribute>
						<attribute attrID="memberOf">item3</attribute>
					</attributes>
				</ldap>""";
		assertXmlEquals(expectedResult, result.getResult().asString());
	}

}
