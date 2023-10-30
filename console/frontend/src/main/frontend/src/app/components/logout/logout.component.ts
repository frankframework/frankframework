import { Component, OnInit } from "@angular/core";
import { Idle } from "@ng-idle/core";
import { AuthService } from "src/app/services/auth.service";
import { PollerService } from "src/app/services/poller.service";

@Component({
  selector: 'app-logout',
  template: ''
})
export class LogoutComponent implements OnInit {
  constructor(
    private Poller: PollerService,
    private authService: AuthService,
    private idle: Idle
  ){}

  ngOnInit() {
    this.Poller.getAll().remove();
    this.idle.stop();
    this.authService.logout();
  };
}
