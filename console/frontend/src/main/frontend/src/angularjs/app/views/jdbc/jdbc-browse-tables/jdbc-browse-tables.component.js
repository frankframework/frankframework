import { appModule } from "../../../app.module";

const JdbcBrowseTablesController = function (appService, Api, $timeout, $state, appConstants) {
  const ctrl = this;

  ctrl.datasources = {};
  ctrl.resultTypes = {};
  ctrl.error = "";
  ctrl.processingMessage = false;
  ctrl.form = {};

  appService.appConstants$.subscribe(function () {
    ctrl.form.datasource = appConstants['jdbc.datasource.default'];
  });

  Api.Get("jdbc", function (data) {
    ctrl.form.datasource = (appConstants['jdbc.datasource.default'] != undefined) ? appConstants['jdbc.datasource.default'] : data.datasources[0];
    ctrl.datasources = data.datasources;
  });

  ctrl.submit = function (formData) {
    ctrl.processingMessage = true;
    if (!formData || !formData.table) {
      ctrl.error = "Please specify a datasource and table name!";
      ctrl.processingMessage = false;
      return;
    };

    if (!formData.datasource) formData.datasource = ctrl.datasources[0] || false;
    if (!formData.resultType) formData.resultType = ctrl.resultTypes[0] || false;

    ctrl.columnNames = [{}];
    var columnNameArray = [];
    ctrl.result = [];

    Api.Post("jdbc/browse", JSON.stringify(formData), function (returnData) {
      ctrl.error = "";
      ctrl.query = returnData.query;
      var i = 0;

      for (const x in returnData.fielddefinition) {
        ctrl.columnNames.push({
          id: i++,
          name: x,
          desc: returnData.fielddefinition[x]
        });
        columnNameArray.push(x);
      };

      for (const x in returnData.result) {
        var row = returnData.result[x];
        var orderedRow = [];

        for (const columnName in row) {
          var index = columnNameArray.indexOf(columnName);
          var value = row[columnName];

          if (index == -1 && columnName.indexOf("LENGTH ") > -1) {
            value += " (length)";
            index = columnNameArray.indexOf(columnName.replace("LENGTH ", ""));
          }
          orderedRow[index] = value;
        }
        ctrl.result.push(orderedRow);
      };

      ctrl.processingMessage = false;
    }, function (errorData) {
      var error = (errorData.error) ? errorData.error : "";
      ctrl.error = error;
      ctrl.query = "";
      ctrl.processingMessage = false;
    }, false);
  };

  ctrl.reset = function () {
    ctrl.query = "";
    ctrl.error = "";
    if (!ctrl.form) return;
    if (ctrl.form.table) ctrl.form.table = "";
    if (ctrl.form.where) ctrl.form.where = "";
    if (ctrl.form.order) ctrl.form.order = "";
    if (ctrl.form.numberOfRowsOnly) ctrl.form.numberOfRowsOnly = "";
    if (ctrl.form.minRow) ctrl.form.minRow = "";
    if (ctrl.form.maxRow) ctrl.form.maxRow = "";
  };
};

appModule.component('jdbcBrowseTables', {
  controller: ['appService', 'Api', '$timeout', '$state', 'appConstants', JdbcBrowseTablesController],
  templateUrl: 'js/app/views/jdbc/jdbc-browse-tables/jdbc-browse-tables.component.html'
});
