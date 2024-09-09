import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { IAFRelease } from 'src/app/app.service';
import { SessionService } from 'src/app/services/session.service';

@Component({
  selector: 'app-iaf-update',
  templateUrl: './iaf-update.component.html',
  styleUrls: ['./iaf-update.component.scss'],
})
export class IafUpdateComponent implements OnInit {
  protected release?: IAFRelease;

  constructor(
    private router: Router,
    private sessionService: SessionService,
  ) {}

  ngOnInit(): void {
    this.release = this.sessionService.get('IAF-Release');

    if (this.release == undefined) {
      this.router.navigate(['/status']);
    }
  }
}
