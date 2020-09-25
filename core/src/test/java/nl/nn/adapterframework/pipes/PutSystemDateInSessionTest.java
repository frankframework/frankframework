package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.util.DateUtils;

public class PutSystemDateInSessionTest extends PipeTestBase<PutSystemDateInSession>{

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	private IPipeLineSession session = new PipeLineSessionBase();

	@Override
	public PutSystemDateInSession createPipe() {
		return new PutSystemDateInSession();
	}

	@Test
	public void testConfigureNullDateFormat() throws Exception {
		expectedEx.expectMessage("has a null value for dateFormat");
		pipe.setDateFormat(null);
		configureAndStartPipe();
	}

	@Test
	public void testConfigureNullSessionKey() throws Exception {
		expectedEx.expectMessage("has a null value for sessionKey");
		pipe.setSessionKey(null);
		configureAndStartPipe();
	}

	@Test
	public void testFixedDateTimeFormatInvalid() throws Exception {
		expectedEx.expectMessage("cannot parse fixed date");
		configureAndStartPipe();
		pipe.setReturnFixedDate(true);
		pipe.setDateFormat(PutSystemDateInSession.FORMAT_FIXEDDATETIME);
		pipe.setSessionKey("first");
		session.put("stub4testtool.fixeddate", "22331");
		doPipe(pipe, "dummy", session);
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

		SimpleDateFormat parser = new SimpleDateFormat(PutSystemDateInSession.FORMAT_FIXEDDATETIME);
		SimpleDateFormat formatter = new SimpleDateFormat(DateUtils.fullIsoFormat);

		Date date = parser.parse(fixedDate);
		assertEquals(formatter.format(date), result);

		pipe.setSessionKey("second");
		doPipe(pipe, "dummy", session);
		String secondResult = (String) session.get("second");

		Date first = formatter.parse(result);
		Date second = formatter.parse(secondResult);

		long timeDifference = second.getTime()-first.getTime();
		assertEquals(timeDifference, 0);
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

		SimpleDateFormat formatter = new SimpleDateFormat(PutSystemDateInSession.FORMAT_FIXEDDATETIME);
		Date first = formatter.parse(result);
		Date second = formatter.parse(secondResult);

		long timeDifference = second.getTime()-first.getTime();
		assertEquals(result, PutSystemDateInSession.FIXEDDATETIME);
		assertEquals(timeDifference, 0);
	}

	@Test
	public void testConfigureIsReturnFixedDatewithoutStub() throws Exception {
		expectedEx.expectMessage("returnFixedDate only allowed in stub mode");
		pipe.setSessionKey("dummy");
		pipe.setReturnFixedDate(true);
		configureAndStartPipe();
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
		assertFalse("Time stamp difference cannot be bigger than 1 s", timeStampInMillis - new Long(timeStampInMillisFromSessionKey)>1000);
	}

	@Test
	public void testConfigureInvalidDateFormat() throws Exception {
		expectedEx.expectMessage("has an illegal value for dateFormat");
		pipe.setDateFormat("test");
		pipe.setSessionKey("dummy");
		configureAndStartPipe();
	}

	@Test
	public void testSleepWhenEqualsToPrevious() throws Exception {
		long sleep = 1000;
		pipe.setSleepWhenEqualToPrevious(sleep);
		configureAndStartPipe();

		pipe.setSessionKey("first");
		doPipe(pipe, "dummy", session);
		String result = (String) session.get("first");

		pipe.setSessionKey("second");
		doPipe(pipe, "dummy", session);
		String secondResult = (String) session.get("second");

		SimpleDateFormat format = new SimpleDateFormat(DateUtils.fullIsoFormat);
		Date first = format.parse(result);
		Date second = format.parse(secondResult);

		long timeDifference = second.getTime()-first.getTime();

		assertFalse("Date difference cannot be bigger than "+sleep, timeDifference > sleep);
	}

}
