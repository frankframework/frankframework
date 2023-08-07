import { appModule } from "../app.module";

appModule.factory('authService', ['$rootScope', '$http', 'Base64', '$location', 'appConstants', 'Misc',
	function ($rootScope, $http, Base64, $location, appConstants, Misc) {
		var authToken;
		return {
			login: function (username, password) {
				if (username != "anonymous") {
					authToken = Base64.encode(username + ':' + password);
					sessionStorage.setItem('authToken', authToken);
					$http.defaults.headers.common['Authorization'] = 'Basic ' + authToken;
				}
				var location = sessionStorage.getItem('location') || "status";
				var absUrl = window.location.href.split("login")[0];
				window.location.href = (absUrl + location);
				window.location.reload();
			},
			loggedin: function () {
				var token = sessionStorage.getItem('authToken');
				if (token != null && token != "null") {
					$http.defaults.headers.common['Authorization'] = 'Basic ' + token;
					if ($location.path().indexOf("login") >= 0)
						$location.path(sessionStorage.getItem('location') || "status");
				}
				else {
					if (appConstants.init > 0) {
						if ($location.path().indexOf("login") < 0)
							sessionStorage.setItem('location', $location.path() || "status");
						$location.path("login");
					}
				}
			},
			logout: function () {
				sessionStorage.clear();
				$http.defaults.headers.common['Authorization'] = null;
				$http.get(Misc.getServerPath() + "iaf/api/logout");
			}
		};
	}]);
