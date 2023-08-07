import { appModule } from "../app.module";

appModule.service('Session', ['Debug', function (Debug) {
	this.get = function (key) {
		//Debug.log(key, sessionStorage.getItem(key), sessionStorage.getItem(key) == null, sessionStorage.getItem(key) == "null");
		return JSON.parse(sessionStorage.getItem(key));
	};
	this.set = function (key, value) {
		sessionStorage.setItem(key, JSON.stringify(value));
	};
	this.remove = function (key) {
		sessionStorage.removeItem(key);
	};
	this.clear = function () {
		sessionStorage.clear();
	};
}]);
