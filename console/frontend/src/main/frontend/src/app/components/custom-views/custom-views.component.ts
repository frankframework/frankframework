import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { AppConstants, AppService } from 'src/app/app.service';

@Component({
  selector: 'app-custom-views',
  templateUrl: './custom-views.component.html',
  styleUrls: ['./custom-views.component.scss']
})
export class CustomViewsComponent implements OnInit, OnDestroy {
  appConstants: AppConstants;
  customViews: {
    view: string,
    name: string,
    url: string
  }[] = [];

  private _subscriptions = new Subscription();

  constructor(private appService: AppService) {
    this.appConstants = this.appService.APP_CONSTANTS;
  }

  ngOnInit() {
    const appConstantsSubscription = this.appService.appConstants$.subscribe(() => {
      let customViews = this.appConstants["customViews.names"];
      if (customViews == undefined)
        return;

      if (customViews.length > 0) {
        let views = customViews.split(",");
        for (const i in views) {
          let viewId = views[i];
          let name = this.appConstants["customViews." + viewId + ".name"];
          let url = this.appConstants["customViews." + viewId + ".url"];
          if (name && url) this.customViews.push({
            view: viewId,
            name: name,
            url: url
          });
        }
      }
    });
    this._subscriptions.add(appConstantsSubscription);
  }

  ngOnDestroy() {
    this._subscriptions.unsubscribe();
  }
}
