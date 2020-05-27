(function (context) {
  var System = Java.type('java.lang.System');

  function it(testName, exec) {
    try {
      print('Starting ' + testName);
      var result = exec(testName);
      if (typeof result === 'object' && typeof result.catch === 'function') {
        result.then(function() {
          print('[success] ' + testName);
        }).catch(function(error) {
          System.err.println('[failed] ' + testName);
        });
      } else {
        print('[success] ' + testName);
      }
    } catch (error) {
      System.err.println('[failed] ' + testName);
    }
    setTimeout(function () {}, 2000);
  }

  context.it = it;
})(this);
