import { Component, inject, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { PollerService } from '../../services/poller.service';

@Component({
  selector: 'app-logout',
  template: '',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [],
})
export class LogoutComponent implements OnInit {
  private Poller = inject(PollerService);
  private authService = inject(AuthService);
  private router = inject(Router);

  ngOnInit(): void {
    this.Poller.getAll().remove();
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
