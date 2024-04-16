package org.frankframework.management.web.spring;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

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
	public Map<String,String> test() {
		Map<String, String> response = new HashMap<>();
		response.put("key1","value1");
		response.put("key2","value2");
		return response;
	}

}
