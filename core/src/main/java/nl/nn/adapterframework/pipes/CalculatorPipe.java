package nl.nn.adapterframework.pipes;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;

public class CalculatorPipe extends FixedForwardPipe {
	
	private static final Pattern INVALID_CHARACTERS = Pattern.compile("([a-zA-Z]+)");

	@Override
	public PipeRunResult doPipe(Message message, IPipeLineSession session) throws PipeRunException {
		try {
			String script = message.asString();
			validate(script);
			Message newMessage = calculateMessage(script);
			return new PipeRunResult(getForward(), newMessage);
		} catch (ScriptException | IOException e) {
			throw new PipeRunException(this, "Could not calculate", e);
		}
	}

	private void validate(String script) throws PipeRunException {
		Matcher matcher = INVALID_CHARACTERS.matcher(script);
		if (!matcher.find()) {
			return;
		}
		
		String invalidCharacter = matcher.group(1);
		String errorMessage = MessageFormat.format("{0} is an invalid character", invalidCharacter);
		throw new PipeRunException(this, errorMessage);
	}

	private Message calculateMessage(String assignment) throws IOException, ScriptException {
		// https://stackoverflow.com/questions/24550923/simple-library-for-calculator-in-java-android
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByName("JavaScript");

		String result = "" + engine.eval(assignment);
		String assignmentResult = MessageFormat.format("{0} = {1}", assignment, result);
		return Message.asMessage(assignmentResult);
	}
}
