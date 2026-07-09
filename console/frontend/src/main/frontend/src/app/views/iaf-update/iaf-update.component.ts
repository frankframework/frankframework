import { Component, inject, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faGithub } from '@fortawesome/free-brands-svg-icons';
import { IAFRelease } from '../../app.service';
import { SessionService } from '../../services/session.service';
import { MarkDownPipe } from '../../pipes/mark-down.pipe';
import { ToDateDirective } from '../../components/to-date.directive';

@Component({
  selector: 'app-iaf-update',
  imports: [MarkDownPipe, ToDateDirective, FaIconComponent],
  templateUrl: './iaf-update.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./iaf-update.component.scss'],
})
export class IafUpdateComponent implements OnInit {
  protected release: IAFRelease | null = null;
  protected readonly faGithub = faGithub;

  private router = inject(Router);
  private sessionService = inject(SessionService);

  ngOnInit(): void {
    this.release = this.sessionService.get('IAF-Release');

    if (this.release === null) {
      this.router.navigate(['/status']);
    }
  }
}
