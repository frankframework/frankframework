(function (context) {
  function test(name) {
    return function () { print(name + ' ' + i++); }
  }
  var i = 1;
  setInterval(test('setInterval 100ms'), 200);

  setTimeout(function() {
    context.__nashorn_polyfill_timer.cancel();
    if (i - 1 !== 5) throw new Error('setInterval should only run 5 times.');
  }, 1050)
})(this);
