import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { ServerInfoService } from '../../../services/server-info.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-pages-footer',
  templateUrl: './pages-footer.component.html',
})
export class PagesFooterComponent implements OnInit, OnDestroy {
  protected consoleVersion = '';

  private readonly serverInfoService: ServerInfoService = inject(ServerInfoService);
  private subscriptions: Subscription = new Subscription();

  ngOnInit(): void {
    this.subscriptions.add(
      this.serverInfoService.consoleVersion$.subscribe({
        next: (data) => {
          this.consoleVersion = data.version ?? 'null';
        },
      }),
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }
}
