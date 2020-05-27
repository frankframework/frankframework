(function (context) {
  function test(name) {
    return function () { print(name + ' ' + i++); }
  }
  var i = 1;
  var timerId = setInterval(test('setInterval 100ms'), 100);
  setTimeout(function () {
    clearTimeout(timerId)
    if (i - 1 !== 2) throw new Error('setInterval should only run 2 times.');
  }, 250)

  setTimeout(function() {
    context.__nashorn_polyfill_timer.cancel();
    print('The process is terminated by hacking way of __nashorn_polyfill_timer.cancel().');
  }, 2 * 1000)
})(this);
