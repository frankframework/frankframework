import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { AppConstants } from 'src/angularjs/app/app.module';
import { AppService } from 'src/angularjs/app/app.service';
import { ApiService } from 'src/angularjs/app/services/api.service';
import { APPCONSTANTS } from 'src/app/app.module';

interface Date {
  id: string
  count: number
}

interface Form {
  datasource: string
}

interface Result {
  name: string
  slotcount: number
  slots?: Slot[]
  msgcount: number
  type: string
}

interface Slot {
  id: string
  first: string
  adapter: string
  receiver: string
  pipe: string
  msgcount: number
  dates: Date[]
}

@Component({
  selector: 'app-ibisstore-summary',
  templateUrl: './ibisstore-summary.component.html',
  styleUrls: ['./ibisstore-summary.component.scss']
})
export class IbisstoreSummaryComponent implements OnInit, OnDestroy {
  datasources: string[] = [];
  form: Form = {
    datasource: ""
  };
  error: string = "";
  result: Result[] = [];

  private _subscriptions = new Subscription();

  constructor(
    private appService: AppService,
    @Inject(APPCONSTANTS) private appConstants: AppConstants,
    private apiService: ApiService,
  ) { };

  ngOnInit() {
    const appConstantsSubscription = this.appService.appConstants$.subscribe(() => {
      this.form["datasource"] = this.appConstants['jdbc.datasource.default'];
    });
    this._subscriptions.add(appConstantsSubscription);

    this.apiService.Get("jdbc", (data) => {
      Object.assign(this, data);
      this.form["datasource"] = (this.appConstants['jdbc.datasource.default'] != undefined) ? this.appConstants['jdbc.datasource.default'] : data.datasources[0];
    });

    // TODO
    // if (this.$location.search() && this.$location.search().datasource != null) {
    //   var datasource = this.$location.search().datasource;
    //   this.fetch(datasource);
    // };
    console.warn("Location search doesn't exist anymore, needs angular new router module to recreate functionality")
  }

  ngOnDestroy() {
    this._subscriptions.unsubscribe();
  }

  fetch(datasource: string) {
    this.apiService.Post("jdbc/summary", JSON.stringify({ datasource: datasource }), (data) => {
      this.error = "";
      Object.assign(this, data);
    }, (errorData, status, errorMsg) => {
      var error = (errorData) ? errorData.error : errorMsg;
      error = error;
      this.result = [];
    }, false);
  };

  submit(formData: Form) {
    if (!formData) formData = { datasource: "" };

    if (!formData.datasource) formData.datasource = this.datasources[0] || "";
    // TODO this.$location.search('datasource', formData.datasource);
    this.fetch(formData.datasource);
  };

  reset() {
    // TODO this.$location.search('datasource', null);
    this.result = [];
    this.error = "";
  };
}
