import { Component, OnInit } from '@angular/core';
import { StateService } from '@uirouter/angularjs';
import { MiscService } from 'src/angularjs/app/services/misc.service';

@Component({
  selector: 'app-iframe-custom-view',
  templateUrl: './iframe-custom-view.component.html',
  styleUrls: ['./iframe-custom-view.component.scss']
})
export class IframeCustomViewComponent implements OnInit {
  url = "";
  redirectURL = "";

  constructor(
    private miscService: MiscService,
    private $state: StateService,
    private window: Window
  ) { };

  ngOnInit(): void {
    if (this.$state.params["url"] == "")
      this.$state.go('pages.status');

    if (this.$state.params["url"].indexOf("http") > -1) {
      this.window.open(this.$state.params["url"], this.$state.params["name"]);
      this.redirectURL = this.$state.params["url"];
    }
    else
      this.url = this.miscService.getServerPath() + this.$state.params["url"];
  }
}
