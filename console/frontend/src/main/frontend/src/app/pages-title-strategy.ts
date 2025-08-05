import { inject, Injectable } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { AppService } from './app.service';

@Injectable({ providedIn: 'root' })
export class PagesTitleStrategy extends TitleStrategy {
  private readonly title = inject(Title);
  private readonly appService = inject(AppService);

  constructor() {
    super();
  }

  override updateTitle(routerState: RouterStateSnapshot): void {
    const title = this.buildTitle(routerState);
    if (title !== undefined) {
      const dtapStage = this.appService.dtapStage();
      const instanceName = this.appService.instanceName();
      this.title.setTitle(`${dtapStage}-${instanceName} | ${title}`);
    }
  }
}
