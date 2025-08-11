import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { AppConstants, AppService } from 'src/app/app.service';
import { JdbcBrowseForm, JdbcService } from '../jdbc.service';
import { HttpErrorResponse } from '@angular/common/http';

interface ColumnName {
  id: number
  name: string
  desc: string
}

@Component({
  selector: 'app-jdbc-browse-tables',
  templateUrl: './jdbc-browse-tables.component.html',
  styleUrls: ['./jdbc-browse-tables.component.scss']
})
export class JdbcBrowseTablesComponent implements OnInit, OnDestroy {
  datasources: string[] = [];
  resultTypes: string[] = [];
  error: string = "";
  processingMessage: boolean = false;
  form: JdbcBrowseForm = {
    datasource: "",
    resultType: "",
    table: "",
    where: "",
    order: "",
    numberOfRowsOnly: false,
    minRow: 0,
    maxRow: 0
  };
  columnNames: ColumnName[] = [];
  result: string[][] = [];
  query: string = "";

  private _subscriptions = new Subscription();
  private appConstants: AppConstants;

  constructor(
    private appService: AppService,
    private jdbcService: JdbcService
  ) {
    this.appConstants = this.appService.APP_CONSTANTS;
    const appConstantsSubscription = this.appService.appConstants$.subscribe(() => {
      this.appConstants = this.appService.APP_CONSTANTS;
    });
    this._subscriptions.add(appConstantsSubscription);
  };

  ngOnInit(): void {
    const appConstantsSubscription = this.appService.appConstants$.subscribe(() => {
      this.form["datasource"] = this.appConstants['jdbc.datasource.default'];
    });
    this._subscriptions.add(appConstantsSubscription);

    this.jdbcService.getJdbc().subscribe((data) => {
      this.form["datasource"] = (this.appConstants['jdbc.datasource.default'] != undefined) ? this.appConstants['jdbc.datasource.default'] : data.datasources[0];
      this.datasources = data.datasources;
    });
  }

  ngOnDestroy(): void {
    this._subscriptions.unsubscribe();
  }

  submit(formData: JdbcBrowseForm) {
    const columnNameArray: string[] = [];
    this.columnNames = [];
    this.result = [];
    this.processingMessage = true;

    if (!formData || !formData.table) {
      this.error = "Please specify a datasource and table name!";
      this.processingMessage = false;
      return;
    }

    if (!formData.datasource) formData.datasource = this.datasources[0] || "";
    if (!formData.resultType) formData.resultType = this.resultTypes[0] || "";

    this.jdbcService.postJdbcBrowse(formData).subscribe({ next: returnData => {
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
          const value = row[columnName];

            if (index === -1 && columnName.includes('LENGTH ')) {
              const replaceIndex = columnNameArray.indexOf(columnName.replace('LENGTH ', ''));
              orderedRow[replaceIndex] = `${value} (length)`;
              continue;
            }
            orderedRow[index] = value;
          }
          this.result.push(orderedRow);
        }

      this.processingMessage = false;
    }, error: (errorData: HttpErrorResponse) => {
      const error = errorData.error ? errorData.error.error : "";
      this.error = typeof error === 'object' ? error.error : error;
      this.query = "";
      this.processingMessage = false;
    }}); // TODO no intercept
  }

  reset() {
    this.query = "";
    this.error = "";
    if (!this.form) return;
    if (this.form["table"]) this.form["table"] = "";
    if (this.form["where"]) this.form["where"] = "";
    if (this.form["order"]) this.form["order"] = "";
    if (this.form["numberOfRowsOnly"]) this.form["numberOfRowsOnly"] = false;
    if (this.form["minRow"]) this.form["minRow"] = 0;
    if (this.form["maxRow"]) this.form["maxRow"] = 0;
  }
}
