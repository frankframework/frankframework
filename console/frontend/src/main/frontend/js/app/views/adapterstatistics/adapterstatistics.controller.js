import { appModule } from "../../app.module";

appModule.controller('AdapterStatisticsCtrl', ['$scope', 'Api', '$stateParams', 'SweetAlert', '$timeout', '$filter', 'appConstants', 'Debug', 'Misc', function ($scope, Api, $stateParams, SweetAlert, $timeout, $filter, appConstants, Debug, Misc) {
	var adapterName = $stateParams.name;
	if (!adapterName)
		return SweetAlert.Warning("Adapter not found!");
	$scope.adapterName = adapterName;
	$scope.refreshing = false;

	$scope.hourlyStatistics = {
		labels: [],
		data: [],
	};

	$scope.stats = [];
	var defaults = { "name": "Name", "count": "Count", "min": "Min", "max": "Max", "avg": "Average", "stdDev": "StdDev", "sum": "Sum", "first": "First", "last": "Last" };
	$scope.statisticsTimeBoundaries = angular.copy(defaults);
	$scope.statisticsSizeBoundaries = angular.copy(defaults);
	function populateBoundaries() {
		var timeBoundaries = appConstants["Statistics.boundaries"].split(",");
		var sizeBoundaries = appConstants["Statistics.size.boundaries"].split(",");
		var percBoundaries = appConstants["Statistics.percentiles"].split(",");

		var publishPercentiles = appConstants["Statistics.percentiles.publish"] == "true";
		var publishHistograms = appConstants["Statistics.histograms.publish"] == "true";
		var calculatePercentiles = appConstants["Statistics.percentiles.internal"] == "true";
		var displayPercentiles = publishPercentiles || publishHistograms || calculatePercentiles;

		Debug.info("appending Statistic.boundaries", timeBoundaries, sizeBoundaries, percBoundaries);

		for (const i in timeBoundaries) {
			var j = timeBoundaries[i];
			$scope.statisticsTimeBoundaries[j + "ms"] = "< " + j + "ms";
		}
		for (const i in sizeBoundaries) {
			var j = sizeBoundaries[i];
			$scope.statisticsSizeBoundaries[j + "B"] = "< " + j + "B";
		}
		if (displayPercentiles) {
			for (const i in percBoundaries) {
				var j = "p" + percBoundaries[i];
				$scope.statisticsTimeBoundaries[j] = j;
				$scope.statisticsSizeBoundaries[j] = j;
			}
		}
	};
	if (appConstants["Statistics.boundaries"]) {
		populateBoundaries(); //AppConstants already loaded
	}
	else {
		$scope.$on('appConstants', populateBoundaries); //Wait for appConstants trigger to load
	}

	$scope.statisticsNames = [];
	$scope.refresh = function () {
		$scope.refreshing = true;
		Api.Get("adapters/" + Misc.escapeURL(adapterName) + "/statistics", function (data) {
			$scope.stats = data;

			var labels = [];
			var chartData = [];
			for (const i in data["hourly"]) {
				var a = data["hourly"][i];
				labels.push(a["time"]);
				chartData.push(a["count"]);
			}
			$scope.hourlyStatistics.labels = labels;
			$scope.hourlyStatistics.data = chartData;

			$timeout(function () {
				$scope.refreshing = false;
			}, 500);
		});
	};

	$scope.dataset = {
		fill: false,
		backgroundColor: "#2f4050",
		borderColor: "#2f4050",
	};
	$scope.options = {
		responsive: true,
		maintainAspectRatio: false,
		scales: {
			yAxes: [{
				display: true,
				scaleLabel: {
					display: true,
					labelString: 'Messages Per Hour'
				},
				ticks: {
					beginAtZero: true,
					suggestedMax: 10
				}
			}]
		},
		tooltips: {
			mode: 'index',
			intersect: false,
			displayColors: false,
		},
		hover: {
			mode: 'nearest',
			intersect: true
		}
	};

	$timeout(function () {
		$scope.refresh();
	}, 1000);
}]);
