import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { AppConstants, AppService, ServerErrorResponse } from 'src/app/app.service';
import { JdbcBrowseForm, JdbcQueryForm, JdbcService } from '../jdbc.service';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { LaddaModule } from 'angular2-ladda';

import { OrderByPipe } from '../../../pipes/orderby.pipe';
import { QuickSubmitFormDirective } from '../../../components/quick-submit-form.directive';
import { WebStorageService } from '../../../services/web-storage.service';

interface ColumnName {
  id: number;
  name: string;
  desc: string;
}

@Component({
  selector: 'app-jdbc-browse-tables',
  imports: [FormsModule, LaddaModule, OrderByPipe, QuickSubmitFormDirective],
  templateUrl: './jdbc-browse-tables.component.html',
  styleUrls: ['./jdbc-browse-tables.component.scss'],
})
export class JdbcBrowseTablesComponent implements OnInit, OnDestroy {
  protected datasources: string[] = [];
  protected error: string | null = null;
  protected processingMessage: boolean = false;
  protected form: JdbcBrowseForm = {
    datasource: '',
    resultType: '',
    table: '',
    where: '',
    order: '',
    numberOfRowsOnly: false,
    minRow: 1,
    maxRow: 100,
  };
  protected columnNames: ColumnName[] = [];
  protected result: string[][] = [];
  protected query: string = '';

  private _subscriptions = new Subscription();
  private appService: AppService = inject(AppService);
  private jdbcService: JdbcService = inject(JdbcService);
  private webStorageService: WebStorageService = inject(WebStorageService);
  private appConstants: AppConstants = this.appService.APP_CONSTANTS;

  ngOnInit(): void {
    const appConstantsSubscription = this.appService.appConstants$.subscribe(() => {
      this.appConstants = this.appService.APP_CONSTANTS;
      this.form.datasource = this.appConstants['jdbc.datasource.default'] as string;
    });
    this._subscriptions.add(appConstantsSubscription);

    const browseTablesCookie = this.webStorageService.get<JdbcBrowseForm>('browseTables');

    this.jdbcService.getJdbc().subscribe((data) => {
      this.form.datasource =
        this.appConstants['jdbc.datasource.default'] == undefined
          ? data.datasources[0]
          : (this.appConstants['jdbc.datasource.default'] as string);
      this.datasources = data.datasources;
      this.form.datasource = data.datasources[0] ?? '';
      this.form.resultType = data.resultTypes[0] ?? '';

      if (browseTablesCookie) {
        this.form.datasource = browseTablesCookie.datasource;
        this.form.resultType = browseTablesCookie.resultType;
        this.form.table = browseTablesCookie.table;
        this.form.where = browseTablesCookie.where;
        this.form.order = browseTablesCookie.order;
        this.form.numberOfRowsOnly = browseTablesCookie.numberOfRowsOnly;
        this.form.minRow = browseTablesCookie.minRow;
        this.form.maxRow = browseTablesCookie.maxRow;
      }
    });
  }

  ngOnDestroy(): void {
    this._subscriptions.unsubscribe();
  }

  submit(formData: JdbcBrowseForm): void {
    const columnNameArray: string[] = [];
    this.columnNames = [];
    this.result = [];
    this.processingMessage = true;

    if (formData.table === '') {
      this.error = 'Please specify a datasource and table name!';
      this.processingMessage = false;
      return;
    }

    this.webStorageService.set('browseTables', formData);

    this.jdbcService.postJdbcBrowse(formData).subscribe({
      next: (returnData) => {
        this.error = null;
        this.query = returnData.query;
        let index = 0;

        for (const x in returnData.fielddefinition) {
          this.columnNames.push({
            id: index++,
            name: x,
            desc: returnData.fielddefinition[x],
          });
          columnNameArray.push(x);
        }

        for (const row of Object.values(returnData.result)) {
          const orderedRow: string[] = [];

          for (const columnName in row) {
            const index = columnNameArray.indexOf(columnName);
            let value = row[columnName];

            if (index === -1 && columnName.includes('LENGTH ')) {
              value += ' (length)';
              value = row[columnName.replace('LENGTH ', '')];
            }
            orderedRow[index] = value;
          }
          this.result.push(orderedRow);
        }

        this.processingMessage = false;
      },
      error: (errorData: HttpErrorResponse) => {
        try {
          const errorResponse = JSON.parse(errorData.error) as ServerErrorResponse | undefined;
          this.error = errorResponse ? errorResponse.error : errorData.message;
        } catch {
          this.error = errorData.message;
        }
        this.query = '';
        this.processingMessage = false;
      },
    }); // TODO no intercept
  }

  reset(): void {
    this.query = '';
    this.error = null;
    this.webStorageService.remove('browseTables');
    this.form = {
      datasource: this.form.datasource,
      resultType: '',
      table: '',
      where: '',
      order: '',
      numberOfRowsOnly: false,
      minRow: 1,
      maxRow: 100,
    };
  }
}
