import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AppService } from 'src/app/app.service';

@Component({
  selector: 'app-loading',
  imports: [],
  templateUrl: './loading.component.html',
  styleUrls: ['./loading.component.scss'],
})
export class LoadingComponent implements OnInit {
  private readonly router: Router = inject(Router);
  private readonly appService: AppService = inject(AppService);

  ngOnInit(): void {
    this.appService.getServerHealth().subscribe({
      next: () => {
        this.router.navigate(['/status']);
      },
      error: (response: HttpErrorResponse) => {
        if (response.status == 401) return;
        if (response.statusText == 'SERVICE_UNAVAILABLE') {
          this.router.navigate(['/status']);
        } else {
          this.router.navigate(['/error']);
        }
      },
    });
  }
}
