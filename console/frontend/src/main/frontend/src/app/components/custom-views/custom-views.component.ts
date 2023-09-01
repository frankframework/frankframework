import { Component, Inject } from '@angular/core';
import { AppConstants } from 'src/angularjs/app/app.module';
import { AppService } from 'src/angularjs/app/app.service';
import { APPCONSTANTS } from 'src/app/app.module';

@Component({
  selector: 'app-custom-views',
  templateUrl: './custom-views.component.html',
  styleUrls: ['./custom-views.component.scss']
})
export class CustomViewsComponent {
  appConstants: AppConstants;
  customViews: {
    view: string,
    name: string,
    url: string
  }[] = [];

  constructor(private appService: AppService, @Inject(APPCONSTANTS) appConstants: AppConstants) {
    this.appConstants = appConstants;
  }

  $onInit() {
    this.appService.appConstants$.subscribe(() => {
      var customViews = this.appConstants["customViews.names"];
      if (customViews == undefined)
        return;

      if (customViews.length > 0) {
        var views = customViews.split(",");
        for (const i in views) {
          var viewId = views[i];
          var name = this.appConstants["customViews." + viewId + ".name"];
          var url = this.appConstants["customViews." + viewId + ".url"];
          if (name && url) this.customViews.push({
            view: viewId,
            name: name,
            url: url
          });
        }
      }
    });
  }
}
