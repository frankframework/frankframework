import { Component, OnInit } from "@angular/core";
import { Idle } from "@ng-idle/core";
import { AuthService } from "src/angularjs/app/services/authservice.service";
import { PollerService } from "src/angularjs/app/services/poller.service";

@Component({
  selector: 'app-logout',
  template: ''
})
export class LogoutComponent implements OnInit {
  constructor(
    private Poller: PollerService,
    private authService: AuthService,
    // private idleService: Idle
  ){}

  ngOnInit() {
    this.Poller.getAll().remove();
    // this.idleService.unwatch(); TODO handle this in a seperate service
    this.authService.logout();
  };
}
