Code Style Guidelines
=====================

The code style has been defined in a few different formats:
* `.editorconfig`
* `Frank_Framework_CodeStyle IntelliJ.xml`
* `Frank_Framework_CodeStyle Eclipse.xml`

Please apply the appropriate file to the project if it's not picked up automatically.

I want to suggest a few additional code style guidelines.

Aim of these code style guidelines is to promote code that is:
- Readable
- Testable
- Maintainable
- Has fewer bugs
- Make it easier to reason about what the code does
- Documented

Guidelines
----------

1. Use `final` parameters and variables where possible.
   - Avoids bugs through accidental re-assignment,
   - Allows more compiler optimisations.
   
   For instance, instead of writing:
```java
String x = null;
if (condition) {
	x = "value";
}
```
Write:
```java
final String x = (condition) ? "value" : null;
```


2. Function names that describe _what_ the code accomplishes, rather than _how_ it
	does so.
	
	For instance: 
	
	`public StringTokenizer AppConstants.getTokenizedProperty() {...}` 
	
	describes _how_ the code accomplishes something (using a StringTokenizer, but not clear for what
	purpose until you read the docs).
	
	`public List AppConstants.getMultiValuedProperty() {...}` (or `getListProperty()`)
	
	would tell you _what_ the code accomplishes without you having to look at the JavaDoc,
	and without bothering you with the how.


3. Delegate subtasks of your function to other functions
   - Keeps your code shorter
     Thus it reduces complexity, makes code more readable
   - Name of function tells the intention of the code instead of just the actions
   - Helps writing code that doesn't need to modify state variables
   - Increases code testability because the helper function can be tested independently


4. Early Returns from functions
   - Don't embed the main code of a function inside an `if` condition, but instead
     invert that `if` condition and if condition is not met, immediately exit the
     function.

     This reduces nesting in the code and keeps it clearer to the reader what the main
     body of the function is, what the preconditions are, and that there is no `else`
     following the `if` with an alternative path.


5. Functions without side effects:
   - Don't modify global state from you functions
   - Whenever possible, don't modify input arguments either
   - Compute something from the arguments, and return that
     Makes it easy to test, makes it easier to ensure your code is overall correct, makes
     it easier to read and understand the code calling the function.


6. "Triple-A Testing":

	 Insert the comments `// Arrange`, `// Act` and `// Assert` in your unit tests in the
	 places where you start doing test setup, where you perform the actual action to be tested,
	 and where you start doing asserts to verify the results.
	
	 This helps to make tests more readable, by making it clear what is being tested.
	
	 Sometimes no setup is needed so it can be skipped, and sometimes (when using `assertThrows()`
	 for instance) there is no separation between Act and Assert so you can add a comment like `// Act / Assert`
	 to indicate these steps are performed together.


7. Java Streams with `.map()` and related functions

	The below is a suggestion and explanation, but not necessarily something I would like to see
	promoted to a new coding standard right away.
	
	 - Replace `for` loops with stream operations
	 - For readability put each stream operation on a separate line!
	 - The idea is to put focus the actual operations your code does instead of burying that
		in the ceremony around it.

8. Usage of 'var'
	Since Java 9, the usage of `var` is supported. This is still strongly typed, but notation is less verbose:
	`String string = new String()` can become `var string = new String()`.
	This can become less readable when using the return type of a method. To keep code as readable as possible, we only use 
	this when the type is clear, such as a direct assignment of a string or scalar value or constructor. Try to avoid using `var` for variables assigned from method return values, this makes code less readable
	when not using an IDE.
9. Abstract classes	
   
	When creating abstract classes, name them like `AbstractClass` and don't use `ClassBase`. When using a base/abstract super class, combined with interfaces, see if a `default` interface method might suffice.
