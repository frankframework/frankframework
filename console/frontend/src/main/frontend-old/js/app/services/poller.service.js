import { appModule } from "../app.module";

appModule.service('Poller', ['Api', 'appConstants', 'Debug', function (Api, appConstants, Debug) {
	var data = {};
	this.createPollerObject = function (uri, callback) {
		this.uri = uri;
		this.waiting = true;
		this.pollerInterval = appConstants["console.pollerInterval"];
		this.fired = 0;
		this.errorList = [];
		this.addError = function () {
			this.errorList.push({
				time: (new Date()).getTime(),
				fired: this.fired
			});
			if (this.errorList.length > 10)
				this.errorList.shift();
		};
		this.getLastError = function () {
			return this.errorList[this.errorList.length - 1];
		};
		this.ai = {
			list: [],
			avg: 0,
			push: function (obj) {
				this.list.push(obj);
				if (this.list.length == 5) {
					var tmp = 0;
					for (var i = this.list.length - 1; i >= 0; i--) {
						tmp += this.list[i];
					}
					this.avg = Math.round((tmp / this.list.length) / 100) * 100;
					this.list = [];
					return this.avg;
				}
			}
		};
		this.started = function () { return (this.poller) ? true : false; };
		this.stop = function () {
			if (!this.started()) return;

			this.ai.list = [];
			this.ai.avg = 0;
			if (this.waiting)
				clearTimeout(this.poller);
			else
				clearInterval(this.poller);
			this.waiting = true;
			delete this.poller;
		};
		this.fn = function (runOnce) {
			var runOnce = !!runOnce;
			var poller = data[uri];
			poller.fired++;
			Api.Get(uri, callback, function () {
				poller.addError();

				var e = 0;
				for (const x in poller.errorList) {
					var y = poller.errorList[x];
					if (poller.fired == y.fired || poller.fired - 1 == y.fired || poller.fired - 2 == y.fired)
						e++;
				}
				Debug.info("Encountered unhandled exception, poller[" + uri + "] eventId[" + poller.fired + "] retries[" + e + "]");
				if (e < 3) return;

				Debug.warn("Max retries reached. Stopping poller [" + uri + "]", poller);

				runOnce = true;
				data[uri].stop();
			}, { poller: true }).then(function () {
				if (runOnce) return;

				var p = data[uri];
				if (p && p.waiting)
					p.start();
			});
		};
		this.run = function () {
			this.fn(true);
		};
		this.start = function () {
			if (this.started() && !this.waiting) return;

			if (this.waiting) {
				var now = new Date().getTime();
				if (this.lastPolled) {
					var timeBetweenLastPolledAndNow = now - this.lastPolled;
					var interval = this.ai.push(timeBetweenLastPolledAndNow);
					if (interval > 0 && interval > this.pollerInterval) {
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
		this.setInterval = function (interval, restart) {
			var restart = (!restart || restart === false) ? false : true;
			Debug.info("Interval for " + this.uri + " changed to [" + interval + "] restart [" + restart + "]");
			this.pollerInterval = interval;
			if (restart)
				this.restart();
		};
		this.waitForResponse = function (bool) {
			this.stop();
			delete this.lastPolled;
			this.waiting = !!bool;
			if (bool != this.waiting)
				Debug.info("waitForResponse for " + this.uri + " changed to: " + bool);
			this.start();
		};
		this.restart = function () {
			this.stop();
			this.start();
		};
	},
		this.changeInterval = function (uri, interval) {
			data[uri].waitForResponse(true);
			data[uri].setInterval(interval, false);
		},
		this.add = function (uri, callback, autoStart, interval) {
			if (!data[uri]) {
				Debug.log("Adding new poller [" + uri + "] autoStart [" + !!autoStart + "] interval [" + interval + "]");
				var poller = new this.createPollerObject(uri, callback);
				data[uri] = poller;
				if (!!autoStart)
					poller.fn();
				if (interval && interval > 1500)
					poller.setInterval(interval);
				return poller;
			}
		},
		this.remove = function (uri) {
			if (data[uri]) {
				data[uri].stop();
				delete data[uri];
			}
		},
		this.get = function (uri) {
			return data[uri];
		},
		this.getAll = function () {
			var args = arguments || [];
			if (args.length > 0 && typeof args[0] == "function") {
				var callback = args[0];
				for (const x in data) {
					callback.apply(this, [data[x]]);
				}
			}
			return {
				changeInterval: function (interval) {
					var i = interval || appConstants["console.pollerInterval"];
					for (const x in data)
						data[x].setInterval(i, false);
				},
				start: function () {
					Debug.info("starting all Pollers");
					for (const x in data)
						data[x].fn();
				},
				stop: function () {
					Debug.info("stopping all Pollers");
					for (const x in data)
						data[x].stop();
				},
				remove: function () {
					Debug.info("removing all Pollers");
					for (const x in data) {
						data[x].stop();
						delete data[x];
					}
					data = {};
				},
				list: function () {
					this.list = [];
					for (const uri in data) {
						this.list.push(uri);
					}
					return this.list;
				}
			};
		};
}]);
