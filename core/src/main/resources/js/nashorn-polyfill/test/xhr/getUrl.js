(function (context) {
  var url = 'http://requestb.in/r48vlqr4';

  it('Get url', function () {
    return fetch(url).then(function(response) {
      if (response.status != 200) throw new Error();
    });
  });

  setTimeout(function() { context.__nashorn_polyfill_timer.cancel(); }, 3 * 1000);
})(this);
