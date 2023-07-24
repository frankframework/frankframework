import { appModule } from "../app.module";

appModule.service('GDPR', ['$cookies', '$rootScope', 'Debug', function ($cookies, $rootScope, Debug) {
	this.settings = null;
	this.defaults = { necessary: true, functional: true, personalization: true };
	var date = new Date();
	date.setFullYear(date.getFullYear() + 10);

	this.cookieName = "_cookieSettings";
	this.options = {
		expires: date,
		path: '/'
	};

	this.showCookie = function () {
		this.getSettings();
		return this.settings == null;
	};

	this.getSettings = function () {
		if (this.settings == null) {
			var cookie = $cookies.getObject(this.cookieName);
			if (cookie != undefined) {
				Debug.log("fetch cookie with GDPR settings", cookie);
				this.settings = cookie;

				//Extend the cookie lifetime by another 10 years
				$cookies.putObject(this.cookieName, cookie, this.options);
			}
			else {
				Debug.log("unable to find GDPR settings, falling back to defaults", this.defaults);
				return this.defaults;
			}

			Debug.info("set GDPR settings to", this.settings);
		}
		return this.settings;
	};
	this.allowFunctional = function () {
		return this.getSettings().functional;
	};
	this.allowPersonalization = function () {
		return this.getSettings().personalization;
	};
	this.setSettings = function (settings) {
		this.settings = settings;
		$cookies.putObject(this.cookieName, settings, this.options);

		$rootScope.$broadcast('GDPR');
	};
}]);
