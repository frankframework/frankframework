import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Idle } from '@ng-idle/core';
import { AuthService } from 'src/app/services/auth.service';
import { PollerService } from 'src/app/services/poller.service';

@Component({
  selector: 'app-logout',
  template: '',
  imports: [],
})
export class LogoutComponent implements OnInit {
  private Poller = inject(PollerService);
  private authService = inject(AuthService);
  private idle = inject(Idle);
  private router = inject(Router);

  ngOnInit(): void {
    this.Poller.getAll().remove();
    this.idle.stop();
    this.authService.logout().subscribe({
      next: () => {
        this.router.navigate(['/login']);
      },
      error: () => {
        globalThis.setTimeout(() => {
          this.router.navigate(['/']);
        }, 2000);
      },
    });
  }
}
