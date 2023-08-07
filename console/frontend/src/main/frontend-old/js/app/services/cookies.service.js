import { appModule } from "../app.module";

appModule.service('Cookies', ['Debug', '$cookies', 'GDPR', '$rootScope', function (Debug, $cookies, GDPR, $rootScope) {
	this.cache = null;
	this.addToCache = function (key, value) {
		Debug.log("adding cookie[" + key + "] to cache");

		if (this.cache == null)
			this.cache = {};

		//If the same key is set twice, just overwrite the old setting
		this.cache[key] = value;
	};

	this.flushCache = function () {
		Debug.info("trying to save cookies from cache", this.cache);

		if (GDPR.allowFunctional() === true) { //Only run when explicitly set to true
			for (const c in this.cache) {
				this.set(c, this.cache[c]);
			}
			this.cache = null; //Clear the cache, free up memory :)
		}
	};

	//Runs everytime the GDPR settings update
	var Cookies = this;
	$rootScope.$on('GDPR', function () {
		Cookies.flushCache();
	});

	var date = new Date();
	date.setDate(date.getDate() + 7);
	this.options = {
		expires: date,
		path: '/'
	};
	this.get = function (key) {
		var val = null;
		if (this.cache != null) //Maybe a cookie has been set but the user has not accepted cookies?
			val = this.cache[key];
		if (val == null)
			val = $cookies.getObject(key);
		return val;
	};
	this.set = function (key, value) {
		if (GDPR.allowFunctional())
			$cookies.putObject(key, value, this.options); //Only actually set the cookie when allowed to
		else
			this.addToCache(key, value); //Cache the request while the user hasn't selected their preference or disallowed functional cookies
	};
	this.remove = function (key) {
		$cookies.remove(key, { path: '/' });
	};
	this.clear = function () {
		for (const key in $cookies.getAll()) {
			if (!key.startsWith("_"))
				this.remove(key);
		}
	};
}]);
