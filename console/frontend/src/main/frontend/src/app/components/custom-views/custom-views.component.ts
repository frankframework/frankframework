import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { AppRoutingModule } from 'src/app/app-routing.module';
import { AppConstants, AppService } from 'src/app/app.service';

@Component({
  selector: 'app-custom-views',
  templateUrl: './custom-views.component.html',
  styleUrls: ['./custom-views.component.scss'],
  standalone: true,
  imports: [CommonModule, AppRoutingModule],
})
export class CustomViewsComponent implements OnInit, OnDestroy {
  appConstants: AppConstants = this.appService.APP_CONSTANTS;
  customViews: {
    view: string;
    name: string;
    url: string;
  }[] = [];

  private _subscriptions = new Subscription();

  constructor(private appService: AppService) {}

  ngOnInit(): void {
    const appConstantsSubscription = this.appService.appConstants$.subscribe(
      () => {
        this.appConstants = this.appService.APP_CONSTANTS;
        const customViews = this.appConstants['customViews.names'] as string;
        if (typeof customViews !== 'string') return;

        if (customViews.length > 0) {
          const views = customViews.split(',');
          for (const index in views) {
            const viewId = views[index];
            const name = this.appConstants[
              `customViews.${viewId}.name`
            ] as string;
            const url = this.appConstants[
              `customViews.${viewId}.url`
            ] as string;
            if (name && url)
              this.customViews.push({
                view: viewId,
                name: name,
                url: url,
              });
          }
        }
      },
    );
    this._subscriptions.add(appConstantsSubscription);
  }

  ngOnDestroy(): void {
    this._subscriptions.unsubscribe();
  }
}
