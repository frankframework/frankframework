import { Component, Inject, OnInit } from '@angular/core';
import { MiscService } from 'src/angularjs/app/services/misc.service';

@Component({
  selector: 'app-iframe-ladybug',
  templateUrl: './iframe-ladybug.component.html',
  styleUrls: ['./iframe-ladybug.component.scss']
})
export class IframeLadybugComponent implements OnInit {
  url = "";
  redirectURL = "";

  constructor(
    private Misc: MiscService,
  ) { };

  ngOnInit(): void {
    this.url = this.Misc.getServerPath() + "iaf/testtool";
  };
}
