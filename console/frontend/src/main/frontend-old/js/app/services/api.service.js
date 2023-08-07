import { appModule } from "../app.module";

appModule.service('Api', ['$http', 'appConstants', 'Misc', 'Session', 'Debug', function ($http, appConstants, Misc, Session, Debug) {
	var absolutePath = Misc.getServerPath();
	absolutePath += "iaf/api/";
	var etags = {};
	var allowed = {};

	function buildURI(uri) {
		return absolutePath + uri;
	}

	$http.defaults.headers.post["Content-Type"] = "application/json";
	$http.defaults.timeout = appConstants["console.pollerInterval"] - 1000;

	this.Get = function (uri, callback, error, httpOptions, intercept) {
		var defaultHttpOptions = { headers: {}, intercept: intercept };

		if (httpOptions) {
			//If httpOptions is TRUE, skip additional/custom settings, if it's an object, merge both objects
			if (typeof httpOptions == "object") {
				angular.merge(defaultHttpOptions, defaultHttpOptions, httpOptions);
				if (!httpOptions.poller) {
					Debug.log("Sending request to uri [" + uri + "] using HttpOptions ", defaultHttpOptions);
				}
			}
		}
		if (etags.hasOwnProperty(uri)) { //If not explicitly disabled (httpOptions==false), check eTag
			var tag = etags[uri];
			defaultHttpOptions.headers['If-None-Match'] = tag;
		}

		return $http.get(buildURI(uri), defaultHttpOptions).then(function (response) {
			if (callback && typeof callback === 'function') {
				if (response.headers("etag")) {
					etags[uri] = response.headers("etag");
				}
				if (response.headers("allow")) {
					allowed[uri] = response.headers("allow");
				}
				callback(response.data);
			}
		}).catch(function (response) { errorException(response, error); });
	};

	this.Post = function () { // uri, object, callback, error, intercept 4xx errors
		var args = Array.prototype.slice.call(arguments);
		var uri = args.shift();
		var object = (args.shift() || {});
		var headers = {};
		if (object instanceof FormData) {
			headers = { 'Content-Type': undefined }; //Unset default contentType when posting formdata
		}
		var callback = args.shift();
		var error = args.shift();
		var intercept = args.shift();
		var responseType = args.shift();

		return $http.post(buildURI(uri), object, {
			headers: headers,
			responseType: responseType,
			transformRequest: angular.identity,
			intercept: intercept,
		}).then(function (response) {
			if (callback && typeof callback === 'function') {
				etags[uri] = response.headers("etag");
				callback(response.data);
			}
		}).catch(function (response) { errorException(response, error); });
	};

	this.Put = function (uri, object, callback, error, intercept) {
		var headers = {};
		var data = {};
		if (object != null) {
			if (object instanceof FormData) {
				data = object;
				headers["Content-Type"] = undefined;
			} else {
				data = JSON.stringify(object);
				headers["Content-Type"] = "application/json";
			}
		}
		var intercept = intercept;

		return $http.put(buildURI(uri), data, {
			headers: headers,
			transformRequest: angular.identity,
			intercept: intercept,
		}).then(function (response) {
			if (callback && typeof callback === 'function') {
				etags[uri] = response.headers("etag");
				callback(response.data);
			}
		}).catch(function (response) { errorException(response, error); });
	};

	this.Delete = function () { // uri, callback, error || uri, object, callback, error
		var args = Array.prototype.slice.call(arguments);
		var uri = args.shift();
		var request = { url: buildURI(uri), method: "delete", headers: {} };
		var callback;

		var object = args.shift(); // this can be a function or an object.
		if (object instanceof Function) { //we have a callback function, that means no object is present!
			callback = object; // set the callback method accordingly
		} else {
			if (object instanceof FormData) {
				request.data = object;
				request.headers["Content-Type"] = undefined;
			} else {
				request.data = JSON.stringify(object);
				request.headers["Content-Type"] = "application/json";
			}

			callback = args.shift(); // the previous argument was an object, that means the next object is the callback!
		}

		var error = args.shift();
		request.intercept = args.shift();

		return $http(request).then(function (response) {
			if (callback && typeof callback === 'function') {
				etags[uri] = response.headers("etag");
				callback(response.data);
			}
		}).catch(function (response) { errorException(response, error); });
	};

	var errorException = function (response, callback) {
		if (response.status != 304) {
			var status = (response.status > 0) ? " " + response.status + " error" : "n unknown error";
			if (response.status == 404 || response.status == 500) {
				var config = response.config;
				var debug = " url[" + config.url + "] method[" + config.method + "]";
				if (config.data && config.data != "") debug += " data[" + config.data + "]";
				Debug.warn("A" + status + " occurred, please notify a system administrator!" + '\n' + debug);
			}
			else {
				Debug.info("A" + status + " occured.", response);
			}

			if ((response.status != 304) && (callback && typeof callback === 'function')) {
				callback(response.data, response.status, response.statusText);
			}
		}
	};

	//Getters
	this.errorException = errorException;
	this.absolutePath = absolutePath;
	this.etags = etags;

	this.flushCache = function () {
		etags = {};
	};

}]);
