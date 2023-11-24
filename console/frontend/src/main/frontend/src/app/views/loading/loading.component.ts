import { Component, OnInit } from '@angular/core';
import { ApiService } from 'src/angularjs/app/services/api.service';
import { StateService } from "@uirouter/angularjs";

@Component({
  selector: 'app-loading',
  templateUrl: './loading.component.html',
  styleUrls: ['./loading.component.scss']
})
export class LoadingComponent implements OnInit {

  constructor(
    private apiService: ApiService,
    private stateService: StateService
  ) { };

  ngOnInit(): void {
    this.apiService.Get("server/health", () => {
      this.stateService.go("pages.status");
    }, (data, statusCode) => {
      if (statusCode == 401) return;
      if (data.status == "SERVICE_UNAVAILABLE") {
        this.stateService.go("pages.status");
      } else {
        this.stateService.go("pages.errorpage");
      };
    });
  };
}