10. Java Optionals.

	You can use Java `Optional` to indicate that the return value of a method can be `null`.
	Optional should not be used for parameters.
	When working with streams and extracting methods to integrate in the streams, working with Optional return values makes code easier to understand. Refactoring existing code might not be that easy. 
	If you do not use `Optional` then it is a good idea to annotate your methods with `@Nonnull`
	and companions.
	One good scenario for using `Optional` is for avoiding re-assigning variables which could
	otherwise be `final` in a scenario like this:
```java
MyClass value = getSomeValue(key);
if (value == null) {
   value = new MyClass(a, b, c);
}
```

If `getSomeValue()` would return `Optional<MyClass>` then we could rewrite this as:
```java
final MyClass value = getSomeValue(key).orElseGet(() -> new MyClass(a, b, c));
```

For larger numbers of alternative choices, when using Java9 or higher a number of `Optional`s can
be chained using `or`:
```java
Optional<String> getValue(String key, Collection<String> source) { ... }
Optional<String> alternativeProvider(String key) { ... }

final String value = getValue(key, source1)
					   .or(()-> getValue(key, soure2))
					   .or(()-> alternativeProvider(key))
					   .orElse("default");
```

__NOTE:__   
In my experience, it does take some getting used to to read code with streams or optionals!

Here is an example of how a loop could be replaced with Streams.

Now which do you consider more readable?
Which version makes its intent clearer?

Before:

```java
protected String[] getSpringConfigurationFiles(ClassLoader classLoader) {
	List<String> springConfigurationFiles = new ArrayList<>();
	
	// Some lines of code omitted b/c they're not relevant to this refactoring
	
	StringTokenizer locationTokenizer = AppConstants.getInstance().getTokenizedProperty("SPRING.CONFIG.LOCATIONS");
	while (locationTokenizer.hasMoreTokens()) {
		String file = locationTokenizer.nextToken();
		log.debug("found spring configuration file to load [{}]", file);

		URL fileURL = classLoader.getResource(file);
		if (fileURL == null) {
			log.error("unable to locate Spring configuration file [{}]", file);
		} else {
			if (!file.contains(":")) {
				file = ResourceUtils.CLASSPATH_URL_PREFIX + "/" + file;
			}

			springConfigurationFiles.add(file);
		}
	}
	
	// More code omitted
	
	return springConfigurationFiles.toArray(new String[springConfigurationFiles.size()]);
}
```

After:

```java
Arrays
	.stream(AppConstants.getInstance().getProperty("SPRING.CONFIG.LOCATIONS")
		.split(","))
	.filter(filename -> isSpringConfigFileOnClasspath(classLoader, filename))
	.map(this::addClasspathPrefix)
	.forEach(springConfigurationFiles::add);
```

To help making the code readable, this refactoring extracts some logic in two short helper functions:

```java
private boolean isSpringConfigFileOnClasspath(ClassLoader classLoader, String filename) {
	URL fileURL = classLoader.getResource(filename);
	if (fileURL == null) {
		log.error("unable to locate Spring configuration file [{}]", filename);
	}
	return fileURL != null;
}

private String addClasspathPrefix(String filename) {
	if (filename.contains(":")) {
		return filename;
	}
	return ResourceUtils.CLASSPATH_URL_PREFIX + "/" + filename;
}
```

I do that instead of putting more logic inside the lambda functions inside the `map` and `filter`
statements on the stream to keep things simple -- but it is a judgement call. Sometimes I
put more logic inside the lambda functions.

In the first version, I do not instantly see why a URL is loaded from the ClassLoader
for instance. In the rewritten version, I see the intention is to see if the file is
readable.

Now there are still some side effects in this code. For instance, it appends to an existing
collection instead of adding only to a new collection.
Testability is also reduced because it depends on retrieving some global state instead of having
simple input. However this code is part of a larger function.
So ideally, I would extract the entire loop into a new function:

```java
private List<String> splitIntoConfigFiles(ClassLoader classLoader, String fileList) {
	return Arrays
		.stream(fileList.split(","))
		.filter(filename -> isSpringConfigFileOnClasspath(classLoader, filename))
		.map(this::addClasspathPrefix)
		.collect(Collectors.toList());
	
}
```

