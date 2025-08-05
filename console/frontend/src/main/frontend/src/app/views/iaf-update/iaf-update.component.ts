import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { IAFRelease } from 'src/app/app.service';
import { SessionService } from 'src/app/services/session.service';
import { MarkDownPipe } from '../../pipes/mark-down.pipe';
import { ToDateDirective } from '../../components/to-date.directive';

@Component({
  selector: 'app-iaf-update',
  imports: [MarkDownPipe, ToDateDirective],
  templateUrl: './iaf-update.component.html',
  styleUrls: ['./iaf-update.component.scss'],
})
export class IafUpdateComponent implements OnInit {
  protected release: IAFRelease | null = null;

  private router = inject(Router);
  private sessionService = inject(SessionService);

  ngOnInit(): void {
    this.release = this.sessionService.get('IAF-Release');

    if (this.release === null) {
      this.router.navigate(['/status']);
    }
  }
}
