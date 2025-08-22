import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from 'src/app/services/auth.service';
import { FormsModule } from '@angular/forms';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faCopyright, faGlobe } from '@fortawesome/free-solid-svg-icons';
import { faGithub } from '@fortawesome/free-brands-svg-icons';

type Credentials = {
  username: string;
  password: string;
};

type Alert = {
  type: string;
  message: string;
  time: number;
  id?: number;
};

@Component({
  selector: 'app-login',
  imports: [FormsModule, FaIconComponent],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent implements OnInit {
  protected credentials: Credentials = {
    username: '',
    password: '',
  };
  protected notifications: Alert[] = [];
  protected isUwu = false;
  protected readonly faCopyright = faCopyright;
  protected readonly faGlobe = faGlobe;
  protected readonly faGithub = faGithub;

  private authService = inject(AuthService);
  private router = inject(Router);

  ngOnInit(): void {
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/']);
    }

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
