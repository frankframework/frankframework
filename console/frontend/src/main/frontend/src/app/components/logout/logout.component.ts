import { Component, OnInit } from "@angular/core";
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
    private Idle: angular.idle.IIdleService
  ){}

  ngOnInit() {
    this.Poller.getAll().remove();
    this.Idle.unwatch();
    this.authService.logout();
  };
}
