/// <reference path="../../../../../node_modules/monaco-editor/monaco.d.ts" />

import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { AppService, ServerErrorResponse } from 'src/app/app.service';
import { WebStorageService } from 'src/app/services/web-storage.service';
import { JdbcQueryForm, JdbcService } from '../jdbc.service';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { LaddaModule } from 'angular2-ladda';

import { QuickSubmitFormDirective } from '../../../components/quick-submit-form.directive';
import { MonacoEditorComponent } from '../../../components/monaco-editor/monaco-editor.component';
import { toObservable } from '@angular/core/rxjs-interop';

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
  protected processingMessage = false;
  protected form: JdbcQueryForm = {
    query: '',
    queryType: '',
    datasource: '',
    resultType: '',
    avoidLocking: false,
    trimSpaces: false,
  };
  protected result = '';

  protected readonly editorActions = {
    ctrlEnter: {
      id: 'submit',
      label: 'Submit Form',
      run: (): void => this.submit(this.form),
    },
  };

  private readonly webStorageService: WebStorageService = inject(WebStorageService);
  private readonly jdbcService: JdbcService = inject(JdbcService);
  private readonly appService: AppService = inject(AppService);
  private appConstants$ = toObservable(this.appService.appConstants);
  private appConstantsSubscription: Subscription | null = null;

  ngOnInit(): void {
    this.appConstantsSubscription = this.appConstants$.subscribe((appConstants) => {
      this.form.datasource = appConstants['jdbc.datasource.default'] as string;
    });

    const executeQueryCookie = this.webStorageService.get<JdbcQueryForm>('executeQuery');

    this.jdbcService.getJdbc().subscribe((data) => {
      const appConstants = this.appService.appConstants();
      this.datasources = data.datasources;
      this.queryTypes = data.queryTypes;
      this.resultTypes = data.resultTypes;

      if (executeQueryCookie) {
        this.form = executeQueryCookie;
      } else {
        this.form.datasource =
          appConstants['jdbc.datasource.default'] == undefined
            ? data.datasources[0]
            : (appConstants['jdbc.datasource.default'] as string);
        this.form.queryType = data.queryTypes[0];
        this.form.resultType = data.resultTypes[0];
      }
    });
  }

  ngOnDestroy(): void {
    this.appConstantsSubscription?.unsubscribe();
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
