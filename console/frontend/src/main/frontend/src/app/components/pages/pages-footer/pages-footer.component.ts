import { Component, computed, inject } from '@angular/core';
import { ServerInfoService } from '../../../services/server-info.service';

@Component({
  selector: 'app-pages-footer',
  templateUrl: './pages-footer.component.html',
})
export class PagesFooterComponent {
  protected consoleVersion = computed(() => this.serverInfoService.consoleInfo().version ?? '');

  private readonly serverInfoService: ServerInfoService = inject(ServerInfoService);
}
