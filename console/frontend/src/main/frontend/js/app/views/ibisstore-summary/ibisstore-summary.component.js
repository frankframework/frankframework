import { appModule } from "../../app.module";

const ibisStoreSummaryController = function ($scope, Api, $location, appConstants) {
    const ctrl = this;

    ctrl.datasources = {};
    ctrl.form = {};

    ctrl.$onInit = function () {
        $scope.$on('appConstants', function () {
            ctrl.form.datasource = appConstants['jdbc.datasource.default'];
        });

        Api.Get("jdbc", function (data) {
            $.extend(ctrl, data);
            ctrl.form.datasource = (appConstants['jdbc.datasource.default'] != undefined) ? appConstants['jdbc.datasource.default'] : data.datasources[0];
        });

        if ($location.search() && $location.search().datasource != null) {
            var datasource = $location.search().datasource;
            fetch(datasource);
        };
    };

    function fetch(datasource) {
        Api.Post("jdbc/summary", JSON.stringify({ datasource: datasource }), function (data) {
            ctrl.error = "";
            $.extend(ctrl, data);
        }, function (errorData, status, errorMsg) {
            var error = (errorData) ? errorData.error : errorMsg;
            ctrl.error = error;
            ctrl.result = "";
        }, false);
    };

    ctrl.submit = function (formData) {
        if (!formData) formData = {};

        if (!formData.datasource) formData.datasource = ctrl.datasources[0] || false;
        $location.search('datasource', formData.datasource);
        fetch(formData.datasource);
    };

    ctrl.reset = function () {
        $location.search('datasource', null);
        ctrl.result = "";
        ctrl.error = "";
    };
};

appModule.component('ibisStoreSummary', {
    controller: ['$scope', 'Api', '$location', 'appConstants', ibisStoreSummaryController],
    templateUrl: 'js/app/views/ibisstore-summary/ibisstore-summary.component.html'
});
