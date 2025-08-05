import { AfterViewInit, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Alert, AlertService } from 'src/app/services/alert.service';
import { AuthService } from 'src/app/services/auth.service';
import { FormsModule } from '@angular/forms';

type Credentials = {
  username: string;
  password: string;
};

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent implements OnInit, AfterViewInit {
  protected credentials: Credentials = {
    username: '',
    password: '',
  };
  protected notifications: Alert[] = [];
  protected isUwu = false;

  constructor(
    private alertService: AlertService,
    private authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/']);
    }

    this.isUwu = !!localStorage.getItem('uwu');
  }

  ngAfterViewInit(): void {
    this.notifications = this.alertService.get();
  }

  /* login(credentials: Credentials): void {
    this.authService.login(credentials.username, credentials.password);
  } */
  login(): void {
    // this.authService.login(credentials.username, credentials.password);
    this.router.navigate(['/']);
  }
}
