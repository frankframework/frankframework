import { Component, Inject, OnInit } from '@angular/core';
import { MiscService } from 'src/angularjs/app/services/misc.service';

@Component({
  selector: 'app-iframe-ladybug-beta',
  templateUrl: './iframe-ladybug-beta.component.html',
  styleUrls: ['./iframe-ladybug-beta.component.scss']
})
export class IframeLadybugBetaComponent implements OnInit {
  url = "";
  redirectURL = "";

  constructor(
    private miscService: MiscService
  ) { };

  ngOnInit(): void {
    this.url = this.miscService.getServerPath() + "iaf/ladybug";
  };
}
