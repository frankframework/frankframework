import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { first, Subscription } from 'rxjs';
import { AppService, ServerErrorResponse } from 'src/app/app.service';
import { JdbcService, JdbcSummary, JdbcSummaryForm } from '../jdbc/jdbc.service';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgClass } from '@angular/common';
import { HasAccessToLinkDirective } from '../../components/has-access-to-link.directive';
import { QuickSubmitFormDirective } from '../../components/quick-submit-form.directive';
import { FormsModule } from '@angular/forms';
import { toObservable } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-ibisstore-summary',
  imports: [RouterLink, NgClass, HasAccessToLinkDirective, QuickSubmitFormDirective, FormsModule],
  templateUrl: './ibisstore-summary.component.html',
  styleUrls: ['./ibisstore-summary.component.scss'],
})
export class IbisstoreSummaryComponent implements OnInit, OnDestroy {
  protected datasources: string[] = [];
  protected form: JdbcSummaryForm = { datasource: '' };
  protected error: string | null = null;
  protected result: JdbcSummary[] = [];

  private readonly router: Router = inject(Router);
  private readonly route: ActivatedRoute = inject(ActivatedRoute);
  private readonly jdbcService: JdbcService = inject(JdbcService);
  private readonly appService: AppService = inject(AppService);
  private appConstants$ = toObservable(this.appService.appConstants);
  private appConstantsSubscription: Subscription | null = null;

  ngOnInit(): void {
    this.appConstantsSubscription = this.appConstants$.subscribe((appConstants) => {
      this.form.datasource = appConstants['jdbc.datasource.default'] as string;
    });

    this.jdbcService.getJdbc().subscribe((data) => {
      const appConstants = this.appService.appConstants();
      this.datasources = data.datasources;
      this.form.datasource =
        appConstants['jdbc.datasource.default'] === undefined
          ? data.datasources[0]
          : (appConstants['jdbc.datasource.default'] as string);
    });
    this.route.queryParamMap.pipe(first()).subscribe((parameters) => {
      if (parameters.has('datasource')) this.fetch(parameters.get('datasource')!);
    });
  }

  ngOnDestroy(): void {
    this.appConstantsSubscription?.unsubscribe();
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
