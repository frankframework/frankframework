import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Idle } from '@ng-idle/core';
import { AuthService } from 'src/app/services/auth.service';
import { PollerService } from 'src/app/services/poller.service';

@Component({
  selector: 'app-logout',
  template: '',
  imports: [CommonModule],
})
export class LogoutComponent implements OnInit {
  constructor(
    private Poller: PollerService,
    private authService: AuthService,
    private idle: Idle,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.Poller.getAll().remove();
    this.idle.stop();
    this.authService.logout().subscribe({
      next: () => {
        this.router.navigate(['/login']);
      },
      error: () => {
        window.setTimeout(() => {
          this.router.navigate(['/']);
        }, 2000);
      },
    });
  }
}
