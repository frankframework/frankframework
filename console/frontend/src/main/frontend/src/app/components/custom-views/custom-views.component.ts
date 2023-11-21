import { Component, Inject, OnInit } from '@angular/core';
import { first } from 'rxjs';
import { AppConstants } from 'src/angularjs/app/app.module';
import { AppService } from 'src/angularjs/app/app.service';
import { APPCONSTANTS } from 'src/app/app.module';

type CustomView = { view: string, name: string, url: string };

@Component({
  selector: 'app-custom-views',
  templateUrl: './custom-views.component.html',
  styleUrls: ['./custom-views.component.scss']
})
export class CustomViewsComponent implements OnInit {
  appConstants: AppConstants;
  customViews: CustomView[] = [];

  constructor(private appService: AppService, @Inject(APPCONSTANTS) appConstants: AppConstants) {
    this.appConstants = appConstants;
  }

  ngOnInit() {
    this.appService.appConstants$.pipe(first()).subscribe(() => {
      const customViews = this.appConstants['customViews.names'];
      if (customViews && customViews.length > 0) {
        const views = customViews.split(",");
        for (const i in views) {
          const view = views[i];
          const name = this.appConstants[`customViews.${viewId}.name`];
          const url = this.appConstants[`customViews.${viewId}.url`];
          if (name && url){
            this.customViews.push({ view, name, url }); 
          }
        }
      }
    });
  }
}
