import { AppConstants, appModule } from "../../app.module";
import { AppService } from "../../app.service";
import { ApiService } from "../../services/api.service";

class IbisstoreSummaryController {
    datasources = [];
    form: Record<string, any> = {};
    error = "";
    result = "";

    constructor(
        private appService: AppService,
        private appConstants: AppConstants,
        private Api: ApiService,
        private $location: angular.ILocationService
    ) { };

    $onInit() {
        this.appService.appConstants$.subscribe(() => {
            this.form["datasource"] = this.appConstants['jdbc.datasource.default'];
        });

        this.Api.Get("jdbc", (data) => {
            $.extend(this, data);
            this.form["datasource"] = (this.appConstants['jdbc.datasource.default'] != undefined) ? this.appConstants['jdbc.datasource.default'] : data.datasources[0];
        });

        if (this.$location.search() && this.$location.search().datasource != null) {
            var datasource = this.$location.search().datasource;
            this.fetch(datasource);
        };
    };

    fetch(datasource: []) {
        this.Api.Post("jdbc/summary", JSON.stringify({ datasource: datasource }), (data) => {
            this.error = "";
            $.extend(this, data);
        }, (errorData, status, errorMsg) => {
            var error = (errorData) ? errorData.error : errorMsg;
            error = error;
            this.result = "";
        }, false);
    };

    submit(formData: any) {
        if (!formData) formData = {};

        if (!formData.datasource) formData.datasource = this.datasources[0] || false;
        this.$location.search('datasource', formData.datasource);
        this.fetch(formData.datasource);
    };

    reset() {
        this.$location.search('datasource', null);
        this.result = "";
        this.error = "";
    };
}

appModule.component('ibisStoreSummary', {
    controller: ['appService', 'Api', '$location', 'appConstants', IbisstoreSummaryController],
    templateUrl: 'js/app/views/ibisstore-summary/ibisstore-summary.component.html'
});