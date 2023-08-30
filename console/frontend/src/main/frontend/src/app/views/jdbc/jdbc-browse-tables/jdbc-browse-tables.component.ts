import { Component, Inject, OnInit } from '@angular/core';
import { AppConstants } from 'src/angularjs/app/app.module';
import { ApiService } from 'src/app/services.types';

@Component({
  selector: 'app-jdbc-browse-tables',
  templateUrl: './jdbc-browse-tables.component.html',
  styleUrls: ['./jdbc-browse-tables.component.scss']
})
export class JdbcBrowseTablesComponent implements OnInit {
  datasources = [];
  resultTypes = {};
  error = "";
  processingMessage = false;
  form: Record<string, any> = {};
  columnNames = [{}];
  result: any[] = [];
  query: any;

  constructor(
    @Inject("$scope") private $scope: angular.IScope,
    @Inject("apiService") private apiService: ApiService,
    @Inject("appConstants") private appConstants: AppConstants
  ) { };

  ngOnInit(): void {
    this.$scope.$on('appConstants', () => {
      this.form["datasource"] = this.appConstants['jdbc.datasource.default'];
    });

    this.apiService.Get("jdbc", (data) => {
      this.form["datasource"] = (this.appConstants['jdbc.datasource.default'] != undefined) ? this.appConstants['jdbc.datasource.default'] : data.datasources[0];
      this.datasources = data.datasources;
    });
  };

  submit(formData: any) {
    const columnNameArray: string[] = [];
    this.processingMessage = true;

    if (!formData || !formData.table) {
      this.error = "Please specify a datasource and table name!";
      this.processingMessage = false;
      return;
    }

    if (!formData.datasource) formData.datasource = this.datasources[0] || false;
    if (!formData.resultType) formData.resultType = this.resultTypes[0] || false;

    this.apiService.Post("jdbc/browse", JSON.stringify(formData), (returnData: any) => {
      this.error = "";
      this.query = returnData.query;
      let i = 0;

      for (const x in returnData.fielddefinition) {
        this.columnNames.push({
          id: i++,
          name: x,
          desc: returnData.fielddefinition[x]
        });
        columnNameArray.push(x);
      }

      for (const x in returnData.result) {
        const row = returnData.result[x];
        const orderedRow: string[] = [];

        for (const columnName in row) {
          const index = columnNameArray.indexOf(columnName);
          let value = row[columnName];

          if (index === -1 && columnName.indexOf("LENGTH ") > -1) {
            value += " (length)";
            value = row[columnName.replace("LENGTH ", "")];
          }
          orderedRow[index] = value;
        }
        this.result.push(orderedRow);
      }

      this.processingMessage = false;
    }, (errorData: any) => {
      const error = errorData.error ? errorData.error : "";
      this.error = error;
      this.query = "";
      this.processingMessage = false;
    }, false);
  }

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
  }
}
