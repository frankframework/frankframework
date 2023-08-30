import { Component, Inject, OnInit } from '@angular/core';
import { MiscService } from 'src/angularjs/app/services/misc.service';

@Component({
  selector: 'app-iframe-larva',
  templateUrl: './iframe-larva.component.html',
  styleUrls: ['./iframe-larva.component.scss']
})
export class IframeLarvaComponent implements OnInit {
  url = "";
  redirectURL = '';

  constructor(
    @Inject("miscService") private miscService: MiscService,
  ) { };

  ngOnInit(): void {
    this.url = this.miscService.getServerPath() + "iaf/larva";
  };
}
