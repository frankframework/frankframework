package org.frankframework.management.web.socket;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.util.HtmlUtils;

@Controller
public class GreetingController {

	@MessageMapping("/hello")
	@SendTo("/event/greetings")
	public Greeting greeting(HelloMessage message) {
		return new Greeting("Hello, " + HtmlUtils.htmlEscape(message.name()) + "!");
	}

	public record HelloMessage(String name) {}

	public record Greeting(String content) {}
	
}
