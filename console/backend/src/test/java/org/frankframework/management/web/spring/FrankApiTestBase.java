package org.frankframework.management.web.spring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebAppConfiguration
@ExtendWith(SpringExtension.class)
public abstract class FrankApiTestBase {
	protected MockMvc mockMvc;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@BeforeEach
	public void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	protected String asJsonString(final Object obj) {
		try {
			return new ObjectMapper().writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected MockMultipartFile createMockMultipartFile(final String name, final String originalFilename, final byte[] content) {
		return new MockMultipartFile(name, originalFilename, MediaType.MULTIPART_FORM_DATA_VALUE, content);
	}

	protected void testBasicRequest(String url, String topic, String action) throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get(url))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("topic").value(topic))
				.andExpect(MockMvcResultMatchers.jsonPath("action").value(action));
	}
}

/*public abstract class FrankApiTestBase<M extends FrankApiBase> extends Mockito {

	public static final String STUBBED_SPRING_BUS_CONFIGURATION = "stubbedBusApplicationContext.xml";

	private final Logger log = LogManager.getLogger(org.frankframework.management.web.FrankApiTestBase.class);
	public enum IbisRole {
		IbisWebService, IbisObserver, IbisDataAdmin, IbisAdmin, IbisTester;
	}

	protected FrankApiTestBase.MockDispatcher dispatcher = new FrankApiTestBase.MockDispatcher();
	protected M springMvcResource;
	private static ApplicationContext applicationContext;

	public abstract M createSpringMvcResource();

	@BeforeEach
	public void setUp() throws Exception {
		M resource = createSpringMvcResource();
		springMvcResource = spy(resource);

		springMvcResource.setApplicationContext(getApplicationContext());
		springMvcResource.servletRequest = new MockHttpServletRequest();

	}

	private final ApplicationContext getApplicationContext() {
		if(FrankApiTestBase.applicationContext == null) {
			ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
			applicationContext.setConfigLocation(STUBBED_SPRING_BUS_CONFIGURATION);
			applicationContext.setDisplayName("STUB [stubbed-frank-management-bus]");
			applicationContext.refresh();

			FrankApiTestBase.applicationContext = applicationContext;
		}

		return FrankApiTestBase.applicationContext;
	}

	public class MockDispatcher {
		public Map<String, Method> mvcRequests = new HashMap<>();

		*//**
		 * scan all methods in the Spring MVC class
		 *//*
		public void register(M springMvcResource) {
			Method[] classMethods = springMvcResource.getClass().getDeclaredMethods();
			RequestMapping requestMappingAnnotation = AnnotationUtils.findAnnotation(springMvcResource.getClass(), RequestMapping.class);
			final String basePath = requestMappingAnnotation != null ? requestMappingAnnotation.value()[0] : "/";
			for(Method classMethod : classMethods) {
				int modifiers = classMethod.getModifiers();
				if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
					RequestMapping methodRequestMapping = AnnotationUtils.findAnnotation(classMethod, RequestMapping.class);
					if(methodRequestMapping != null) {
						String compiledPath = getPath(basePath, methodRequestMapping.value()[0]);
						String mvcResourceKey = compileKey(methodRequestMapping.method()[0].toString(), compiledPath);

						log.info("adding new Spring MVC resource key [{}] method [{}] path [{}]", ()->mvcResourceKey, classMethod::getName, ()->compiledPath);
						mvcRequests.put(mvcResourceKey, classMethod);
					}
					//Ignore security for now
				}
			}
		}

		// The basepath is usually a '/', but path may also start with a slash.
		// Ensure a valid path is returned.
		private String getPath(String basePath, String path) {
			StringBuilder pathToUse = new StringBuilder();
			if(!basePath.startsWith("/")) {
				pathToUse.append("/");
			}
			pathToUse.append(basePath);
			pathToUse.append( basePath.endsWith("/") && path.startsWith("/") ? path.substring(1) : path);
			return pathToUse.toString();
		}

		*//**
		 * uppercase the HTTP method and combine with the resource path
		 *//*
		public String compileKey(String method, String path) {
			String url = getPath(path);
			return "%S:%s".formatted(method, url);
		}

		*//**
		 * Makes sure the url begins with a slash and strips all QueryParams off the URL
		 *//*
		private String getPath(String path) {
			String url = path;
			if(!url.startsWith("/")) {
				url = "/"+url;
			}
			int questionMark = url.indexOf("?");
			if(questionMark != -1) {
				url = url.substring(0, questionMark);
			}
			return url;
		}

		*//**
		 * Tries to find the correct Spring MVC method (with optional path parameter)
		 * @param mvcResourceKey unique key to identify all Spring MVC resources in the given resource class
		 * @return Spring MVC method or `null` when no resource is found
		 *//*
		private Method findRequest(String mvcResourceKey) {
			String[] uriSegments = mvcResourceKey.split("/");

			for (Iterator<String> it = mvcRequests.keySet().iterator(); it.hasNext();) {
				String pattern = it.next();
				String[] patternSegments = pattern.split("/");

				if (patternSegments.length != uriSegments.length || patternSegments.length < uriSegments.length) {
					continue;
				}

				int matches = 0;
				for (int i = 0; i < uriSegments.length; i++) {
					if(patternSegments[i].equals(uriSegments[i]) || (patternSegments[i].startsWith("{") && patternSegments[i].endsWith("}"))) {
						matches++;
					} else {
						continue;
					}
				}
				if(matches == uriSegments.length) {
					return mvcRequests.get(pattern);
				}
			}
			return null;
		}

		public ResponseEntity<?> dispatchRequest(String httpMethod, String url) {
			return dispatchRequest(httpMethod, url, null);
		}

		public ResponseEntity<?> dispatchRequest(String httpMethod, String url, Object jsonOrFormdata) {
			return dispatchRequest(httpMethod, url, jsonOrFormdata, null, null);
		}

		public ResponseEntity<?> dispatchRequest(String httpMethod, String url, Object jsonOrFormdata, FrankApiTestBase.IbisRole role) {
			return dispatchRequest(httpMethod, url, jsonOrFormdata, role, null);
		}

		*//**
		 * Dispatches the mocked request.
		 * @param httpMethod GET PUT POST DELETE
		 * @param url the relative path of the FF! API
		 * @param jsonOrFormdata when using PUT/POST requests, a json string of formdata object
		 * @param role IbisRole if you want to test authorization as well
		 * @param headers Map of header parameters where the key is the header name and the value is the header value
		 *//*
		public ResponseEntity<?> dispatchRequest(String httpMethod, String url, Object jsonOrFormdata, FrankApiTestBase.IbisRole role, Map<String, String> headers) {

			String mvcResourceKey = compileKey(httpMethod, url);
			log.info("trying to dispatch request to [{}]", mvcResourceKey);

			if("GET".equalsIgnoreCase(httpMethod) && jsonOrFormdata != null) {
				fail("can't use arguments on a GET request");
			}
			applyIbisRole(role);

			Method method = findRequest(mvcResourceKey);
			if(method == null) {
				fail("can't find resource ["+url+"] method ["+httpMethod+"]");
			}

			RequestMapping requestMappingAnnotation = AnnotationUtils.findAnnotation(method.getDeclaringClass(), RequestMapping.class);
			if(requestMappingAnnotation == null) {
				fail("can't find mapping annotation on resource ["+url+"] method ["+httpMethod+"]");
			}
			final String basePath = requestMappingAnnotation.value()[0]; //TODO: support multiple base paths
			final String methodPath = basePath + method.getAnnotation(RequestMapping.class).value()[0];//TODO same as above

			log.debug("found Spring MVC resource [{}]", ()->compileKey(httpMethod, methodPath));

			try {
				Parameter[] parameters = method.getParameters(); //find all parameters in the Spring MVC method, and try to populate them.
				Object[] methodArguments = new Object[parameters.length];

				for (int i = 0; i < parameters.length; i++) {
					Parameter parameter = parameters[i];

					boolean isPathParameter = parameter.isAnnotationPresent(PathVariable.class);
					boolean isQueryParameter = parameter.isAnnotationPresent(RequestParam.class);
					boolean isHeaderParameter = parameter.isAnnotationPresent(RequestHeader.class);
					boolean isFormPartParameter = parameter.isAnnotationPresent(RequestPart.class);

					if(isPathParameter) {
						String pathValue = findPathParameter(parameter, methodPath, url);

						Object value = ClassUtils.convertToType(parameter.getType(), pathValue);
						log.debug("setting method argument [{}] to value [{}]", i, value);
						methodArguments[i] = value;
					} else if(isQueryParameter) {
						Object value = findQueryParameter(parameter, url);
						log.debug("setting method argument [{}] to value [{}]", i, value);
						methodArguments[i] = value;
					} else if (isHeaderParameter) {
						Object value = findHeaderParameter(parameter, headers);
						log.debug("setting method argument [{}] to value [{}]", i, value);
						methodArguments[i] = value;
					} else if(isFormPartParameter) {
						Object value = findMultiPartParameter(parameter, jsonOrFormdata);
						log.debug("setting method argument [{}] to value [{}]", i, value);
						methodArguments[i] = value;
					} else {
						String[] consumes = requestMappingAnnotation.consumes();
						if(consumes.length == 0) {
							fail("found additional argument without consumes!");
						}
						if(jsonOrFormdata == null) {
							fail("found unset parameter ["+parameter+"] on method!");
						}

						String mediaType = consumes[0];
						if(mediaType.equals(MediaType.APPLICATION_JSON_VALUE)) { //We need to convert our input to json
							Object value = convertInputToParameterType(parameter, jsonOrFormdata);

							log.debug("setting method argument [{}] to value [{}] with type [{}]", i, value, value.getClass().getSimpleName());
							methodArguments[i] = value;
						*//*} else if(mediaType.equals(MediaType.MULTIPART_FORM_DATA_VALUE)){
							if(jsonOrFormdata instanceof List<?>) {
								@SuppressWarnings("unchecked")
								MultipartBody multipartBody = new MultipartBody((List<Attachment>) jsonOrFormdata);
								methodArguments[i] = multipartBody;
							}*//*
						} else {
							fail("mediaType ["+mediaType+"] not yet implemented");
						}
					}
				}

				validateIfAllArgumentsArePresent(methodArguments, parameters);

				ResponseEntity<?> originalResponse = (ResponseEntity<?>) method.invoke(springMvcResource, methodArguments);
				HttpHeaders meta = originalResponse.getHeaders();
				ResponseEntity.BodyBuilder response = ResponseEntity.status(originalResponse.getStatusCode());
				Object responseBody = originalResponse.getBody();

				String[] produces = requestMappingAnnotation.produces();
				if(produces.length > 0) {
					String mediaType = produces[0];
					meta.add(HttpHeaders.CONTENT_TYPE, mediaType);

					if(mediaType.equals(MediaType.APPLICATION_JSON_VALUE)) {
						ObjectMapper objectMapper = new ObjectMapper();
						responseBody = objectMapper.writeValueAsString(originalResponse.getBody());
					}
				}

				response.headers(meta);
				return response.body(responseBody);
			} catch (InvocationTargetException e) {
				//Directly throw AssertionError so JUnit can analyze the StackTrace
				//Directly throw ApiExceptions so they can be asserted in JUnit tests
				if(e.getCause() instanceof RuntimeException) {
					throw (RuntimeException) e.getCause();
				}
				log.fatal("unexpected InvocationTargetException, failing test", e);
				return fail("error dispatching request ["+mvcResourceKey+"] " + e.getMessage());
			} catch (Exception e) {
				//Handle all other 'unexpected' exceptions by logging the exception and failing the test
				log.fatal("unexpected exception, failing test", e);
				return fail("error dispatching request ["+mvcResourceKey+"] " + e.getMessage());
			}
		}

		*//**
		 * If set, apply the IbisRole to the mocked request.
		 *//*
		protected void applyIbisRole(FrankApiTestBase.IbisRole role) {
			if(role != null) {
				doReturn(true).when(springMvcResource.servletRequest).isUserInRole(role.name());
			}
		}

		*//**
		 * Convert the json input to the parameters class type
		 *//*
		private Object convertInputToParameterType(Parameter parameter, Object jsonOrFormdata) {
			ObjectMapper objectMapper = new ObjectMapper();
			try {
				return objectMapper.readValue((String) jsonOrFormdata, parameter.getType());
			} catch (Throwable e) {
				fail("error transforming json to type ["+parameter.getType()+"]");
				return null;
			}
		}

		*//**
		 * If a path parameter is used, try to find it
		 * @return the resolved path parameter value
		 *//*
		private String findPathParameter(Parameter parameter, String methodPath, String url) {
			PathVariable pathParameter = parameter.getAnnotation(PathVariable.class);
			String path = "{%s}".formatted(pathParameter.value());
			log.debug("looking up path [{}]", path);

			String[] pathSegments = methodPath.split("/");
			String[] urlSegments = getPath(url).split("/");
			String pathValue = null;
			for (int j = 0; j < pathSegments.length; j++) {
				String segment = pathSegments[j];
				if(segment.equals(path)) {
					pathValue = urlSegments[j];
					break;
				}
			}
			if(pathValue == null) {
				fail("unable to populate path param ["+path+"]");
			}
			return pathValue;
		}

		*//**
		 * If a query parameter is used, try to find it
		 * @return the resolved query parameter value
		 *//*
		private Object findQueryParameter(Parameter parameter, String url) {
			RequestParam queryParameter = parameter.getAnnotation(RequestParam.class);
			int questionMark = url.indexOf("?");
			if(questionMark == -1) {
				log.info("found query parameter [{}] on method but it was not present in the URL [{}]", queryParameter.value(), url);
			}

			String urlQueryParameters = url.substring(questionMark +1);
			String[] urlQuerySegments = urlQueryParameters.split("&");
			String queryValue = null;
			for (String querySegment : urlQuerySegments) {
				if(querySegment.startsWith(queryParameter.value()+"=")) {
					queryValue = querySegment.substring(querySegment.indexOf("=")+1);
					break;
				}
			}
			if(queryValue == null) {
				String defaultValue = queryParameter.defaultValue();
				if(!defaultValue.equals(ValueConstants.DEFAULT_NONE)) {
					queryValue = defaultValue;
				} else {
					return null;
				}
			}

			Object value = ClassUtils.convertToType(parameter.getType(), queryValue);
			log.info("resolved value [{}] to type [{}]", queryValue, value.getClass());
			return value;
		}

		*//**
		 * If a header parameter is used, try to find it
		 * @return the resolved header parameter value
		 *//*
		private Object findHeaderParameter(Parameter parameter, Map<String, String> headers) {
			RequestHeader headerParameter = parameter.getAnnotation(RequestHeader.class);
			String headerValue = headers.get(headerParameter.value());

			if(headerValue == null) {
				String defaultValue = headerParameter.defaultValue();
				if(!defaultValue.equals(ValueConstants.DEFAULT_NONE)) {
					headerValue = defaultValue;
				} else {
					return null;
				}
			}

			Object value = ClassUtils.convertToType(parameter.getType(), headerValue);
			log.info("resolved value [{}] to type [{}]", headerValue, value.getClass());
			return value;
		}

		private Object findMultiPartParameter(Parameter parameter, Object formDataMap) {
			RequestPart formPart = parameter.getAnnotation(RequestPart.class);
			String formDataName = formPart.value();
			if(formDataName.isEmpty()) {
				formDataName = formPart.name();
			}

			return ((Map<String, Object>)formDataMap).get(formDataName);
		}

		*//**
		 * Validate that all arguments are correctly set on the (Spring MVC resource) method
		 *//*
		private void validateIfAllArgumentsArePresent(Object[] methodArguments, Parameter[] parameters) {
			for (int j = 0; j < methodArguments.length; j++) {
				Object object = methodArguments[j];
				if(object == null) {
					log.warn("missing argument [{}] for method, expecting type [{}]", j, parameters[j]);
				}
			}
		}
	}

}*/
