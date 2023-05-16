import './app/components/pages/sidebar.service';

import './app/services/alert.service';
import './app/services/api.service';
import './app/services/cookies.service';
import './app/services/debug.service';
import './app/services/gdpr.service';
import './app/services/hooks.service';
import './app/services/misc.service';
import './app/services/notification.service';
import './app/services/poller.service';
import './app/services/session.service';
import './app/services/sweetalert.service';
import './app/services/toastr.service';

angular.module('iaf.beheerconsole')
	.factory('authService', ['$rootScope', '$http', 'Base64', '$location', 'appConstants', 'Misc',
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
