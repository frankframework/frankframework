import './services/alert.service';
import './services/api.service';
import './services/cookies.service';
import './services/debug.service';
import './services/gdpr.service';
import './services/hooks.service';
import './services/notification.service';
import './services/poller.service';
import './services/session.service';
import './services/sweetalert.service';
import './services/toastr.service';

angular.module('iaf.beheerconsole')
	.filter('ucfirst', function() {
		return function(s) {
			return (angular.isString(s) && s.length > 0) ? s[0].toUpperCase() + s.substr(1).toLowerCase() : s;
		};
	}).filter('truncate', function() {
		return function(input, length) {
			if(input && input.length > length) {
				return input.substring(0, length) + "... ("+(input.length - length)+" characters more)";
			}
			return input;
		};
	}).filter('dropLastChar', function() {
		return function(input) {
			if(input && input.length > 0) {
				return input.substring(0, input.length-1);
			}
			return input;
		};
	}).filter('markDown', function() {
		return function(input) {
			if(!input) return;
			input = input.replace(/(?:\r\n|\r|\n)/g, '<br />');
			input = input.replace(/\[(.*?)\]\((.+?)\)/g, '<a target="_blank" href="$2" alt="$1">$1</a>');
			return input;
		};
	}).filter('withJavaListener', function() {
		return function(adapters) {
			if(!adapters) return;
			let schedulerEligibleAdapters={};
			for(const adapter in adapters) {
				let receivers = adapters[adapter].receivers;
				for(const r in receivers) {
					let receiver=receivers[r];
					if(receiver.listener.class.startsWith('JavaListener')){
						schedulerEligibleAdapters[adapter] = adapters[adapter];
					}
				}
			}
			return schedulerEligibleAdapters;
		};
	}).filter('dash', function() {
		return function(input) {
			if(input || input === 0) return input;
			else return "-";
		};
	}).filter('perc', function() {
		return function(input) {
			if(input || input === 0) return input+"%";
			else return "-";
		};
	}).filter('formatStatistics', function() {
		return function(input, format) {
			if(!input || !format) return; //skip when no input
			var formatted = {};
			for(const key in format) {
				var value = input[key];
				if(!value && value !== 0) { // if no value, return a dash
					value = "-";
				}
				if((key.endsWith("ms") || key.endsWith("B")) && value != "-") {
					value += "%";
				}
				formatted[key] = value;
			}
			formatted["$$hashKey"] = input["$$hashKey"]; //Copy the hashKey over so Angular doesn't trigger another digest cycle
			return formatted;
		};
	}).factory('authService', ['$rootScope', '$http', 'Base64', '$location', 'appConstants', 'Misc',
		function($rootScope, $http, Base64, $location, appConstants, Misc) {
		var authToken;
		return {
			login: function(username, password) {
				if(username != "anonymous") {
					authToken = Base64.encode(username + ':' + password);
					sessionStorage.setItem('authToken', authToken);
					$http.defaults.headers.common['Authorization'] = 'Basic ' + authToken;
				}
				var location = sessionStorage.getItem('location') || "status";
				var absUrl = window.location.href.split("login")[0];
				window.location.href = (absUrl + location);
				window.location.reload();
			},
			loggedin: function() {
				var token = sessionStorage.getItem('authToken');
				if(token != null && token != "null") {
					$http.defaults.headers.common['Authorization'] = 'Basic ' + token;
					if($location.path().indexOf("login") >= 0)
						$location.path(sessionStorage.getItem('location') || "status");
				}
				else {
					if(appConstants.init > 0) {
						if($location.path().indexOf("login") < 0)
							sessionStorage.setItem('location', $location.path() || "status");
						$location.path("login");
					}
				}
			},
			logout: function() {
				sessionStorage.clear();
				$http.defaults.headers.common['Authorization'] = null;
				$http.get(Misc.getServerPath() + "iaf/api/logout");
			}
		};
	}]).factory('Base64', function () {
		var keyStr = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';

		return {
			encode: function (input) {
				var output = "";
				var chr1, chr2, chr3 = "";
				var enc1, enc2, enc3, enc4 = "";
				var i = 0;

				do {
					chr1 = input.charCodeAt(i++);
					chr2 = input.charCodeAt(i++);
					chr3 = input.charCodeAt(i++);

					enc1 = chr1 >> 2;
					enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
					enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
					enc4 = chr3 & 63;

					if (isNaN(chr2)) {
						enc3 = enc4 = 64;
					} else if (isNaN(chr3)) {
						enc4 = 64;
					}

					output = output +
						keyStr.charAt(enc1) +
						keyStr.charAt(enc2) +
						keyStr.charAt(enc3) +
						keyStr.charAt(enc4);
					chr1 = chr2 = chr3 = "";
					enc1 = enc2 = enc3 = enc4 = "";
				} while (i < input.length);

				return output;
			},

			decode: function (input) {
				var output = "";
				var chr1, chr2, chr3 = "";
				var enc1, enc2, enc3, enc4 = "";
				var i = 0;

				// remove all characters that are not A-Z, a-z, 0-9, +, /, or =
				var base64test = /[^A-Za-z0-9\+\/\=]/g;
				if (base64test.exec(input)) {
					console.error("There were invalid base64 characters in the input text.\n" +
						"Valid base64 characters are A-Z, a-z, 0-9, '+', '/',and '='\n" +
						"Expect errors in decoding.");
				}
				input = input.replace(/[^A-Za-z0-9\+\/\=]/g, "");

				do {
					enc1 = keyStr.indexOf(input.charAt(i++));
					enc2 = keyStr.indexOf(input.charAt(i++));
					enc3 = keyStr.indexOf(input.charAt(i++));
					enc4 = keyStr.indexOf(input.charAt(i++));

					chr1 = (enc1 << 2) | (enc2 >> 4);
					chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
					chr3 = ((enc3 & 3) << 6) | enc4;

					output = output + String.fromCharCode(chr1);

					if (enc3 != 64) {
						output = output + String.fromCharCode(chr2);
					}
					if (enc4 != 64) {
						output = output + String.fromCharCode(chr3);
					}

					chr1 = chr2 = chr3 = "";
					enc1 = enc2 = enc3 = enc4 = "";

				} while (i < input.length);

				return output;
			}
		};
	}).config(['$httpProvider', function($httpProvider) {
		$httpProvider.interceptors.push(['appConstants', '$q', 'Misc', 'Toastr', '$location', function(appConstants, $q, Misc, Toastr, $location) {
			var errorCount = 0;
			return {
				request: function(config) {
					//First check if we can append the version, then if it's an HTML file, and lastly if it's ours!
					if (ff_version != null && config.url.indexOf('.html') !== -1 && config.url.indexOf('views/') !== -1) {
						config.url = config.url + '?v=' + ff_version;
					}
					return config;
				},
				responseError: function(rejection) {
					if(rejection.config) { //It should always have a config object, but just in case!
						if(rejection.config.url && rejection.config.url.indexOf(Misc.getServerPath()) < 0) return $q.reject(rejection); //Don't capture non-api requests

						switch (rejection.status) {
							case -1:
								fetch(rejection.config.url, { redirect: "manual" }).then((res) => {
									if (res.type === "opaqueredirect") {
										// if the request ended in a redirect that failed, then login
										login_url = Misc.getServerPath() + 'iaf/';
										window.location.href = login_url;
									}
								});

								if(appConstants.init == 1) {
									if(rejection.config.headers["Authorization"] != undefined) {
										console.warn("Authorization error");
									} else {
										Toastr.error("Failed to connect to backend!");
									}
								}
								else if(appConstants.init == 2 && rejection.config.poller) {
									console.warn("Connection to the server was lost!");
									errorCount++;
									if(errorCount == 3) {
										Toastr.error({
											title: "Server Error",
											body: "Connection to the server was lost! Click to refresh the page.",
											timeout: 0,
											showCloseButton: true,
											clickHandler: function(_, isCloseButton) {
												if(isCloseButton !== true) {
													window.location.reload();
												}
												return true;
											}
										});
									}
								}
								break;
							case 400:
								Toastr.error("Request failed", "Bad Request, check the application logs for more information.");
								break;
							case 401:
								sessionStorage.clear();
								$location.path("login");
								break;
							case 403:
								Toastr.error("Forbidden", "You do not have the permissions to complete this operation.");
								break;
							case 500:
								if(rejection.config.intercept != undefined && rejection.config.intercept === false) return $q.reject(rejection); //Don't capture when explicitly disabled
								if(rejection.data != null && rejection.data.error != null) //When formatted data is returned, Toast it!
									Toastr.error("Server Error", rejection.data.error);
								break;
						}
					}
					// otherwise, default behaviour
					return $q.reject(rejection);
				}
			};
		}]);
	}]);
