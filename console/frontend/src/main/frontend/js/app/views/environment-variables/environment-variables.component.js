import { appModule } from "../../app.module";

const EnvironmentVariablesController = function ($scope, Api, appConstants, $rootScope, appService) {
    const ctrl = this;

    ctrl.variables = {};
    ctrl.searchFilter = "";
    ctrl.selectedConfiguration = null;
    ctrl.configProperties = [];
    ctrl.environmentProperties = [];
    ctrl.systemProperties = [];
    ctrl.appConstants = [];

    ctrl.$onInit = function () {
        ctrl.configurations = appService.configurations;
        $rootScope.$on('configurations', function () { ctrl.configurations = appService.configurations; });

        Api.Get("environmentvariables", function (data) {
            var instanceName = null;
            for (var configName in data["Application Constants"]) {
                ctrl.appConstants[configName] = convertPropertiesToArray(data["Application Constants"][configName]);
                if (instanceName == null) {
                    instanceName = data["Application Constants"][configName]["instance.name"];
                }
            }
            ctrl.changeConfiguration("All");
            ctrl.environmentProperties = convertPropertiesToArray(data["Environment Variables"]);
            ctrl.systemProperties = convertPropertiesToArray(data["System Properties"]);
        });
    };

    ctrl.changeConfiguration = function (name) {
        ctrl.selectedConfiguration = name;
        ctrl.configProperties = ctrl.appConstants[name];
    };

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
};

appModule.component('environmentVariables', {
    controller: ['$scope', 'Api', 'appConstants', '$rootScope', 'appService', EnvironmentVariablesController],
    templateUrl: 'js/app/views/environment-variables/environment-variables.component.html'
});