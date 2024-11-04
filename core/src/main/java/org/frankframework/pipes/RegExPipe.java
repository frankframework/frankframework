/*
   Copyright 2024 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package org.frankframework.pipes;

import lombok.Getter;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.Forward;
import org.frankframework.doc.Mandatory;
import org.frankframework.stream.Message;
import org.frankframework.util.XmlBuilder;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Tries to match the input against the provided regex.
 *
 * <pre>{@code
 * 	<RegExPipe name="regExPipe" regex="^(.*?)(name!)$" flags="CASE_INSENSITIVE">
 * 		<Forward name="then" path="EXIT" />
 * 		<Forward name="else" path="EXIT" />
 * 	</RegExPipe>
 * }</pre>
 *
 * <p>Input:</p>
 * <pre>{@code
 * 	Hello name!
 * }</pre>
 *
 * <p>Output:</p>
 * <pre>{@code
 *  <matches>
 * 		<match index="1" value="Hello name!">
 * 			<group index="1">Hello </group>
 * 			<group index="2">name!</group>
 * 		</match>
 *  </matches>
 * }</pre>
 *
 * @ff.tip https://regex101.com can be used to quickly create and debug regex expressions.
 *
 */
@Forward(name = RegExPipe.THEN_FORWARD, description = "When a match is found.")
@Forward(name = RegExPipe.ELSE_FORWARD, description = "When no match is found.")
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.ROUTER)
public class RegExPipe extends AbstractPipe {

	protected final static String THEN_FORWARD = "then";
	protected final static String ELSE_FORWARD = "else";

	public enum RegExFlag {
		CANON_EQ(Pattern.CANON_EQ),
		CASE_INSENSITIVE(Pattern.CASE_INSENSITIVE),
		COMMENTS(Pattern.COMMENTS),
		DOT_ALL(Pattern.DOTALL),
		LITERAL(Pattern.LITERAL),
		MULTILINE(Pattern.MULTILINE),
		UNICODE_CASE(Pattern.UNICODE_CASE),
		UNICODE_CHARACTER_CLASS(Pattern.UNICODE_CHARACTER_CLASS),
		UNIX_LINES(Pattern.UNIX_LINES);

		private final @Getter int flag;

		RegExFlag(int flag) {
			this.flag = flag;
		}
	}

	private String regex = "";
	private List<RegExFlag> flags = List.of();

	private Pattern pattern;

	private PipeForward thenForward;
	private PipeForward elseForward;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		final int flags = this.flags.stream()
				.map(RegExFlag::getFlag)
				.reduce(0, (a, b) -> a | b);

		try {
			this.pattern = Pattern.compile(this.regex, flags);
		} catch (PatternSyntaxException e) {
			throw new ConfigurationException("Pattern of regex is incorrect", e);
		}

		this.thenForward = findForward(THEN_FORWARD);
		this.elseForward = findForward(ELSE_FORWARD);

		if (this.thenForward == null) {
			throw new ConfigurationException("has no forward with name [" + THEN_FORWARD + "]");
		}

		if (this.elseForward == null) {
			throw new ConfigurationException("has no forward with name [" + ELSE_FORWARD + "]");
		}
	}

	private PipeRunResult noMatchFound() {
		return new PipeRunResult(elseForward, null);
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		if (Message.isEmpty(message)) {
			return noMatchFound();
		}

		String sInput;
		try {
			sInput = message.asString();
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot open stream", e);
		}
		if (StringUtils.isEmpty(sInput)) {
			return noMatchFound();
		}

		Matcher matcher = this.pattern.matcher(sInput);
		XmlBuilder matches = new XmlBuilder("matches");

		int index = 1;
		boolean foundMatch = false;
		while (matcher.find()) {
			foundMatch = true;
			matches.addSubElement(matchToXml(matcher, index));
			index += 1;
		}

		if (!foundMatch) {
			return noMatchFound();
		}

		Message result = matches.asMessage();
		return new PipeRunResult(thenForward, result);
	}

	private XmlBuilder matchToXml(Matcher matcher, int index) {
		int groups = matcher.groupCount();

		XmlBuilder match = new XmlBuilder("match");
		match.addAttribute("index", index);
		match.addAttribute("value", matcher.group());

		for (int i = 1; i <= groups; i++) {
			XmlBuilder group = new XmlBuilder("group");
			group.addAttribute("index", i);
			group.setValue(matcher.group(i));

			match.addSubElement(group);
		}

		return match;
	}

	/**
	 * The regex expression to match against the input.
	 */
	@Mandatory
	public void setRegex(String regex) {
		this.regex = regex;
	}

	/**
	 * Comma seperated list of flags, which changes the behavior of the regex expression.
	 */
	public void setFlags(RegExFlag... flags) {
		this.flags = List.of(flags);
	}

}
