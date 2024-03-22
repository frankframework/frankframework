import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { AppConstants, AppService } from 'src/app/app.service';
import {
  JdbcService,
  JdbcSummary,
  JdbcSummaryForm,
} from '../jdbc/jdbc.service';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-ibisstore-summary',
  templateUrl: './ibisstore-summary.component.html',
  styleUrls: ['./ibisstore-summary.component.scss'],
})
export class IbisstoreSummaryComponent implements OnInit, OnDestroy {
  datasources: string[] = [];
  form: JdbcSummaryForm = {
    datasource: '',
  };
  error: string = '';
  result: JdbcSummary[] = [];

  private _subscriptions = new Subscription();
  private appConstants: AppConstants;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private appService: AppService,
    private jdbcService: JdbcService,
  ) {
    this.appConstants = this.appService.APP_CONSTANTS;
    const appConstantsSubscription = this.appService.appConstants$.subscribe(
      () => {
        this.appConstants = this.appService.APP_CONSTANTS;
        this.form['datasource'] = this.appConstants[
          'jdbc.datasource.default'
        ] as string;
      },
    );
    this._subscriptions.add(appConstantsSubscription);
  }

  ngOnInit(): void {
    this.jdbcService.getJdbc().subscribe((data) => {
      Object.assign(this, data);
      this.form['datasource'] =
        this.appConstants['jdbc.datasource.default'] == undefined
          ? data.datasources[0]
          : (this.appConstants['jdbc.datasource.default'] as string);
    });
    this.route.queryParamMap.subscribe((parameters) => {
      if (parameters.has('datasource'))
        this.fetch(parameters.get('datasource')!);
    });
  }

  ngOnDestroy(): void {
    this._subscriptions.unsubscribe();
  }

  fetch(datasource: string): void {
    this.jdbcService.postJdbcSummary({ datasource: datasource }).subscribe({
      next: (data) => {
        this.error = '';
        this.result = data.result;
      },
      error: (errorData: HttpErrorResponse) => {
        const error = errorData.error
          ? errorData.error.erorr
          : errorData.message;
        this.error = error;
        this.result = [];
      },
    }); // TODO no intercept
  }

  submit(formData: JdbcSummaryForm): void {
    if (!formData) formData = { datasource: '' };

    if (!formData.datasource) formData.datasource = this.datasources[0] || '';
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { datasource: formData.datasource },
    });
    this.fetch(formData.datasource);
  }

  reset(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { datasource: null },
    });
    this.result = [];
    this.error = '';
  }
}
