/// <reference path="../../../../../node_modules/monaco-editor/monaco.d.ts" />

import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { AppConstants, AppService, ServerErrorResponse } from 'src/app/app.service';
import { WebStorageService } from 'src/app/services/web-storage.service';
import { JdbcQueryForm, JdbcService } from '../jdbc.service';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { LaddaModule } from 'angular2-ladda';

import { QuickSubmitFormDirective } from '../../../components/quick-submit-form.directive';
import { MonacoEditorComponent } from '../../../components/monaco-editor/monaco-editor.component';

@Component({
  selector: 'app-jdbc-execute-query',
  imports: [FormsModule, LaddaModule, QuickSubmitFormDirective, MonacoEditorComponent],
  templateUrl: './jdbc-execute-query.component.html',
  styleUrls: ['./jdbc-execute-query.component.scss'],
})
export class JdbcExecuteQueryComponent implements OnInit, OnDestroy {
  protected datasources: string[] = [];
  protected resultTypes: string[] = [];
  protected queryTypes: string[] = [];
  protected error: string | null = '';
  protected processingMessage: boolean = false;
  protected form: JdbcQueryForm = {
    query: '',
    queryType: '',
    datasource: '',
    resultType: '',
    avoidLocking: false,
    trimSpaces: false,
  };
  protected result: string = '';

  protected readonly editorActions = {
    ctrlEnter: {
      id: 'submit',
      label: 'Submit Form',
      run: (): void => this.submit(this.form),
    },
  };

  private _subscriptions = new Subscription();
  private appConstants: AppConstants = this.appService.APP_CONSTANTS;

  constructor(
    private webStorageService: WebStorageService,
    private appService: AppService,
    private jdbcService: JdbcService,
  ) {}

  ngOnInit(): void {
    const appConstantsSubscription = this.appService.appConstants$.subscribe(() => {
      this.appConstants = this.appService.APP_CONSTANTS;
      this.form.datasource = this.appConstants['jdbc.datasource.default'] as string;
    });
    this._subscriptions.add(appConstantsSubscription);

    const executeQueryCookie = this.webStorageService.get<JdbcQueryForm>('executeQuery');

    this.jdbcService.getJdbc().subscribe((data) => {
      this.datasources = data.datasources;
      this.queryTypes = data.queryTypes;
      this.resultTypes = data.resultTypes;

      this.form.datasource =
        this.appConstants['jdbc.datasource.default'] == undefined
          ? data.datasources[0]
          : (this.appConstants['jdbc.datasource.default'] as string);
      this.form.queryType = data.queryTypes[0];
      this.form.resultType = data.resultTypes[0];

      if (executeQueryCookie) {
        this.form.query = executeQueryCookie.query;
        this.form.resultType = executeQueryCookie.resultType;
        if (data.datasources.includes(executeQueryCookie.datasource)) {
          this.form.datasource = executeQueryCookie.datasource;
        }
      }
    });
  }

  ngOnDestroy(): void {
    this._subscriptions.unsubscribe();
  }

  submit(formData: JdbcQueryForm): void {
    this.processingMessage = true;

    if (formData.query === '') {
      this.error = 'Please specify a datasource, resulttype and query!';
      this.processingMessage = false;
      return;
    }

    this.webStorageService.set('executeQuery', formData);

    this.jdbcService.postJdbcQuery(formData).subscribe({
      next: (returnData) => {
        this.error = null;

        if (!returnData) {
          returnData = 'Ok';
        }

        this.result = returnData;
        this.processingMessage = false;
      },
      error: (errorData: HttpErrorResponse) => {
        try {
          const errorResponse = JSON.parse(errorData.error) as ServerErrorResponse | undefined;
          this.error = errorResponse ? errorResponse.error : errorData.message;
        } catch {
          this.error = errorData.message;
        }
        this.result = '';
        this.processingMessage = false;
      },
    }); // TODO no intercept
  }

  reset(): void {
    this.error = null;
    this.result = '';
    this.webStorageService.remove('executeQuery');
    this.form = {
      query: '',
      queryType: '',
      datasource: this.datasources[0],
      resultType: this.resultTypes[0],
      avoidLocking: false,
      trimSpaces: false,
    };
  }
}
