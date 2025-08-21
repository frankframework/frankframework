import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { AppConstants, AppService } from 'src/app/app.service';
import { RouterModule } from '@angular/router';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { toObservable } from '@angular/core/rxjs-interop';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faDesktop } from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'app-custom-views',
  templateUrl: './custom-views.component.html',
  styleUrls: ['./custom-views.component.scss'],
  imports: [RouterModule, NgbModule, FaIconComponent],
})
export class CustomViewsComponent implements OnInit, OnDestroy {
  protected readonly faDesktop = faDesktop;
  protected customViews: {
    view: string;
    name: string;
    url: string;
  }[] = [];

  private appService: AppService = inject(AppService);
  private appConstants$ = toObservable(this.appService.appConstants);
  private subscription: Subscription | null = null;

  ngOnInit(): void {
    this.subscription = this.appConstants$.subscribe(() => this.updateCustomViews());
    this.updateCustomViews();
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  updateCustomViews(): void {
    const appConstants: AppConstants = this.appService.appConstants();
    const customViews = appConstants['customViews.names'] as string | undefined;
    if (typeof customViews !== 'string') return;

    if (customViews.length > 0) {
      const views = customViews.split(',');
      for (const index in views) {
        const viewId = views[index];
        const name = appConstants[`customViews.${viewId}.name`] as string;
        const url = appConstants[`customViews.${viewId}.url`] as string;
        if (name && url)
          this.customViews.push({
            view: viewId,
            name: name,
            url: url,
          });
      }
    }
  }
}
