const config = require('./webpack.config');
const path = require("path");

// const distDir = path.resolve(__dirname, '../../../target/classes/META-INF/resources/iaf/gui/');
// config.output.path = distDir;
config.devServer.proxy = {
	'/iaf/api': 'http://localhost:8080/iaf-test/',
}

module.exports = config;
