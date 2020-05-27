(function (context) {
  var url = 'http://requestb.in/r48vlqr4';

  it('Test XmlHttpRequest post with body null', function() {
    return fetch(url, {
      method: 'POST',
      body: null
    }).then(function (response) {
      if (response.status !== 200) throw new Error();
    });
  });

  it('Test XmlHttpRequest post with body undefined', function () {
    return fetch(url, {
      method: 'POST'
    }).then(function (response) {
      if (response.status !== 200) throw new Error();
    });
  });

  it('Test XmlHttpRequest post with json body', function () {
    return fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        username: 'tester',
        password: 'tester'
      })
    }).then(function(response) {
      if (response.status !== 200) throw new Error();
    });
  })

  setTimeout(function() { context.__nashorn_polyfill_timer.cancel(); }, 5 * 1000);
})(this);
