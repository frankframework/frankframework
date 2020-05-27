(function (context) {
  function test(name) {
    return function () { print(name); }
  }
  var timerId = setTimeout(test('setTimeout 500ms'), 3000);
  print('clearTimeout clears timerId ' + timerId);
  clearTimeout(timerId)

  setTimeout(function() {
    context.__nashorn_polyfill_timer.cancel();
    if (context.__nashorn_polyfill_timerMap.size() !== 1) {
      throw new Error('timer is not cleaned.')
    }
  }, 1 * 1000)
})(this);