This is testable with any input, and to me is more readable. Individual subtasks can be tested
explicitly instead of other unit-tests just happening to cover these code-paths:

- To test with any input regardless of what exists on the classpath, the function `isSpringConfigFileOnClasspath`
  could be mocked
- To test `isSpringConfigFileOnClasspath` itself, make sure you pass it files you know to
  either exist or not exist on the classpath

The entire original function now reads like this:

```java
protected String[] getSpringConfigurationFiles(ClassLoader classLoader) {
	List<String> springConfigurationFiles = new ArrayList<>();
	if (parentContext == null) { //When not running in a web container, populate top-level beans so they can be found throughout this/sub-contexts.
		springConfigurationFiles.add(SpringContextScope.STANDALONE.getContextFile());
	}
	springConfigurationFiles.add(SpringContextScope.APPLICATION.getContextFile());
	springConfigurationFiles.addAll(splitIntoConfigFiles(classLoader, AppConstants.getInstance().getProperty("SPRING.CONFIG.LOCATIONS")));
	addJmxConfigurationIfEnabled(springConfigurationFiles);

	log.info("loading Spring configuration files {}", springConfigurationFiles);
	return springConfigurationFiles.toArray(new String[springConfigurationFiles.size()]);
}
```

Which again is more to the point and delegates subtasks.

Now we have to be practical and pragmatic, not dogmatic. Not all code improves by using
Java Streams -- Kotlin has a few more tools to make Stream-like code better than Java lacks,
at least Java8. (I'm not sure about Java 17).

For instance:

```java
private void registerApplicationModules(List<String> modules) {
	for (String module : modules) {
		String version = getModuleVersion(module);

		if (version != null) {
			iafModules.put(module, version);
			APP_CONSTANTS.put(module + ".version", version);
			log.info("Loading IAF module [{}] version [{}]", module, version);
		}
	}
}
```

Probably is still more readable than the Streams API alternative that I could come up with,
because 2 pieces of information need to be passed on which is clumsy in Java:

```java
private void registerApplicationModules(List<String> modules) {
	modules.stream()
		.map(module -> Pair.of(module, getModuleVersion(module)))
		.filter(pair -> pair.getRight() != null)
		.forEach(pair -> {
			String module = pair.getLeft();
			String version = pair.getRight();
			iafModules.put(module, version);
			APP_CONSTANTS.put(module + ".version", version);
			log.info("Loading IAF module [{}] version [{}]", module, version);
		});
}
```

It requires the use of a `Pair` class and although in Kotlin you would also need that, in
Kotlin you at least have a neat way to unpack the information:

```kotlin
fun registerApplicationModules(modules: List<String>) =
  modules.map { module -> Pair(module, getModuleVersion(module)) }
		 .filter { pair -> pair.second != null }
		 .forEach { (module, version) ->
			 iafModules.put(module, version)
			 APP_CONSTANTS.put(module + ".version", version)
			 log.info { "Loading IAF module [$version] version [$module]" }
		 }
```

### Documentation
Since javadoc is used for generating documentation in the Frank!Doc, it's important to provide usable information in the class for Frank Developers.

* Please provide documentation in the javadoc if there's been a (breaking) change and offer an example in the documentation. See [FixedResultPipe.java](https://github.com/frankframework/frankframework/blob/master/core/src/main/java/org/frankframework/pipes/FixedResultPipe.java) for instance.  
* When using multiline Code examples, please use `<pre>{@code ... }</pre>` blocks.
* When referring to classes, please use `{@link ClassName}`
* Frank!Doc treats {@value} and {@literal} differently, please use them like this:
  * When referring to a class value, please use `{@value #VALUE}`
  * When referring to a value (of a parameter, variable, etc), please use `{@literal null}`
