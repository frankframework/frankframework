import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { AppService, ServerErrorResponse } from 'src/app/app.service';
import { JdbcBrowseForm, JdbcService } from '../jdbc.service';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { LaddaModule } from 'angular2-ladda';

import { OrderByPipe } from '../../../pipes/orderby.pipe';
import { QuickSubmitFormDirective } from '../../../components/quick-submit-form.directive';
import { WebStorageService } from '../../../services/web-storage.service';
import { toObservable } from '@angular/core/rxjs-interop';

type ColumnName = {
  id: number;
  name: string;
  desc: string;
};

@Component({
  selector: 'app-jdbc-browse-tables',
  imports: [FormsModule, LaddaModule, OrderByPipe, QuickSubmitFormDirective],
  templateUrl: './jdbc-browse-tables.component.html',
  styleUrls: ['./jdbc-browse-tables.component.scss'],
})
export class JdbcBrowseTablesComponent implements OnInit, OnDestroy {
  protected datasources: string[] = [];
  protected error: string | null = null;
  protected processingMessage = false;
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
  protected query = '';

  private readonly jdbcService: JdbcService = inject(JdbcService);
  private readonly webStorageService: WebStorageService = inject(WebStorageService);
  private readonly appService: AppService = inject(AppService);
  private appConstants$ = toObservable(this.appService.appConstants);
  private appConstantsSubscription: Subscription | null = null;

  ngOnInit(): void {
    this.appConstantsSubscription = this.appConstants$.subscribe((appConstants) => {
      this.form.datasource = appConstants['jdbc.datasource.default'] as string;
    });

    const browseTablesSession = this.webStorageService.get<JdbcBrowseForm>('browseTables');

    this.jdbcService.getJdbc().subscribe((data) => {
      const appConstants = this.appService.appConstants();
      this.datasources = data.datasources;

      if (browseTablesSession) {
        this.form = browseTablesSession;
      } else {
        this.form.datasource =
          appConstants['jdbc.datasource.default'] == undefined
            ? data.datasources[0]
            : (appConstants['jdbc.datasource.default'] as string);
        this.form.datasource = data.datasources[0] ?? '';
        this.form.resultType = data.resultTypes[0] ?? '';
      }
    });
  }

  ngOnDestroy(): void {
    this.appConstantsSubscription?.unsubscribe();
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
