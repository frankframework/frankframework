import { appModule } from "../../app.module";

appModule.controller('ShowMonitorsCtrl', ['$scope', 'Api', '$state', 'Misc', '$rootScope', function ($scope, Api, $state, Misc, $rootScope) {

	$scope.selectedConfiguration = null;
	$scope.monitors = [];
	$scope.destinations = [];
	$scope.eventTypes = [];

	$rootScope.$watch('configurations', function () { $scope.configurations = $rootScope.configurations; });

	$scope.changeConfiguration = function (name) {
		$scope.selectedConfiguration = name;

		if ($state.params.configuration == "" || $state.params.configuration != name) { //Update the URL
			$state.transitionTo('pages.monitors', { configuration: name }, { notify: false, reload: false });
		}

		update();
	};

	$scope.totalRaised = 0;
	function update() {
		Api.Get("configurations/" + $scope.selectedConfiguration + "/monitors", function (data) {
			$.extend($scope, data);

			$scope.totalRaised = 0;
			for (const i in $scope.monitors) {
				if ($scope.monitors[i].raised) $scope.totalRaised++;
				var monitor = $scope.monitors[i];
				monitor.activeDestinations = [];
				for (const j in $scope.destinations) {
					var destination = $scope.destinations[j];
					monitor.activeDestinations[destination] = (monitor.destinations.indexOf(destination) > -1);
				}
			}
		});
	}

	//Wait for the 'configurations' field to be populated to change the monitoring page
	$scope.$watch('configurations', function (configs) {
		if (configs) {
			var configName = $state.params.configuration; //See if the configuration query param is populated
			if (!configName) configName = configs[0].name; //Fall back to the first configuration
			$scope.changeConfiguration(configName); //Update the view
		}
	});

	function getUrl(monitor, trigger) {
		var url = "configurations/" + $scope.selectedConfiguration + "/monitors/" + monitor.name;
		if (trigger != undefined && trigger != "") url += "/triggers/" + trigger.id;
		return url;
	}

	$scope.raise = function (monitor) {
		Api.Put(getUrl(monitor), { action: "raise" }, function () {
			update();
		});
	}
	$scope.clear = function (monitor) {
		Api.Put(getUrl(monitor), { action: "clear" }, function () {
			update();
		});
	}
	$scope.edit = function (monitor) {
		var destinations = [];
		for (const dest in monitor.activeDestinations) {
			if (monitor.activeDestinations[dest]) {
				destinations.push(dest);
			}
		}
		Api.Put(getUrl(monitor), { action: "edit", name: monitor.displayName, type: monitor.type, destinations: destinations }, function () {
			update();
		});
	}
	$scope.deleteMonitor = function (monitor) {
		Api.Delete(getUrl(monitor), function () {
			update();
		});
	}

	$scope.deleteTrigger = function (monitor, trigger) {
		Api.Delete(getUrl(monitor, trigger), function () {
			update();
		});
	}

	$scope.downloadXML = function (monitorName) {
		var url = Misc.getServerPath() + "iaf/api/configurations/" + $scope.selectedConfiguration + "/monitors";
		if (monitorName) {
			url += "/" + monitorName;
		}
		window.open(url + "?xml=true", "_blank");
	}
}]);
