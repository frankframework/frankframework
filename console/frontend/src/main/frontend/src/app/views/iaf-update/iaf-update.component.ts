import { Component, OnInit } from '@angular/core';
import { SessionService } from 'src/angularjs/app/services/session.service';

interface Release {
  name: string
  html_url: string
  created_at: string
}

@Component({
  selector: 'app-iaf-update',
  templateUrl: './iaf-update.component.html',
  styleUrls: ['./iaf-update.component.scss']
})
export class IafUpdateComponent implements OnInit {
  release: Release = {
    name: "",
    html_url: "",
    created_at: ""
  };

  constructor(
    // TODO: private $location: angular.ILocationService,
    private sessionService: SessionService,
  ) { };

  ngOnInit(): void {
    this.release = this.sessionService.get("IAF-Release");

    if (this.release == undefined) {
      // TODO: this.$location.path("status");;
    }
  };
}
