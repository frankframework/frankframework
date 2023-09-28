import { AppConstants, appModule } from "../../app.module";
import { AppService } from "../../app.service";
import { ApiService } from "../../services/api.service";

class EnvironmentVariablesController {
    variables = {};
    searchFilter = "";
    selectedConfiguration = null;
    configProperties = [];
    environmentProperties: any = [];
    systemProperties: any = [];
    configurations = {};

    constructor(
        private appService: AppService,
        private appConstants: AppConstants,
        private Api: ApiService,
        private $location: angular.ILocationService
    ) { };

    $onInit() {
        function convertPropertiesToArray(propertyList: any) {
            var tmp = new Array();
            for (var variableName in propertyList) {
                tmp.push({
                    key: variableName,
                    val: propertyList[variableName]
                });
            }
            return tmp;
        }

        this.configurations = this.appService.configurations;
        this.appService.configurations$.subscribe(() => { this.configurations = this.appService.configurations; });

        this.Api.Get("environmentvariables", (data) => {
            var instanceName = null;
            for (var configName in data["Application Constants"]) {
                this.appConstants[configName] = convertPropertiesToArray(data["Application Constants"][configName]);
                if (instanceName == null) {
                    instanceName = data["Application Constants"][configName]["instance.name"];
                }
            }
            this.changeConfiguration("All");
            this.environmentProperties = convertPropertiesToArray(data["Environment Variables"]);
            this.systemProperties = convertPropertiesToArray(data["System Properties"]);
        });
    };

    changeConfiguration(name: any) {
        this.selectedConfiguration = name;
        this.configProperties = this.appConstants[name];
    };
};

appModule.component('environmentVariables', {
    controller: ['Api', 'appService', EnvironmentVariablesController],
    templateUrl: 'js/app/views/environment-variables/environment-variables.component.html'
});
