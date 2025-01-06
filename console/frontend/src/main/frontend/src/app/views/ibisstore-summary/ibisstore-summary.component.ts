import { Component, OnDestroy, OnInit } from '@angular/core';
import { first, Subscription } from 'rxjs';
import { AppConstants, AppService, ServerErrorResponse } from 'src/app/app.service';
import { JdbcService, JdbcSummary, JdbcSummaryForm } from '../jdbc/jdbc.service';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgClass, NgFor, NgIf } from '@angular/common';
import { HasAccessToLinkDirective } from '../../components/has-access-to-link.directive';

@Component({
  selector: 'app-ibisstore-summary',
  imports: [RouterLink, NgClass, NgIf, NgFor, HasAccessToLinkDirective],
  templateUrl: './ibisstore-summary.component.html',
  styleUrls: ['./ibisstore-summary.component.scss'],
})
export class IbisstoreSummaryComponent implements OnInit, OnDestroy {
  protected datasources: string[] = [];
  protected form: JdbcSummaryForm = { datasource: '' };
  protected error: string | null = null;
  protected result: JdbcSummary[] = [];

  private _subscriptions = new Subscription();
  private appConstants: AppConstants = this.appService.APP_CONSTANTS;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private appService: AppService,
    private jdbcService: JdbcService,
  ) {}

  ngOnInit(): void {
    const appConstantsSubscription = this.appService.appConstants$.subscribe(() => {
      this.appConstants = this.appService.APP_CONSTANTS;
      this.form.datasource = this.appConstants['jdbc.datasource.default'] as string;
    });
    this._subscriptions.add(appConstantsSubscription);

    this.jdbcService.getJdbc().subscribe((data) => {
      this.datasources = data.datasources;
      this.form.datasource =
        this.appConstants['jdbc.datasource.default'] === undefined
          ? data.datasources[0]
          : (this.appConstants['jdbc.datasource.default'] as string);
    });
    this.route.queryParamMap.pipe(first()).subscribe((parameters) => {
      if (parameters.has('datasource')) this.fetch(parameters.get('datasource')!);
    });
  }

  ngOnDestroy(): void {
    this._subscriptions.unsubscribe();
  }

  fetch(datasource: string): void {
    this.jdbcService.postJdbcSummary({ datasource: datasource }).subscribe({
      next: (data) => {
        this.error = null;
        this.result = data.result;
      },
      error: (errorData: HttpErrorResponse) => {
        try {
          const errorResponse = JSON.parse(errorData.error) as ServerErrorResponse | undefined;
          this.error = errorResponse ? errorResponse.error : errorData.message;
        } catch {
          this.error = errorData.message;
        }
        this.result = [];
      },
    }); // TODO no intercept
  }

  submit(formData: JdbcSummaryForm): void {
    if (!formData.datasource) formData.datasource = this.datasources[0] ?? '';
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
    this.error = null;
  }
}
