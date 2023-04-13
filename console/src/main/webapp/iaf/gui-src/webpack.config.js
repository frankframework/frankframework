const path = require('path');
const HtmlWebpackPlugin = require("html-webpack-plugin");
const CopyPlugin = require("copy-webpack-plugin");
const { CleanWebpackPlugin } = require('clean-webpack-plugin');

module.exports = {
  mode: 'development',
  entry: './index.js',
  output: {
    filename: 'js/main.[contenthash].js',
    path: path.resolve(__dirname, '../gui/'),
  },
  plugins: [
    new CopyPlugin({
      patterns: [
		{ from: "./css/patterns", to: "css/patterns" },
        { from: "./images", to: "images" },
        { from: "./views", to: "views" },
      ],
    }),
    new HtmlWebpackPlugin({
      filename: 'index.html',
      template: './index.html'
    }),
    new CleanWebpackPlugin(),
  ],
  module: {
    rules: [
      {
        test: /\.m?js$/,
        exclude: /(node_modules|bower_components)/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: ['@babel/preset-env'],
            plugins: ['angularjs-annotate']
          }
        }
      },
      {
        test: /\.(scss|css)$/,
        use: [
          {
            loader: 'style-loader',
            options: {
              injectType: 'linkTag'
            }
          },
          {
            loader: 'file-loader',
            options: {
              name: "css/[name].[contenthash].css",
            }
          },
          'sass-loader'
        ]
      },
      {
        test: /\.(woff(2)?|ttf|eot|svg)(\?v=\d+\.\d+\.\d+)?$/,
        use: [
          {
            loader: 'file-loader',
            options: {
              name: '[name].[ext]',
              outputPath: 'fonts/'
            }
          }
        ]
      },
    ]
  }
};
