const path = require('path');
const CopyPlugin = require("copy-webpack-plugin");
const HtmlWebpackPlugin = require('html-webpack-plugin');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');

const distDir = path.resolve(__dirname, '../../../target/frontend/');

module.exports = {
  mode: 'development',
  entry: './index.js',
  output: {
    filename: 'js/[name].[contenthash].js',
    // filename: 'js/[name].bundle.js',
    path: distDir,
  },
  optimization: {
    runtimeChunk: 'single',
    splitChunks: {
	  cacheGroups: {
	    vendor: {
		  test: /[\\/]node_modules[\\/]/,
		  name: 'vendors',
		  chunks: 'all'
		}
	  }
	}
  },
  plugins: [
    new CopyPlugin({
      patterns: [
        { from: "./css/patterns", to: "css/patterns" },
        { from: "./css/plugins/iCheck/green.png", to: "css/green.png" },
        { from: "./css/plugins/iCheck/green@2x.png", to: "css/green@2x.png" },
        { from: "./js/**/*.html" },
        { from: "./images", to: "images" },
        // { from: "./views", to: "views" },
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
      {
        test: require.resolve("jquery"),
        loader: "expose-loader",
        options: {
          exposes: ["$", "jQuery"],
        },
      },
    ]
  },
	devServer: {
		port: 4200,
		open: true,
		proxy: {
			'/*/iaf/api': 'http://localhost:8080',
		},
	},
};
