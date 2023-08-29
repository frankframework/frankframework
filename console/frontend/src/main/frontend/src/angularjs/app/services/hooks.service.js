import { appModule } from "../app.module";

appModule.service('Hooks', ['$rootScope', '$timeout', function ($rootScope, $timeout) {
	this.call = function () {
		$rootScope.callHook.apply(this, arguments);
	};
	this.register = function () {
		$rootScope.registerHook.apply(this, arguments);
		//$rootScope.$on.apply(this, arguments);
	};
}]).run(function ($rootScope, $timeout) {
	$rootScope.hooks = [];

	$rootScope.callHook = function () {
		var args = Array.prototype.slice.call(arguments);
		var name = args.shift();
		//when this is called execute:
		$timeout(function () {
			if ($rootScope.hooks.hasOwnProperty(name)) {
				var hooks = $rootScope.hooks[name];
				for (const id in hooks) {
					hooks[id].apply(this, args);
					if (id == "once") {
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
	$rootScope.registerHook = function () {
		var args = Array.prototype.slice.call(arguments);
		var name = args.shift();
		var id = 0;
		if (name.indexOf(":") > -1) {
			id = name.substring(name.indexOf(":") + 1);
			name = name.substring(0, name.indexOf(":"));
		}
		var callback = args.shift();
		if (!$rootScope.hooks.hasOwnProperty(name))
			$rootScope.hooks[name] = [];

		if ($rootScope.hooks[name].hasOwnProperty(id)) {
			console.warn("Tried to redefine the same hook twice...");
		}
		else {
			$rootScope.hooks[name][id] = callback;
		}
	};
	$rootScope.removeHook = function (name, id) {
		if (name != null && id != null)
			delete $rootScope.hooks[name][id];
	};
});
