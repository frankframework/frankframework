const path = require('path')

function base () {
  const args = [path.resolve(__dirname, '..')].concat([].slice.call(arguments))
  return path.resolve.apply(path, args)
}

module.exports = {
  base: base,
  lib: base.bind(null, 'lib'),
  build: base.bind(null, 'build'),
  test: base.bind(null, 'test'),
  dist: base.bind(null, 'dist')
}
