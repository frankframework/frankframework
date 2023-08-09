import * as angular from "angular";
import { AppConstants, appModule } from "../../../app.module";
import { ApiService } from "src/app/services.types";

class JdbcBrowseTablesController {
    datasources = {};
    resultTypes = {};
    error = "";
    processingMessage = false;
    form: Record<string, any> = {};
    columnNames = [{}];
    result: any[] = [];
    query: any;

    constructor(
        private $scope: angular.IScope,
        private Api: ApiService,
        private $timeout: angular.ITimeoutService,
        private $state: any,
        private appConstants: AppConstants
    ) { };

    $onInit() {
        this.$scope.$on('appConstants', () => {
            this.form["datasource"] = this.appConstants['jdbc.datasource.default'];
        });

        this.Api.Get("jdbc", (data) => {
            this.form["datasource"] = (this.appConstants['jdbc.datasource.default'] != undefined) ? this.appConstants['jdbc.datasource.default'] : data.datasources[0];
            this.datasources = data.datasources;
        });
    };

    submit(formData) {
        var columnNameArray: string[] = [];
        this.processingMessage = true;

        if (!formData || !formData.table) {
            this.error = "Please specify a datasource and table name!";
            this.processingMessage = false;
            return;
        };

        if (!formData.datasource) formData.datasource = this.datasources[0] || false;
        if (!formData.resultType) formData.resultType = this.resultTypes[0] || false;

        this.Api.Post("jdbc/browse", JSON.stringify(formData), (returnData) => {
            this.error = "";
            this.query = returnData.query;
            var i = 0;

            for (const x in returnData.fielddefinition) {
                this.columnNames.push({
                    id: i++,
                    name: x,
                    desc: returnData.fielddefinition[x]
                });
                columnNameArray.push(x);
            };

            for (const x in returnData.result) {
                var row = returnData.result[x];
                var orderedRow: string[] = [];

                for (const columnName in row) {
                    var index = columnNameArray.indexOf(columnName);
                    var value = row[columnName];

                    if (index == -1 && columnName.indexOf("LENGTH ") > -1) {
                        value += " (length)";
                        index = columnNameArray.indexOf(columnName.replace("LENGTH ", ""));
                    }
                    orderedRow[index] = value;
                }
                this.result.push(orderedRow);
            };

            this.processingMessage = false;
        }, (errorData) => {
            var error = (errorData.error) ? errorData.error : "";
            this.error = error;
            this.query = "";
            this.processingMessage = false;
        }, false);
    };

    reset() {
        this.query = "";
        this.error = "";
        if (!this.form) return;
        if (this.form["table"]) this.form["table"] = "";
        if (this.form["where"]) this.form["where"] = "";
        if (this.form["order"]) this.form["order"] = "";
        if (this.form["numberOfRowsOnly"]) this.form["numberOfRowsOnly"] = "";
        if (this.form["minRow"]) this.form["minRow"] = "";
        if (this.form["maxRow"]) this.form["maxRow"] = "";
    };
};

appModule.component('jdbcBrowseTables', {
    controller: ['$scope', 'Api', '$timeout', '$state', 'appConstants', JdbcBrowseTablesController],
    templateUrl: 'angularjs/app/views/jdbc/jdbc-browse-tables/jdbc-browse-tables.component.html'
});
