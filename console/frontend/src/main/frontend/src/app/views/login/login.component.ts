import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Alert, AlertService } from 'src/app/services/alert.service';
import { AuthService } from 'src/app/services/auth.service';

type Credentials = {
  username: string;
  password: string;
};

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent implements OnInit {
  credentials: Credentials = {
    username: '',
    password: '',
  };
  notifications: Alert[] = [];

  protected isUwu: boolean = false;

  constructor(
    private alertService: AlertService,
    private authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/']);
    }

    window.setTimeout(() => {
      this.notifications = this.alertService.get();
    }, 50);

    this.isUwu = !!localStorage.getItem('uwu');
  }

  /* login(credentials: Credentials): void {
    this.authService.login(credentials.username, credentials.password);
  } */
  login(): void {
    // this.authService.login(credentials.username, credentials.password);
    this.router.navigate(['/']);
  }
}
