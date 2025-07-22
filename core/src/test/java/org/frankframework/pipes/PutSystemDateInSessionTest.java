package org.frankframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunException;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.TimeProvider;

public class PutSystemDateInSessionTest extends PipeTestBase<PutSystemDateInSession>{

	@Override
	public PutSystemDateInSession createPipe() {
		return new PutSystemDateInSession();
	}

	@Test
	public void testConfigureNullDateFormat() {
		pipe.setDateFormat(null);

		ConfigurationException e = assertThrows(ConfigurationException.class, this::configureAndStartPipe);
		assertThat(e.getMessage(), Matchers.endsWith("has a null value for dateFormat"));
	}

	@Test
	public void testConfigureNullSessionKey() {
		pipe.setSessionKey(null);

		ConfigurationException e = assertThrows(ConfigurationException.class, this::configureAndStartPipe);
		assertThat(e.getMessage(), Matchers.endsWith("has a null value for sessionKey"));
	}

	@Test
	public void testFixedDateTimeFormatInvalid() throws Exception {
		configureAndStartPipe();
		pipe.setReturnFixedDate(true);
		pipe.setDateFormat(DateFormatUtils.FORMAT_DATETIME_GENERIC);
		pipe.setSessionKey("first");
		session.put("stub4testtool.fixeddate", "22331");

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, "dummy", session));
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

		DateTimeFormatter parser = DateFormatUtils.GENERIC_DATETIME_FORMATTER;
		DateTimeFormatter formatter = DateFormatUtils.FULL_ISO_FORMATTER;

		ZonedDateTime date = ZonedDateTime.from(parser.parse(fixedDate));
		assertEquals(formatter.format(date), result);

		pipe.setSessionKey("second");
		doPipe(pipe, "dummy", session);
		String secondResult = (String) session.get("second");

		Instant first = Instant.from(formatter.parse(result));
		Instant second = Instant.from(formatter.parse(secondResult));

		long timeDifference = second.toEpochMilli()-first.toEpochMilli();
		assertEquals(0, timeDifference);
	}

	@Test
	public void testReturnFixedDate() throws Exception {
		pipe.setSessionKey("first");
		pipe.setDateFormat(DateFormatUtils.FORMAT_DATETIME_GENERIC);
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

		DateTimeFormatter formatter = DateFormatUtils.GENERIC_DATETIME_FORMATTER;
		Instant first = Instant.from(formatter.parse(result));
		Instant second = Instant.from(formatter.parse(secondResult));

		long timeDifference = second.toEpochMilli()-first.toEpochMilli();
		assertEquals(PutSystemDateInSession.FIXEDDATETIME, result);
		assertEquals(0, timeDifference);
	}

	@Test
	public void testConfigureIsReturnFixedDatewithoutStub() {
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
		long timeStampInMillis = TimeProvider.nowAsMillis();
		String timeStampInMillisFromSessionKey = (String) session.get("dummy");
		//Compare timestamp put in session key with the actual timestamp fail if it is bigger than 1 sec.
		assertFalse(timeStampInMillis - Long.parseLong(timeStampInMillisFromSessionKey) > 1000, "Time stamp difference cannot be bigger than 1 s");
	}

	@Test
	public void testConfigureInvalidDateFormat() {
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

		DateTimeFormatter format = DateFormatUtils.FULL_ISO_FORMATTER;
		Instant first = Instant.from(format.parse(result));
		Instant second = Instant.from(format.parse(secondResult));

		long timeDifference = second.toEpochMilli()-first.toEpochMilli();

		assertEquals(1000L, timeDifference);
	}

}
