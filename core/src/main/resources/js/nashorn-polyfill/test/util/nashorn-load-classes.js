(function(context) {
  var Thread = Java.type('java.lang.Thread');
  var JarFile = Java.type('java.util.jar.JarFile');
  var URL = Java.type('java.net.URL');
  var URLClassLoader = Java.type('java.net.URLClassLoader');

  function loadJars(jarFiles) {
    var urls = [];

    jarFiles.forEach(function(jarFilePath) {
      urls.push(new URL("jar:file:" + jarFilePath + "!/"))
    });

    var javaUrls = Java.to(urls, 'java.net.URL[]');
    var cl = URLClassLoader.newInstance(javaUrls);
    Thread.currentThread().setContextClassLoader(cl);

    jarFiles.forEach(function(jarFilePath) {
      var jarFile = new JarFile(jarFilePath);
      var e = jarFile.entries();

      while (e.hasMoreElements()) {
        var je = e.nextElement();
        if (je.isDirectory() || !je.getName().endsWith(".class")) {
          continue;
        }
        // -6 because of .class
        var className = je.getName().substring(0, je.getName().length() - 6);
        className = className.replaceAll('/', '.');

        var c = cl.loadClass(className);
      }
    });
  }

  loadJars(["${$ENV.PWD}/test/lib/httpcore-4.4.5.jar",
    "${$ENV.PWD}/test/lib/httpclient-4.5.2.jar",
    "${$ENV.PWD}/test/lib/httpcore-nio-4.4.5.jar",
    "${$ENV.PWD}/test/lib/httpasyncclient-4.1.2.jar"])
})(this);
