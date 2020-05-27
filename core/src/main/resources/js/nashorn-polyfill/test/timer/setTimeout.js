(function (context) {
  function test(name) {
    return function () { print(name); }
  }

  setTimeout(test('setTimeout 0'), 0);
  setTimeout(test('setTimeout 500ms'), 500);

  setTimeout(function() {
    context.__nashorn_polyfill_timer.cancel();
    print('The process is terminated by hacking way of __nashorn_polyfill_timer.cancel().');
  }, 2 * 1000)
})(this);
