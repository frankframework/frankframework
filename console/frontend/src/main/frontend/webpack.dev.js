const config = require('./webpack.config');
const path = require("path");
const distDir = path.resolve(__dirname, '../../../target/classes/META-INF/resources/iaf/gui/');

config.output.path = distDir;
module.exports = config;
