import { Component, OnInit } from '@angular/core';
import { Alert, AlertService } from 'src/app/services/alert.service';
import { AuthService } from 'src/app/services/auth.service';

type Credentials = {
  username: string,
  password: string
}

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  credentials: Credentials = {
    username: "",
    password: ""
  };
  notifications: Alert[] = [];

  constructor(
    private alertService: AlertService,
    private authService: AuthService
  ){ }

  ngOnInit() {
    this.authService.loggedin(); //Check whether or not the client is logged in.

    window.setTimeout(() => {
      this.notifications = this.alertService.get();
    }, 500);
  }

  login(credentials: Credentials) {
    this.authService.login(credentials.username, credentials.password);
  }
}
