/**
 * nashorn-promise
 *
 * @author hidekatsu.izuno@gmail.com (Hidekatsu Izuno)
 * @license MIT License
 */

(function(global) {
	'use strict';

	if (global.Promise === undefined) {
		var JCompletableFuture = Java.type('java.util.concurrent.CompletableFuture');
		var JCompleteFutureArray = Java.type('java.util.concurrent.CompletableFuture[]');
		var JPromiseException = Java.type('nl.nn.adapterframework.extensions.javascript.PromiseException');

		var Promise = function(resolver, promises) {
			var that = this;
			if (resolver instanceof JCompletableFuture) {
				that._future = resolver;
				that._promises = promises;
			} else {
				var func = Java.synchronized(function() {
					var status, result;
					(0, resolver)(function(value) {
						status = 'fulfilled';
						result = value;
					}, function(reason) {
						status = 'rejected';
						result = reason;
					});
					if (status == 'fulfilled') {
						return {
							result: result
						};
					} else if (status == 'rejected') {
						throw new JPromiseException(result);
					}
				}, global);
				if (Promise._pool) {
					that._future = JCompletableFuture.supplyAsync(func, Promise._pool);
				} else {
					that._future = JCompletableFuture.supplyAsync(func);
				}
			}
		};

		Promise.all = function(array) {
			if (array == null || array.length == null) {
				return Promise.reject(new TypeError('array is not iterable'))
			}
			if (array.length == 0) {
				return Promise.resolve([]);
			}

			var futures = new JCompleteFutureArray(array.length);
			var promises = [];
			for (var i = 0; i < array.length; i++) {
				if (array[i] instanceof Promise) {
					promises[i] = array[i];
				} else {
					promises[i] = Promise.resolve(array[i]);
				}
				futures[i] = promises[i]._future;
			}
			return new Promise(JCompletableFuture.allOf(futures), promises);
		};

		Promise.race = function(array) {
			if (array == null || array.length == null) {
				return Promise.reject(new TypeError('array is not iterable'))
			}
			if (array.length == 0) {
				return Promise.resolve([]);
			}

			var futures = new JCompleteFutureArray(array.length);
			for (var i = 0; i < array.length; i++) {
				if (array[i] instanceof Promise) {
					futures[i] = array[i]._future;
				} else {
					futures[i] = Promise.resolve(array[i])._future;
				}
			}
			return new Promise(JCompletableFuture.anyOf(futures));
		};

		Promise.resolve = function(value) {
			if (value instanceof Promise) {
				return value;
			} else if (value != null
					&& (typeof value === 'function' || typeof value === 'object')
					&& typeof value.then === 'function') {
				return new Promise(function(fulfill, reject) {
					try {
						return {
							result: value.then(fulfill, reject)
						}
					} catch (e) {
						throw new JPromiseException(e);
					}
				});
			} else {
				return new Promise(JCompletableFuture.completedFuture({
					result: value
				}));
			}
		};

		Promise.reject = function(value) {
			return new Promise(function(fulfill, reject) {
				reject(value);
			});
		};

		Promise.prototype.then = function(onFulfillment, onRejection) {
			var that = this;
			return new Promise(that._future.handle(function(success, error) {
				if (success == null && error == null && that._promises != null) {
					var traverse = function(promise) {
						if (promise._promises != null) {
							var result = [];
							for (var i = 0; i < promise._promises.length; i++) {
								result[i] = traverse(promise._promises[i]);
							}
							return result;
						}
						return promise._future.get().result;
					};

					var result = [];
					for (var i = 0; i < that._promises.length; i++) {
						result[i] = traverse(that._promises[i]);
					}
					success = {
						result: result
					};
				}

				if (success != null) {
					if (typeof onFulfillment === 'function') {
						try {
							var value = success.result;
							while (value instanceof Promise) {
								value = value._future.get().result;
							}
							return {
								result: (0, onFulfillment)(value)
							};
						} catch (e) {
							throw new JPromiseException(e)
						}
					}
					return success;
				} else if (error != null) {
					var cerror = error;
					do {
						if (cerror instanceof JPromiseException) {
							error = cerror;
							break;
						}
					} while ((cerror = cerror.getCause()) != null);

					if (typeof onRejection === 'function') {
						try {
							var reason  = error;
							if (error instanceof JPromiseException) {
								reason = error.getResult();
							}

							return {
								result: (0, onRejection)(reason)
							};
						} catch (e) {
							throw new JPromiseException(e)
						}
					}
					throw error;
				}
			}));
		};

		Promise.prototype.catch = function(onRejection) {
			return this.then(null, onRejection);
		};

		global.Promise = Promise;
	}
})(Function('return this')());