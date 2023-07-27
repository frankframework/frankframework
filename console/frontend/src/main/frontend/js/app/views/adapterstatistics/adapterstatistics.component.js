import { appModule } from "../../app.module";

const adapterstatisticsController = function ($rootScope, Api, $stateParams, SweetAlert, $timeout, $filter, appConstants, Debug, Misc) {
    const ctrl = this;

    ctrl.defaults = { "name": "Name", "count": "Count", "min": "Min", "max": "Max", "avg": "Average", "stdDev": "StdDev", "sum": "Sum", "first": "First", "last": "Last" };
	ctrl.adapterName = $stateParams.name;
    ctrl.refreshing = false;
    ctrl.hourlyStatistics = {
        labels: [],
        data: [],
    };
    ctrl.stats = [];
    ctrl.statisticsTimeBoundaries = angular.copy(ctrl.defaults);
	ctrl.statisticsSizeBoundaries = angular.copy(ctrl.defaults);
    ctrl.statisticsNames = [];
    ctrl.dataset = {
        fill: false,
        backgroundColor: "#2f4050",
        borderColor: "#2f4050",
    };
    ctrl.options = {
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

    ctrl.$onInit = function () {
		if (!ctrl.adapterName)
            return SweetAlert.Warning("Adapter not found!");

        if (appConstants["Statistics.boundaries"]) {
			ctrl.populateBoundaries(); //AppConstants already loaded
        }
        else {
			$rootScope.$on('appConstants', ctrl.populateBoundaries); //Wait for appConstants trigger to load
        }

        $timeout(function () {
            ctrl.refresh();
        }, 1000);
    };

    ctrl.refresh = function () {
        ctrl.refreshing = true;
		Api.Get("adapters/" + Misc.escapeURL(ctrl.adapterName) + "/statistics", function (data) {
            ctrl.stats = data;

            var labels = [];
            var chartData = [];
            for (const i in data["hourly"]) {
                var a = data["hourly"][i];
                labels.push(a["time"]);
                chartData.push(a["count"]);
            }
            ctrl.hourlyStatistics.labels = labels;
            ctrl.hourlyStatistics.data = chartData;

            $timeout(function () {
                ctrl.refreshing = false;
            }, 500);
        });
    };

    ctrl.populateBoundaries = function() {
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
            ctrl.statisticsTimeBoundaries[j + "ms"] = "< " + j + "ms";
        }
        for (const i in sizeBoundaries) {
            var j = sizeBoundaries[i];
            ctrl.statisticsSizeBoundaries[j + "B"] = "< " + j + "B";
        }
        if (displayPercentiles) {
            for (const i in percBoundaries) {
                var j = "p" + percBoundaries[i];
                ctrl.statisticsTimeBoundaries[j] = j;
                ctrl.statisticsSizeBoundaries[j] = j;
            }
        }
    };
};

appModule.component('adapterstatistics', {
    controller: ['$rootScope', 'Api', '$stateParams', 'SweetAlert', '$timeout', '$filter', 'appConstants', 'Debug', 'Misc', adapterstatisticsController],
    templateUrl: 'js/app/views/adapterstatistics/adapterstatistics.component.html'
});
