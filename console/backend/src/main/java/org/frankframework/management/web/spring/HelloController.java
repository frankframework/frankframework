package org.frankframework.management.web.spring;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HelloController {

	@GetMapping("/hello")
	public String handle(@RequestParam(required = false) String name) {
		if(name == null) {
			return "Hello, World!";
		}
		return "Hello, " + name + "!";
	}

	@GetMapping(value = "/test", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<Map<String,String>> test() {
		Map<String, String> response = new HashMap<>();
		response.put("key1","value1");
		response.put("key2","value2");

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.add("x-key", "value");

		return ResponseEntity.status(200).headers(entityHeaders).body(response);
	}

	@GetMapping("/srb")
	public ResponseEntity<StreamingResponseBody> handleRbe() {
		StreamingResponseBody stream = out -> {
			String msg = "/srb" + " @ " + new Date();
			out.write(msg.getBytes());
		};
		return new ResponseEntity<>(stream, HttpStatus.OK);
	}

}
