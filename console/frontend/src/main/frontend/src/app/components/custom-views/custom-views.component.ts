import { Component, Inject, OnInit } from '@angular/core';
import { first } from 'rxjs';
import { AppConstants } from 'src/angularjs/app/app.module';
import { AppService } from 'src/angularjs/app/app.service';
import { APPCONSTANTS } from 'src/app/app.module';

@Component({
  selector: 'app-custom-views',
  templateUrl: './custom-views.component.html',
  styleUrls: ['./custom-views.component.scss']
})
export class CustomViewsComponent implements OnInit {
  appConstants: AppConstants;
  customViews: {
    view: string,
    name: string,
    url: string
  }[] = [];

  constructor(private appService: AppService, @Inject(APPCONSTANTS) appConstants: AppConstants) {
    this.appConstants = appConstants;
  }

  ngOnInit() {
    this.appService.appConstants$.pipe(first()).subscribe(() => {
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
  }
}
