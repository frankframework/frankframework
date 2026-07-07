import { Component, computed, inject, ChangeDetectionStrategy } from '@angular/core';
import { ServerInfoService } from '../../../services/server-info.service';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faGlobe } from '@fortawesome/free-solid-svg-icons';
import { faGithub } from '@fortawesome/free-brands-svg-icons';

@Component({
  selector: 'app-pages-footer',
  imports: [FontAwesomeModule],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './pages-footer.component.html',
})
export class PagesFooterComponent {
  protected consoleVersion = computed(() => this.serverInfoService.consoleInfo().version ?? '');
  protected readonly faGlobe = faGlobe;
  protected readonly faGithub = faGithub;

  private readonly serverInfoService: ServerInfoService = inject(ServerInfoService);
}
