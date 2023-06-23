import { appModule } from "../../app.module";

appModule.controller('EnvironmentVariablesCtrl', ['$scope', 'Api', 'appConstants', '$rootScope', function ($scope, Api, appConstants, $rootScope) {
	$scope.variables = {};
	$scope.searchFilter = "";

	$scope.selectedConfiguration = null;
	$scope.changeConfiguration = function (name) {
		$scope.selectedConfiguration = name;
		$scope.configProperties = $scope.appConstants[name];
	};

	$rootScope.$watch('configurations', function () { $scope.configurations = $rootScope.configurations; });

	$scope.configProperties = [];
	$scope.environmentProperties = [];
	$scope.systemProperties = [];
	$scope.appConstants = [];
	Api.Get("environmentvariables", function (data) {
		var instanceName = null;
		for (var configName in data["Application Constants"]) {
			$scope.appConstants[configName] = convertPropertiesToArray(data["Application Constants"][configName]);
			if (instanceName == null) {
				instanceName = data["Application Constants"][configName]["instance.name"];
			}
		}
		$scope.changeConfiguration("All");
		$scope.environmentProperties = convertPropertiesToArray(data["Environment Variables"]);
		$scope.systemProperties = convertPropertiesToArray(data["System Properties"]);
	});

	function convertPropertiesToArray(propertyList) {
		var tmp = new Array();
		for (var variableName in propertyList) {
			tmp.push({
				key: variableName,
				val: propertyList[variableName]
			});
		}
		return tmp;
	}
}]);
