import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { AppConstants, AppService } from 'src/app/app.service';
import { RouterModule } from '@angular/router';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-custom-views',
  templateUrl: './custom-views.component.html',
  styleUrls: ['./custom-views.component.scss'],
  imports: [CommonModule, RouterModule, NgbModule],
})
export class CustomViewsComponent implements OnInit, OnDestroy {
  protected customViews: {
    view: string;
    name: string;
    url: string;
  }[] = [];

  private appConstants: AppConstants = this.appService.APP_CONSTANTS;
  private _subscriptions = new Subscription();

  constructor(private appService: AppService) {}

  ngOnInit(): void {
    const appConstantsSubscription = this.appService.appConstants$.subscribe(() => {
      this.appConstants = this.appService.APP_CONSTANTS;
      this.updateCustomViews();
    });
    this._subscriptions.add(appConstantsSubscription);
    this.updateCustomViews();
  }

  updateCustomViews(): void {
    const customViews = this.appConstants['customViews.names'] as string;
    if (typeof customViews !== 'string') return;

    if (customViews.length > 0) {
      const views = customViews.split(',');
      for (const index in views) {
        const viewId = views[index];
        const name = this.appConstants[`customViews.${viewId}.name`] as string;
        const url = this.appConstants[`customViews.${viewId}.url`] as string;
        if (name && url)
          this.customViews.push({
            view: viewId,
            name: name,
            url: url,
          });
      }
    }
  }

  ngOnDestroy(): void {
    this._subscriptions.unsubscribe();
  }
}
