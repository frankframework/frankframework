import { Component, Inject, OnInit } from '@angular/core';
import { StateService } from 'angular-ui-router';
import { MiscService } from 'src/app/services.types';

@Component({
  selector: 'app-iframe-custom-view',
  templateUrl: './iframe-custom-view.component.html',
  styleUrls: ['./iframe-custom-view.component.scss']
})
export class IframeCustomViewComponent implements OnInit {
  url = "";
  redirectURL = "";

  constructor(
    @Inject("miscService") private miscService: MiscService,
    private $state: StateService,
    @Inject("$window") private $window: angular.IWindowService
  ) { };

  ngOnInit(): void {
    if (this.$state.params["url"] == "")
      this.$state.go('pages.status');

    if (this.$state.params["url"].indexOf("http") > -1) {
      this.$window.open(this.$state.params["url"], this.$state.params["name"]);
      this.redirectURL = this.$state.params["url"];
    }
    else
      this.url = this.miscService.getServerPath() + this.$state.params["url"];
  }
}
