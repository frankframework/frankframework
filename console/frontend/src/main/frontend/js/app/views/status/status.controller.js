import { appModule } from "../../app.module";

appModule.controller('StatusCtrl', ['$scope', 'Hooks', 'Api', 'SweetAlert', 'Poller', '$filter', '$state', 'Misc', '$anchorScroll', '$location', '$http',
	function ($scope, Hooks, Api, SweetAlert, Poller, $filter, $state, Misc, $anchorScroll, $location, $http) {

		var hash = $location.hash();
		var adapterName = $state.params.adapter;
		if (adapterName == "" && hash != "") { //If the adapter param hasn't explicitly been set
			adapterName = hash;
		} else {
			$location.hash(adapterName);
		}

		$scope.showContent = function (adapter) {
			if (adapter.status == "stopped") {
				return true;
			} else if (adapterName != "" && adapter.name == adapterName) {
				$anchorScroll();
				return true;
			} else {
				return false;
			}
		};

		this.filter = {
			"started": true,
			"stopped": true,
			"warning": true
		};
		$scope.filter = this.filter;
		$scope.applyFilter = function (filter) {
			$scope.filter = filter;
			$scope.updateQueryParams();
		};
		if ($state.params.filter != "") {
			var filter = $state.params.filter.split("+");
			for (const f in $scope.filter) {
				$scope.filter[f] = (filter.indexOf(f) > -1);
			}
		}
		$scope.searchText = "";
		if ($state.params.search != "") {
			$scope.searchText = $state.params.search;
		}

		$scope.selectedConfiguration = "All";

		$scope.updateQueryParams = function () {
			var filterStr = [];
			for (const f in $scope.filter) {
				if ($scope.filter[f])
					filterStr.push(f);
			}
			var transitionObj = {};
			transitionObj.filter = filterStr.join("+");
			if ($scope.selectedConfiguration != "All")
				transitionObj.configuration = $scope.selectedConfiguration;
			if ($scope.searchText.length > 0)
				transitionObj.search = $scope.searchText;

			$state.transitionTo('pages.status', transitionObj, { notify: false, reload: false });
		};

		$scope.collapseAll = function () {
			$(".adapters").each(function (i, e) {
				var a = $(e).find("div.ibox-title");
				angular.element(a).scope().showContent = false;
			});
		};
		$scope.expandAll = function () {
			$(".adapters").each(function (i, e) {
				setTimeout(function () {
					var a = $(e).find("div.ibox-title");
					angular.element(a).scope().showContent = true;
				}, i * 10);
			});
		};
		$scope.stopAll = function () {
			let compiledAdapterList = Array();
			let adapters = $filter('configurationFilter')($scope.adapters, $scope);
			for (const adapter in adapters) {
				let configuration = adapters[adapter].configuration;
				compiledAdapterList.push(configuration + "/" + adapter);
			}
			Api.Put("adapters", { "action": "stop", "adapters": compiledAdapterList });
		};
		$scope.startAll = function () {
			let compiledAdapterList = Array();
			let adapters = $filter('configurationFilter')($scope.adapters, $scope);
			for (const adapter in adapters) {
				let configuration = adapters[adapter].configuration;
				compiledAdapterList.push(configuration + "/" + adapter);
			}
			Api.Put("adapters", { "action": "start", "adapters": compiledAdapterList });
		};
		$scope.reloadConfiguration = function () {
			if ($scope.selectedConfiguration == "All") return;

			$scope.isConfigReloading[$scope.selectedConfiguration] = true;

			Poller.getAll().stop();
			Api.Put("configurations/" + $scope.selectedConfiguration, { "action": "reload" }, function () {
				startPollingForConfigurationStateChanges(function () {
					Poller.getAll().start();
				});
			});
		};
		$scope.reloading = false;
		$scope.fullReload = function () {
			$scope.reloading = true;
			Poller.getAll().stop();
			Api.Put("configurations", { "action": "reload" }, function () {
				$scope.reloading = false;
				startPollingForConfigurationStateChanges(function () {
					Poller.getAll().start();
				});
			});
		};

		function startPollingForConfigurationStateChanges(callback) {
			Poller.add("server/configurations", function (configurations) {
				$scope.updateConfigurations(configurations);

				var ready = true;
				for (var i in configurations) {
					var config = configurations[i];
					//When all configurations are in state STARTED or in state STOPPED with an exception, remove the poller
					if (config.state != "STARTED" && !(config.state == "STOPPED" && config.exception != null)) {
						ready = false;
						break;
					}
				}
				if (ready) { //Remove poller once all states are STARTED
					Poller.remove("server/configurations");
					if (callback != null && typeof callback == "function") callback();
				}
			}, true);
		}

		$scope.showReferences = function () {
			window.open($scope.configurationFlowDiagram);
		};
		$scope.configurationFlowDiagram = null;
		$scope.updateConfigurationFlowDiagram = function (configurationName) {
			var url = Misc.getServerPath() + 'iaf/api/configurations/';
			if (configurationName == "All") {
				url += "?flow=true";
			} else {
				url += configurationName + "/flow";
			}
			$http.get(url).then(function (data) {
				let status = (data && data.status) ? data.status : 204;
				if (status == 200) {
					$scope.configurationFlowDiagram = url;
				}
			});
		}

		$scope.$on('appConstants', function () {
			$scope.updateConfigurationFlowDiagram($scope.selectedConfiguration);
		});

		$scope.isConfigStubbed = {};
		$scope.isConfigReloading = {};
		$scope.check4StubbedConfigs = function () {
			for (var i in $scope.configurations) {
				var config = $scope.configurations[i];
				$scope.isConfigStubbed[config.name] = config.stubbed;
				$scope.isConfigReloading[config.name] = config.state == "STARTING" || config.state == "STOPPING"; //Assume reloading when in state STARTING (LOADING) or in state STOPPING (UNLOADING)
			}
		};
		$scope.$watch('configurations', $scope.check4StubbedConfigs);

		$scope.changeConfiguration = function (name) {
			$scope.selectedConfiguration = name;
			$scope.updateAdapterSummary(name);
			$scope.updateQueryParams();
			$scope.updateConfigurationFlowDiagram(name);
		};
		if ($state.params.configuration != "All")
			$scope.changeConfiguration($state.params.configuration);


		$scope.startAdapter = function (adapter) {
			adapter.state = 'starting';
			Api.Put("configurations/" + adapter.configuration + "/adapters/" + Misc.escapeURL(adapter.name), { "action": "start" });
		};
		$scope.stopAdapter = function (adapter) {
			adapter.state = 'stopping';
			Api.Put("configurations/" + adapter.configuration + "/adapters/" + Misc.escapeURL(adapter.name), { "action": "stop" });
		};
		$scope.startReceiver = function (adapter, receiver) {
			receiver.state = 'loading';
			Api.Put("configurations/" + adapter.configuration + "/adapters/" + Misc.escapeURL(adapter.name) + "/receivers/" + Misc.escapeURL(receiver.name), { "action": "start" });
		};
		$scope.stopReceiver = function (adapter, receiver) {
			receiver.state = 'loading';
			Api.Put("configurations/" + adapter.configuration + "/adapters/" + Misc.escapeURL(adapter.name) + "/receivers/" + Misc.escapeURL(receiver.name), { "action": "stop" });
		};
		$scope.addThread = function (adapter, receiver) {
			receiver.state = 'loading';
			Api.Put("configurations/" + adapter.configuration + "/adapters/" + Misc.escapeURL(adapter.name) + "/receivers/" + Misc.escapeURL(receiver.name), { "action": "incthread" });
		};
		$scope.removeThread = function (adapter, receiver) {
			receiver.state = 'loading';
			Api.Put("configurations/" + adapter.configuration + "/adapters/" + Misc.escapeURL(adapter.name) + "/receivers/" + Misc.escapeURL(receiver.name), { "action": "decthread" });
		};

	}]);
