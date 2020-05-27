# Nashorn Polyfill

This is the polyfill for Nashorn:

- [global, window, self, console, process](./global-polyfill.js)
- [Blob](./lib/blob-polyfill.js)
- [setTimeout, clearTimeout, setInterval, clearInterval](./lib/timer-polyfill.js)
- [URLSearchParams](https://www.npmjs.com/package/url-search-params)
- [XmlHttpRequest](./lib/xml-http-request.polyfill.js)
- [core-js:shim](https://github.com/zloirock/core-js/blob/master/shim.js)

Missing polyfill:

- FormData

## Variable injected in ScriptContext

### Required

#### `__NASHORN_POLYFILL_TIMER__`

instance of ScheduledExecutorService.

Sample:

```
static ScheduledExecutorService globalScheduledThreadPool = Executors.newScheduledThreadPool(20);

// Injection of __NASHORN_POLYFILL_TIMER__ in ScriptContext
sc.setAttribute("__NASHORN_POLYFILL_TIMER__", globalScheduledThreadPool, ScriptContext.ENGINE_SCOPE);
```


### Optional

#### `__HTTP_SERVLET_REQUEST__`

The HttpServletRequest instance. If this variable injected, XmlHttpRequest polyfill will copy `Cookie` and `Authorization` headers of HttpServletRequest, so that AJAX call from Javascript application will act like user session fired from browser.

If more headers want to be copied, it is easy to customize it in [xml-http-request-polyfill](./lib/xml-http-request-polyfill.js) line 92.

## Required Java Jars:

gradle
```
compile group: 'org.apache.httpcomponents', name: 'httpclient', version: '4.5.2'
compile group: 'org.apache.httpcomponents', name: 'httpasyncclient', version: '4.1.2'
compile group: 'org.apache.commons', name: 'commons-pool2', version: '2.4.2'
```

## Important Notes

In [https://github.com/morungos/java-xmlhttprequest](https://github.com/morungos/java-xmlhttprequest), it uses `Timer` to run `setTimeout` and `setInterval` task, but they are run in a separate thread of the `Timer` creates that is different with the main JavaScript thread.

This implementation uses `ScheduledExecutorService` instead of `Timer` so the threads for task scheduling can be reused instead of each JavasScript thread create a `Timer` thread when using `Timer`.

And most important thing is this adds `global.nashornEventLoop` and scheduled tasks only add function callback object in eventLoop (ArrayQueue), and it is main JavaScript thread to run these function callback by calling `global.nashornEventLoop.process();` at the end of JavaScript Application. It is just like browser or NodeJS that event loop is called when the main stack is cleared.

When runs on server with Promise, remember to call `nashornEventLoop.process()` when waiting for Promise by Thread.sleep(), and call `nashornEventLoop.reset()` if server thread (e.g. Servlet thread) decides to be timeout so that eventLoop will be clean for next request.

# Link

### [Product Demo](https://demo.moshian.com): This is demo of a product with react, react-router, apollo (GraphQL client)
### [Moqui React SSR Demo](https://github.com/shendepu/moqui-react-ssr-demo):

This demo shows how react app is rendered on server side. The code playing with Nashorn Script Engine sits in [Moqui React SSR](https://github.com/shendepu/moqui-react-ssr) which is easy to extract to be used in any Java application.

# License

[MIT](./LICENSE)
