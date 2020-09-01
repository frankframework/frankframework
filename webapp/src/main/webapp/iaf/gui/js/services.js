angular.module('iaf.beheerconsole')
	.service('Api', ['$http', 'appConstants', 'Misc', 'Session', 'Debug', function($http, appConstants, Misc, Session, Debug) {
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
			var defaultHttpOptions = { headers:{}, intercept: intercept };

			if(httpOptions) {
				//If httpOptions is TRUE, skip additional/custom settings, if it's an object, merge both objects
				if(typeof httpOptions == "object") {
					angular.merge(defaultHttpOptions, defaultHttpOptions, httpOptions);
					if(!httpOptions.poller) {
						Debug.log("Sending request to uri ["+uri+"] using HttpOptions ", defaultHttpOptions);
					}
				}
			} else if(etags.hasOwnProperty(uri)) { //If not explicitly disabled (httpOptions==false), check eTag
				var tag = etags[uri];
				defaultHttpOptions.headers['If-None-Match'] = tag;
			}

			return $http.get(buildURI(uri), defaultHttpOptions).then(function(response) {
				if(callback && typeof callback === 'function') {
					if(response.headers("etag")) {
						etags[uri] = response.headers("etag");
					}
					if(response.headers("allow")) {
						allowed[uri] = response.headers("allow");
					}
					callback(response.data);
				}
			}).catch(function(response){ errorException(response, error); });
		};

		this.Post = function () { // uri, object, callback, error, intercept 4xx errors
			var args = Array.prototype.slice.call(arguments);
			var uri = args.shift();
			var object = (args.shift() || {});
			var headers = {};
			if(object instanceof FormData) {
				headers = { 'Content-Type': undefined }; //Unset default contentType when posting formdata
			}
			var callback = args.shift();
			var error = args.shift();
			var intercept = args.shift();

			return $http.post(buildURI(uri), object, {
				headers: headers,
				transformRequest: angular.identity,
				intercept: intercept,
			}).then(function(response){
				if(callback && typeof callback === 'function') {
					etags[uri] = response.headers("etag");
					callback(response.data);
				}
			}).catch(function(response){ errorException(response, error); });
		};

		this.Put = function (uri, object, callback, error, intercept) {
			var headers = {};
			var data = {};
			if(object != null) {
				if(object instanceof FormData) {
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
			}).then(function(response){
				if(callback && typeof callback === 'function') {
					etags[uri] = response.headers("etag");
					callback(response.data);
				}
			}).catch(function(response){ errorException(response, error); });
		};

		this.Delete = function () { // uri, callback, error || uri, object, callback, error
			var args = Array.prototype.slice.call(arguments);
			var uri = args.shift();
			var request = {url:buildURI(uri), method: "delete", headers:{} };
			var callback;

			var object = args.shift(); // this can be a function or an object.
			if(object instanceof Function) { //we have a callback function, that means no object is present!
				callback = object; // set the callback method accordingly
			} else {
				if(object instanceof FormData) {
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

			return $http(request).then(function(response){
				if(callback && typeof callback === 'function') {
					etags[uri] = response.headers("etag");
					callback(response.data);
				}
			}).catch(function(response){ errorException(response, error); });
		};

		var errorException = function (response, callback) {
			if(response.status != 304) {
				var status = (response.status > 0) ? " " + response.status + " error" : "n unknown error";
				if(response.status == 404 || response.status == 500) {
					var config = response.config;
					var debug = " url["+config.url+"] method["+config.method+"]";
					if(config.data && config.data != "") debug += " data["+config.data+"]";
					Debug.warn("A" + status + " occurred, please notify a system administrator!" + '\n' + debug);
				}
				else{
					Debug.info("A" + status + " occured.", response);
				}

				if((response.status != 304) && (callback && typeof callback === 'function')) {
					callback(response.data, response.status, response.statusText);
				}
			}
		};

		//Getters
		this.errorException = errorException;
		this.absolutePath = absolutePath;
		this.etags = etags;

		this.flushCache = function() {
			etags = {};
		};

	}]).service('Poller', ['Api', 'appConstants', 'Debug', function(Api, appConstants, Debug) {
		var data = {};
		this.createPollerObject = function(uri, callback) {
			this.uri = uri;
			this.waiting = true;
			this.pollerInterval = appConstants["console.pollerInterval"];
			this.fired = 0;
			this.errorList = [];
			this.addError = function() {
				this.errorList.push({
					time: (new Date()).getTime(),
					fired: this.fired
				});
				if(this.errorList.length > 10)
					this.errorList.shift();
			};
			this.getLastError = function() {
				return this.errorList[this.errorList.length-1];
			};
			this.ai = {
				list: [],
				avg: 0,
				push: function(obj) {
					this.list.push(obj);
					if(this.list.length == 5) {
						var tmp = 0;
						for (var i = this.list.length - 1; i >= 0; i--) {
							tmp += this.list[i];
						}
						this.avg = Math.round((tmp / this.list.length) / 100 ) * 100;
						this.list = [];
						return this.avg;
					}
				}
			};
			this.started = function() {return (this.poller) ? true : false;};
			this.stop = function() {
				if(!this.started()) return;

				this.ai.list = [];
				this.ai.avg = 0;
				if(this.waiting)
					clearTimeout(this.poller);
				else
					clearInterval(this.poller);
				this.waiting = true;
				delete this.poller;
			};
			this.fn = function(runOnce) {
				var runOnce = !!runOnce;
				var poller = data[uri];
				poller.fired++;
				Api.Get(uri, callback, function() {
					poller.addError();

					var e = 0;
					for(x in poller.errorList) {
						var y = poller.errorList[x];
						if(poller.fired == y.fired || poller.fired-1 == y.fired || poller.fired-2 == y.fired)
							e++;
					}
					Debug.info("Encountered unhandled exception, poller["+uri+"] eventId["+poller.fired+"] retries["+e+"]");
					if(e < 3) return;

					Debug.warn("Max retries reached. Stopping poller ["+uri+"]", poller);

					runOnce = true;
					data[uri].stop();
				}, {poller:true}).then(function() {
					if(runOnce) return;

					var p = data[uri];
					if(p && p.waiting)
						p.start();
				});
			};
			this.run = function() {
				this.fn(true);
			};
			this.start = function() {
				if(this.started() && !this.waiting) return;

				if(this.waiting) {
					var now = new Date().getTime();
					if(this.lastPolled) {
						var timeBetweenLastPolledAndNow = now - this.lastPolled;
						var interval = this.ai.push(timeBetweenLastPolledAndNow);
						if(interval > 0 && interval > this.pollerInterval) {
							this.setInterval(interval, false);
							this.waitForResponse(false);
							return;
						}
					}
					this.poller = setTimeout(this.fn, this.pollerInterval);
					this.lastPolled = now;
				}
				else
					this.poller = setInterval(this.fn, this.pollerInterval);
			};
			this.setInterval = function(interval, restart) {
				var restart = (!restart || restart === false) ? false : true;
				Debug.info("Interval for " + this.uri + " changed to [" + interval + "] restart ["+restart+"]");
				this.pollerInterval = interval;
				if(restart)
					this.restart();
			};
			this.waitForResponse = function(bool) {
				this.stop();
				delete this.lastPolled;
				this.waiting = !!bool;
				if(bool != this.waiting)
					Debug.info("waitForResponse for " + this.uri + " changed to: " + bool);
				this.start();
			};
			this.restart = function() {
				this.stop();
				this.start();
			};
		},
		this.changeInterval = function(uri, interval) {
			data[uri].waitForResponse(true);
			data[uri].setInterval(interval, false);
		},
		this.add = function (uri, callback, autoStart, interval) {
			Debug.log("Adding new poller ["+uri+"] autoStart ["+!!autoStart+"] interval ["+interval+"]");
			var poller = new this.createPollerObject(uri, callback);
			data[uri] = poller;
			if(!!autoStart)
				poller.fn();
			if(interval && interval > 1500)
				poller.setInterval(interval);
			return poller;
		},
		this.remove = function (uri) {
			data[uri].stop();
			delete data[uri];
		},
		this.get = function (uri) {
			return data[uri];
		},
		this.getAll = function () {
			var args = arguments || [];
			if(args.length > 0 && typeof args[0] == "function") {
				var callback = args[0];
				for(x in data) {
					callback.apply(this, [data[x]]);
				}
			}
			return {
				changeInterval: function(interval) {
					var i = interval || appConstants["console.pollerInterval"];
					for(x in data)
						data[x].setInterval(i, false);
				},
				start: function() {
					Debug.info("starting all Pollers");
					for(x in data)
						data[x].fn();
				},
				stop: function() {
					Debug.info("stopping all Pollers");
					for(x in data)
						data[x].stop();
				},
				remove: function() {
					Debug.info("removing all Pollers");
					for(x in data) {
						data[x].stop();
						delete data[x];
					}
					data = {};
				},
				list: function () {
					this.list = [];
					for(uri in data) {
						this.list.push(uri);
					}
					return this.list;
				}
			};
		};
	}]).service('Notification', ['$rootScope', '$timeout', function($rootScope, $timeout) {
		Tinycon.setOptions({
			background: '#f03d25'
		});
		this.list = [];
		this.count = 0;
		this.add = function(icon, title, msg, fn) {
			var obj = {
				icon: icon,
				title: title,
				message: (msg) ? msg : false,
				fn: (fn) ? fn: false,
				time: new Date().getTime()
			};
			this.list.unshift(obj);
			obj.id = this.list.length;
			this.count++;

			Tinycon.setBubble(this.count);
		};
		this.get = function(id) {
			for (var i = 0; i < this.list.length; i++) {
				var notification = this.list[i];
				if(notification.id == id) {
					if(notification.fn) {
						$timeout(function(){
							notification.fn.apply(this, notification);
						}, 50);
					}
					return notification;
				}
			}

			return false;
		};
		this.resetCount = function() {
			Tinycon.setBubble(0);
			this.count = 0;
		};
		this.getCount = function() {
			return this.count;
		};
		this.getLatest = function(amount) {
			if(amount < 1) amount = 1;
			return this.list.slice(0, amount);
		};
	}]).service('Cookies', ['Debug', '$cookies', 'GDPR', '$rootScope', function(Debug, $cookies, GDPR, $rootScope) {
		this.cache = null;
		this.addToCache = function(key, value) {
			Debug.log("adding cookie["+key+"] to cache");

			if(this.cache == null)
				this.cache = {};

			//If the same key is set twice, just overwrite the old setting
			this.cache[key] = value;
		};

		this.flushCache = function() {
			Debug.info("trying to save cookies from cache", this.cache);

			if(GDPR.allowFunctional() === true) { //Only run when explicitly set to true
				for(c in this.cache) {
					this.set(c, this.cache[c]);
				}
				this.cache = null; //Clear the cache, free up memory :)
			}
		};

		//Runs everytime the GDPR settings update
		var Cookies = this;
		$rootScope.$on('GDPR', function() {
			Cookies.flushCache();
		});

		var date = new Date();
		date.setDate(date.getDate() + 7);
		this.options = {
			expires: date,
			path: '/'
		};
		this.get = function(key) {
			var val = null;
			if(this.cache != null) //Maybe a cookie has been set but the user has not accepted cookies?
				val = this.cache[key];
			if(val == null)
				val = $cookies.getObject(key);
			return val;
		};
		this.set = function(key, value) {
			if(GDPR.allowFunctional())
				$cookies.putObject(key, value, this.options); //Only actually set the cookie when allowed to
			else
				this.addToCache(key, value); //Cache the request while the user hasn't selected their preference or disallowed functional cookies
		};
		this.remove = function(key) {
			$cookies.remove(key, {path: '/'});
		};
		this.clear = function() {
			for(key in $cookies.getAll()) {
				if(!key.startsWith("_"))
					this.remove(key);
			}
		};
	}]).service('Session', ['Debug', function(Debug) {
		this.get = function(key) {
			//Debug.log(key, sessionStorage.getItem(key), sessionStorage.getItem(key) == null, sessionStorage.getItem(key) == "null");
			return JSON.parse(sessionStorage.getItem(key));
		};
		this.set = function(key, value) {
			sessionStorage.setItem(key, JSON.stringify(value));
		};
		this.remove = function(key) {
			sessionStorage.removeItem(key);
		};
		this.clear = function() {
			sessionStorage.clear();
		};
	}])
	.service('GDPR', ['$cookies', '$rootScope', 'Debug', function($cookies, $rootScope, Debug) {
		this.settings = null;
		this.defaults = {necessary: true, functional: false, analytical: false, personalization: false};
		var date = new Date();
		date.setFullYear(date.getFullYear() +10);

		this.cookieName = "_cookieSettings";
		this.options = {
			expires: date,
			path: '/'
		};

		this.showCookie = function() {
			this.getSettings();
			return this.settings == null;
		};

		this.getSettings = function() {
			if(this.settings == null) {
				var cookie = $cookies.getObject(this.cookieName);
				if(cookie != undefined) {
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
		this.allowFunctional = function() {
			return this.getSettings().functional;
		};
		this.allowAnalytical = function() {
			return this.getSettings().analytical;
		};
		this.allowPersonalization = function() {
			return this.getSettings().personalization;
		};
		this.setSettings = function(settings){
			this.settings = settings;
			$cookies.putObject(this.cookieName, settings, this.options);

			$rootScope.$broadcast('GDPR');
		};
	}])
	.service('gTag', ['Debug', 'GDPR', '$rootScope', function(Debug, GDPR, $rootScope) {
		window.dataLayer = window.dataLayer || [];
		this.trackingId = "";
		this.configured = false;

		//Push something to the dataLayer
		this.add = function() {
			if(this.configured)
				dataLayer.push(arguments);
		};
		//Set the gTag trackingId
		this.setTrackingId = function(id) {
			this.trackingId = id;
			this.configure();
		};
		//Setup the gTag service
		this.configure = function() {
			if(!GDPR.allowAnalytical()) {
				Debug.log("unable to configure gTag due to GDPR settings");
				window.dataLayer = [];
				this.configured = false;
				return ;
			}

			if(this.configured == false) {
				if(this.trackingId) {
					this.add('js', new Date());
					this.config({
						'send_page_view': false,
						'anonymize_ip': true,
						'custom_map': {
							'dimension1': 'application.version'
						}
					});
					this.configured = true;
					Debug.info("succesfully configured gTag with trackingId["+this.trackingId+"]");
				}
				else
					Debug.warn("unable to configure gTag, no trackingId specified");
			}
			else
				Debug.warn("can only configure gTag Analytics once");
		};

		//Not to confuse with configure, this method allows you to push gTag config events
		this.config = function(object) {
			if(typeof object == "object")
				this.add('config', this.trackingId, object);
		};
		//Push events to the dataLayer
		this.event = function(name, label, value, non_interaction) {
			this.add('event', name, {
				'event_label': label,
				'value': (value == undefined || value < 0) ? 1 : value,
				'non_interaction': (non_interaction == undefined || non_interaction == false) ? false : true
			});
		};

		var gTag = this;
		$rootScope.$on('GDPR', function() {
			gTag.configure();
		});

		$rootScope.$on("$stateChangeStart", function(_, state) {
			Debug.log("triggered state change to ["+state.name+"]");
			var url = state.url;
			if(url && url.indexOf("?") > 0)
				url = url.substring(0, url.indexOf("?"));

			if(state.data && state.data.pageTitle) {
				gTag.config({
					'page_path': url,
					'page_title': state.data.pageTitle
				});
			}
		});
	}])
	.service('Debug', function() {
		var level = 0; //ERROR
		var levelEnums = ["ERROR", "WARN", "INFO", "DEBUG"];
		var inGroup = false;
		this.getLevel = function() {
			return level;
		};
		this.setLevel = function(l) {
			l = Math.min(3, Math.max(0, l));
			if(l == level) return;
			console.info(this.head() + " Setting LOG level to ["+levelEnums[l]+"]");
			level = l;
		};
		this.head = function(level) {
			var d = new Date();
			var date = ('0' + d.getUTCDate()).slice(-2)+"-"+('0' + d.getUTCMonth()).slice(-2)+"-"+d.getUTCFullYear();
			date += " "+('0' + d.getSeconds()).slice(-2)+":"+('0' + d.getMinutes()).slice(-2)+":"+('0' + d.getHours()).slice(-2);
			if(level != undefined)
				return date + " ["+levelEnums[level]+"] -";
			else
				return date + " -";
		};
		this.log = function() {
			if(level < 3) return;
			var args = arguments || [];
			var func = window.console.log;
			if(!inGroup)
				Array.prototype.unshift.call(args, this.head(3));
			try {
				func.apply(window.console, args);
			} catch (e) {
				for(var a in args)
					console.log(args[a]);
			};
		};
		this.group = function() {
			var args = arguments || [];
			var title = Array.prototype.shift.call(args);
			inGroup = true;
			window.console.group(this.head() + " " + title);

			if(args.length > 0) { //Loop through args and close group after...
				for(var a in args)
					console.log(args[a]);
				this.groupEnd();
			}
		};
		this.groupEnd = function() {
			inGroup = false;
			window.console.groupEnd();
		};
		this.info = function() {
			if(level < 2) return;
			var args = arguments || [];
			var func = window.console.info;
			if(!inGroup)
				Array.prototype.unshift.call(args, this.head(2));
			try {
				func.apply(window.console, args);
			} catch (e) {
				for(var a in args)
					console.info(args[a]);
			};
		};
		this.warn = function(a) {
			if(level < 1) return;
			var args = arguments || [];
			var func = window.console.warn;
			if(!inGroup)
				Array.prototype.unshift.call(args, this.head(1));
			try {
				func.apply(window.console, args);
			} catch (e) {
				for(var a in args)
					console.warn(args[a]);
			};
		};
		this.error = function(a) {
			var args = arguments || [];
			var func = window.console.error;
			if(!inGroup)
				Array.prototype.unshift.call(args, this.head(0));
			try {
				func.apply(window.console, args);
			} catch (e) {
				for(var a in args)
					console.error(args[a]);
			};
		};
	}).service('SweetAlert', ['Debug', function(Debug) {
		this.defaultSettings = {
//			confirmButtonColor: "#449d44"
		};
		this.defaults = function() {
			var args = arguments || [];
			var options = angular.copy(this.defaultSettings);

			if(args.length == 0 || args.length > 2)
				Debug.error("Invalid argument length specified for SweetAlert.");

			//expects (String, String) or (JsonObject, Function)
			if(typeof args[0] == "object") {
				angular.merge(options, options, args[0]);
				if(args.length == 2 && typeof args[1] == "function") {
					options.callback = args[1];
				}
			} else if(typeof args[0] == "string") {
				options.title = args[0];
				if(args.length == 2 && typeof args[1] == "string") {
					options.text = args[1];
				}
			}

			return options; //var [options, callback] = this.defaults.apply(this, arguments);
		};
		this.Input = function() {
			var options = this.defaults.apply(this, arguments);
			if(options.input == undefined)
				options.input = "text";
			options.showCancelButton = true;
			return swal(options);
		};
		this.Confirm = function() { //(JsonObject, Callback)-> returns boolean
			var options = {
				title: "Are you sure?",
				showCancelButton: true,
			};
			angular.merge(options, options, this.defaults.apply(this, arguments));
			return swal(options, options.callback);
		};
		this.Info = function() {
			var options = {};
			angular.merge(options, {type: "info"}, this.defaults.apply(this, arguments));
			return swal(options, options.callback);
		};
		this.Warning = function() {
			var options = {};
			angular.merge(options, {type: "warning"}, this.defaults.apply(this, arguments));
			return swal(options, options.callback);
		};
		this.Error = function() {
			var options = {};
			angular.merge(options, {type: "error"}, this.defaults.apply(this, arguments));
			return swal(options, options.callback);
		};
		this.Success = function() {
			var options = {};
			angular.merge(options, {type: "success"}, this.defaults.apply(this, arguments));
			return swal(options, options.callback);
		};
	}]).service('Hooks', ['$rootScope', '$timeout', function($rootScope, $timeout) {
		this.call = function() {
			$rootScope.callHook.apply(this, arguments);
			//$rootScope.$broadcast.apply(this, arguments);
		};
		this.register = function() {
			$rootScope.registerHook.apply(this, arguments);
			//$rootScope.$on.apply(this, arguments);
		};
	}]).run(function($rootScope, $timeout) {
		$rootScope.hooks = [];

		$rootScope.callHook = function() {
			var args = Array.prototype.slice.call(arguments);
			var name = args.shift();
			//when this is called execute:
			$timeout( function () {
				if($rootScope.hooks.hasOwnProperty(name)) {
					var hooks = $rootScope.hooks[name];
					for(id in hooks) {
						hooks[id].apply(this, args);
						if(id == "once") {
							$rootScope.removeHook(name, id);
						}
					}
				}
				/*
				else {
					console.warn("Hook: '" + name + "' does not exist!");
				}*/
			}, 50);
		};
		$rootScope.registerHook = function() {
			var args = Array.prototype.slice.call(arguments);
			var name = args.shift();
			var id = 0;
			if(name.indexOf(":") > -1) {
				id = name.substring(name.indexOf(":")+1);
				name = name.substring(0, name.indexOf(":"));
			}
			var callback = args.shift();
			if(!$rootScope.hooks.hasOwnProperty(name))
				$rootScope.hooks[name] = [];

			if($rootScope.hooks[name].hasOwnProperty(id)) {
				console.warn("Tried to redefine the same hook twice...");
			}
			else {
				$rootScope.hooks[name][id] = callback;
			}
		};
		$rootScope.removeHook = function(name, id) {
			if(name != null && id != null)
				delete $rootScope.hooks[name][id];
		};
	}).filter('ucfirst', function() {
		return function(s) {
			return (angular.isString(s) && s.length > 0) ? s[0].toUpperCase() + s.substr(1).toLowerCase() : s;
		};
	}).filter('truncate', function() {
		return function(input, length) {
			if(input.length > length) {
				return input.substring(0, length) + "... ("+(input.length - length)+" characters more)";
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
			for(key in format) {
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
	}).service('Misc', ['appConstants', function(appConstants) {
		this.getServerPath = function() {
			var absolutePath = appConstants.server;
			if(absolutePath && absolutePath.slice(-1) != "/") absolutePath += "/";
			return absolutePath;
		};
		this.isMobile = function() {
			return ( navigator.userAgent.match(/Android/i)
				|| navigator.userAgent.match(/webOS/i)
				|| navigator.userAgent.match(/iPhone/i)
				|| navigator.userAgent.match(/iPad/i)
				|| navigator.userAgent.match(/iPod/i)
				|| navigator.userAgent.match(/BlackBerry/i)
				|| navigator.userAgent.match(/Windows Phone/i)
			) ? true : false;
		};
		this.compare_version = function(v1, v2, operator) {
			// See for more info: http://locutus.io/php/info/version_compare/

			var i, x, compare = 0;
			var vm = {
				'dev': -6,
				'alpha': -5,
				'a': -5,
				'beta': -4,
				'b': -4,
				'RC': -3,
				'rc': -3,
				'#': -2,
				'p': 1,
				'pl': 1
			};

			var _prepVersion = function (v) {
				v = ('' + v).replace(/[_\-+]/g, '.');
				v = v.replace(/([^.\d]+)/g, '.$1.').replace(/\.{2,}/g, '.');
				return (!v.length ? [-8] : v.split('.'));
			};
			var _numVersion = function (v) {
				return !v ? 0 : (isNaN(v) ? vm[v] || -7 : parseInt(v, 10));
			};

			v1 = _prepVersion(v1);
			v2 = _prepVersion(v2);
			x = Math.max(v1.length, v2.length);
			for (i = 0; i < x; i++) {
				if (v1[i] === v2[i]) {
					continue;
				}
				v1[i] = _numVersion(v1[i]);
				v2[i] = _numVersion(v2[i]);
				if (v1[i] < v2[i]) {
					compare = -1;
					break;
				} else if (v1[i] > v2[i]) {
					compare = 1;
					break;
				}
			}
			if (!operator) {
				return compare;
			}

			switch (operator) {
				case '>':
				case 'gt':
					return (compare > 0);
				case '>=':
				case 'ge':
					return (compare >= 0);
				case '<=':
				case 'le':
					return (compare <= 0);
				case '===':
				case '=':
				case 'eq':
					return (compare === 0);
				case '<>':
				case '!==':
				case 'ne':
					return (compare !== 0);
				case '':
				case '<':
				case 'lt':
					return (compare < 0);
				default:
					return null;
			}
		};
	}]).service('Alert', ['$timeout', 'Session', function($timeout, Session) {
		this.add = function(level, message, non_repeditive) {
			if(non_repeditive === true)
				if(this.checkIfExists(message))
					return;

			var type;
			switch(level) {
				case "info":
				case 1:
					type = "fa fa-info";
				break;
				case "warning":
				case 2:
					type = "fa fa-warning";
				break;
				case "severe":
				case 3:
					type = "fa fa-times";
				break;
				default:
					type = "fa fa-info";
				break;
			}
			var list = this.get(true);
			var obj = {
				type: type,
				message: message,
				time: new Date().getTime()
			};
			list.unshift(obj);
			obj.id = list.length;
			Session.set("Alert", list);
			//sessionStorage.setItem("Alert", JSON.stringify(list));
		};
		this.get = function(preserveList) {
			//var list = JSON.parse(sessionStorage.getItem("Alert"));
			var list = Session.get("Alert");
			if(preserveList == undefined) Session.set("Alert", []); //sessionStorage.setItem("Alert", JSON.stringify([])); //Clear after retreival
			return (list != null) ? list : [];
		};
		this.getCount = function() {
			return this.get(true).length || 0;
		};
		this.checkIfExists = function(message) {
			var list = this.get(true);
			if(list.length > 0) {
				for (var i = 0; i < list.length; i++) {
					if(list[i].message == message) {
						return true;
					}
				}
			}
			return false;
		};
	}]).service('Toastr', ['toaster', function(toaster) {
		this.error = function(title, text) {
			var options = {type: 'error', title: title, body: text};
			if (angular.isObject(title)) {
				angular.merge(options, options, title);
			}
			toaster.pop(options);
		};
		this.success = function(title, text) {
			var options = {type: 'success', title: title, body: text};
			if (angular.isObject(title)) {
				angular.merge(options, options, title);
			}
			toaster.pop(options);
		};
	}]).config(['$httpProvider', function($httpProvider) {
		$httpProvider.interceptors.push(['appConstants', '$q', 'Misc', 'Toastr', '$location', function(appConstants, $q, Misc, Toastr, $location) {
			var errorCount = 0;
			return {
				request: function(config) {
					if (config.url.indexOf('views') !== -1 && ff_version != null) {
						config.url = config.url + '?v=' + ff_version;
					}
					return config;
				},
				responseError: function(rejection) {
					if(rejection.config) { //It should always have a config object, but just in case!
						if(rejection.config.url && rejection.config.url.indexOf(Misc.getServerPath()) < 0) return $q.reject(rejection); //Don't capture non-api requests

						switch (rejection.status) {
							case -1:
								if(appConstants.init == 1) {
									if(rejection.config.headers["Authorization"] != undefined) {
										console.warn("Authorization error");
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
											onHideCallback: function() {
												window.location.reload();
											}
										});
									}
								}
								break;
							case 401:
								sessionStorage.clear();
								$location.path("login");
								break;
							case 403:
								Toastr.error("Forbidden", "You do not have the permissions to complete this operation");
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