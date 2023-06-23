import { appModule } from "../../../app.module";

appModule.controller('EditMonitorsCtrl', ['$scope', 'Api', '$state', function ($scope, Api, $state) {
	$scope.loading = true;

	$scope.$on('loading', function () {
		$scope.loading = false;
	});

	$scope.selectedConfiguration = null;
	$scope.monitor = "";
	$scope.events = "";
	$scope.severities = [];
	$scope.triggerId = "";
	$scope.trigger = {
		type: "Alarm",
		filter: "none",
		events: [],
	}
	var url;
	if ($state.params.configuration == "" || $state.params.monitor == "") {
		$state.go('pages.monitors');
	} else {
		$scope.selectedConfiguration = $state.params.configuration;
		$scope.monitor = $state.params.monitor;
		$scope.triggerId = $state.params.trigger || "";
		url = "configurations/" + $scope.selectedConfiguration + "/monitors/" + $scope.monitor + "/triggers/" + $scope.triggerId;
		Api.Get(url, function (data) {
			$.extend($scope, data);
			calculateEventSources();
			if (data.trigger && data.trigger.sources) {
				var sources = data.trigger.sources;
				$scope.trigger.sources = [];
				$scope.trigger.adapters = [];
				for (const adapter in sources) {
					if (data.trigger.filter == "SOURCE") {
						for (const i in sources[adapter]) {
							$scope.trigger.sources.push(adapter + "$$" + sources[adapter][i]);
						}
					} else {
						$scope.trigger.adapters.push(adapter);
					}
				}
			}
		}, function () {
			$state.go('pages.monitors', $state.params);
		});
	}

	$scope.getAdaptersForEvents = function (events) {
		if (!events) return [];

		var adapters = [];
		for (const eventName in $scope.events) {
			if (events.indexOf(eventName) > -1) {
				let sourceList = $scope.events[eventName].sources;
				adapters = adapters.concat(Object.keys(sourceList));
			}
		}
		return Array.from(new Set(adapters));
	}
	$scope.eventSources = [];
	function calculateEventSources() {
		for (const eventCode in $scope.events) {
			var retVal = [];
			var eventSources = $scope.events[eventCode].sources;
			for (const adapter in eventSources) {
				for (const i in eventSources[adapter]) {
					retVal.push({ adapter: adapter, source: eventSources[adapter][i] });
				}
			}
			$scope.eventSources[eventCode] = retVal;
		}
	}
	$scope.getSourceForEvents = function (events) {
		var retval = [];
		for (const i in events) {
			var eventCode = events[i];
			retval = retval.concat($scope.eventSources[eventCode]);
		}
		return retval;
	}

	$scope.submit = function (trigger) {
		if (trigger.filter == "ADAPTER") {
			delete trigger.sources;
		} else if (trigger.filter == "SOURCE") {
			delete trigger.adapters;
			var sources = trigger.sources;
			trigger.sources = {};
			for (const i in sources) {
				var s = sources[i].split("$$");
				var adapter = s[0];
				var source = s[1];
				if (!trigger.sources[adapter]) trigger.sources[adapter] = [];
				trigger.sources[adapter].push(source);
			}
		}
		if ($scope.triggerId && $scope.triggerId > -1) {
			Api.Put(url, trigger, function (returnData) {
				$state.go('pages.monitors', $state.params);
			});
		} else {
			Api.Post(url, JSON.stringify(trigger), function (returnData) {
				$state.go('pages.monitors', $state.params);
			});
		}
	}
}]);
