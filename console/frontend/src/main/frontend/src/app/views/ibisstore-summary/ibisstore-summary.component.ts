import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { AppConstants, AppService } from 'src/app/app.service';
import { JdbcService, JdbcSummary, JdbcSummaryForm } from '../jdbc/jdbc.service';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-ibisstore-summary',
  templateUrl: './ibisstore-summary.component.html',
  styleUrls: ['./ibisstore-summary.component.scss']
})
export class IbisstoreSummaryComponent implements OnInit, OnDestroy {
  datasources: string[] = [];
  form: JdbcSummaryForm = {
    datasource: ""
  };
  error: string = "";
  result: JdbcSummary[] = [];

  private _subscriptions = new Subscription();
  private appConstants: AppConstants;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private appService: AppService,
    private jdbcService: JdbcService
  ) {
    this.appConstants = this.appService.APP_CONSTANTS;
    const appConstantsSubscription = this.appService.appConstants$.subscribe(() => {
      this.appConstants = this.appService.APP_CONSTANTS;
      this.form["datasource"] = this.appConstants['jdbc.datasource.default'];
    });
    this._subscriptions.add(appConstantsSubscription);
  };

  ngOnInit() {
    this.jdbcService.getJdbc().subscribe((data) => {
      Object.assign(this, data);
      this.form["datasource"] = (this.appConstants['jdbc.datasource.default'] != undefined) ? this.appConstants['jdbc.datasource.default'] : data.datasources[0];
    });
    this.route.queryParamMap.subscribe(params => {
      if (params.has('datasource'))
        this.fetch(params.get('datasource')!)
    })
  }

  ngOnDestroy() {
    this._subscriptions.unsubscribe();
  }

  fetch(datasource: string) {
    this.jdbcService.postJdbcSummary( { datasource: datasource }).subscribe({ next: (data) => {
      this.error = "";
      this.result = data.result;
    }, error: (errorData: HttpErrorResponse) => {
      var error = (errorData.error) ? errorData.error.erorr : errorData.message;
      error = error;
      this.result = [];
    }}); // TODO no intercept
  };

  submit(formData: JdbcSummaryForm) {
    if (!formData) formData = { datasource: "" };

    if (!formData.datasource) formData.datasource = this.datasources[0] || "";
    this.router.navigate([], { relativeTo: this.route, queryParams: { datasource: formData.datasource } });
    this.fetch(formData.datasource);
  };

  reset() {
    this.router.navigate([], { relativeTo: this.route, queryParams: { datasource: null } });
    this.result = [];
    this.error = "";
  };
}
