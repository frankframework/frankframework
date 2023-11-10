package nl.nn.adapterframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import java.util.Date;

import org.hamcrest.Matchers;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.util.DateUtils;

public class PutSystemDateInSessionTest extends PipeTestBase<PutSystemDateInSession> {

	@Override
	public PutSystemDateInSession createPipe() {
		return new PutSystemDateInSession();
	}

	@Test
	public void testConfigureNullDateFormat() throws Exception {
		pipe.setDateFormat(null);

		ConfigurationException e = assertThrows(ConfigurationException.class, this::configureAndStartPipe);
		assertThat(e.getMessage(), Matchers.endsWith("has a null value for dateFormat"));
	}

	@Test
	public void testConfigureNullSessionKey() throws Exception {
		pipe.setSessionKey(null);

		ConfigurationException e = assertThrows(ConfigurationException.class, this::configureAndStartPipe);
		assertThat(e.getMessage(), Matchers.endsWith("has a null value for sessionKey"));
	}

	@Test
	public void testFixedDateTimeFormatInvalid() throws Exception {
		configureAndStartPipe();
		pipe.setReturnFixedDate(true);
		pipe.setDateFormat(PutSystemDateInSession.FORMAT_FIXEDDATETIME);
		pipe.setSessionKey("first");
		session.put("stub4testtool.fixeddate", "22331");

		PipeRunException e = assertThrows(PipeRunException.class, () -> doPipe(pipe, "dummy", session));
		assertThat(e.getMessage(), Matchers.containsString("cannot parse fixed date"));
	}

	@Test
	public void testReturnFixedDateFromSessionKey() throws Exception {
		configureAndStartPipe();
		// TODO : this field must be set before configure
		// but setting stub mod from AppConstants does not work
		// because of not being singleton
		pipe.setReturnFixedDate(true);
		pipe.setSessionKey("first");

		String fixedDate = "2011-12-17 09:30:47";
		session.put("stub4testtool.fixeddate", fixedDate);

		doPipe(pipe, "dummy", session);
		String result = (String) session.get("first");

		Date date = DateUtils.parseToDate(fixedDate, PutSystemDateInSession.FORMAT_FIXEDDATETIME);
		assertEquals(DateUtils.format(date, DateUtils.fullIsoFormatter), result);

		pipe.setSessionKey("second");
		doPipe(pipe, "dummy", session);
		String secondResult = (String) session.get("second");

		Date first = DateUtils.parseToDate(result, DateUtils.fullIsoFormatter);
		Date second = DateUtils.parseToDate(secondResult, DateUtils.fullIsoFormatter);

		long timeDifference = second.getTime() - first.getTime();
		assertEquals(0, timeDifference);
	}

	@Test
	public void testReturnFixedDate() throws Exception {
		pipe.setSessionKey("first");
		pipe.setDateFormat(PutSystemDateInSession.FORMAT_FIXEDDATETIME);
		configureAndStartPipe();
		// TODO : this field must be set before configure
		// but setting stub mod from AppConstants does not work
		// because of not being singleton
		pipe.setReturnFixedDate(true);

		doPipe(pipe, "dummy", session);
		String result = (String) session.get("first");

		pipe.setSessionKey("second");
		doPipe(pipe, "dummy", session);
		String secondResult = (String) session.get("second");

		Date first = DateUtils.parseToDate(result, PutSystemDateInSession.FORMAT_FIXEDDATETIME);
		Date second = DateUtils.parseToDate(secondResult, PutSystemDateInSession.FORMAT_FIXEDDATETIME);

		long timeDifference = second.getTime() - first.getTime();
		assertEquals(PutSystemDateInSession.FIXEDDATETIME, result);
		assertEquals(0, timeDifference);
	}

	@Test
	public void testConfigureIsReturnFixedDatewithoutStub() throws Exception {
		pipe.setSessionKey("dummy");
		pipe.setReturnFixedDate(true);

		ConfigurationException e = assertThrows(ConfigurationException.class, this::configureAndStartPipe);
		assertThat(e.getMessage(), Matchers.endsWith("returnFixedDate only allowed in stub mode"));
	}

	@Test
	public void testGetTimeStampInMillis() throws Exception {
		pipe.setSessionKey("dummy");
		pipe.setGetCurrentTimeStampInMillis(true);
		configureAndStartPipe();
		doPipe(pipe, "dummy", session);
		long timeStampInMillis = new Date().getTime();
		String timeStampInMillisFromSessionKey = (String) session.get("dummy");
		//Compare timestamp put in session key with the actual timestamp fail if it is bigger than 1 sec.
		assertFalse("Time stamp difference cannot be bigger than 1 s", timeStampInMillis - new Long(timeStampInMillisFromSessionKey) > 1000);
	}

	@Test
	public void testConfigureInvalidDateFormat() throws Exception {
		pipe.setDateFormat("test");
		pipe.setSessionKey("dummy");

		ConfigurationException e = assertThrows(ConfigurationException.class, this::configureAndStartPipe);
		assertThat(e.getMessage(), Matchers.containsString("has an illegal value for dateFormat"));
	}

	@Test
	public void testSleepWhenEqualsToPrevious() throws Exception {
		long sleep = 100;
		pipe.setSleepWhenEqualToPrevious(sleep);
		configureAndStartPipe();

		pipe.setSessionKey("first");
		doPipe(pipe, "dummy", session);
		String result = (String) session.get("first");

		pipe.setSessionKey("second");
		doPipe(pipe, "dummy", session);
		String secondResult = (String) session.get("second");

		Date first = DateUtils.parseToDate(result, DateUtils.fullIsoFormatter);
		Date second = DateUtils.parseToDate(secondResult, DateUtils.fullIsoFormatter);

		long timeDifference = second.getTime() - first.getTime();

		assertEquals("Timestamps should be different", 1000L, timeDifference);
	}

}
