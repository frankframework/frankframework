import { Injectable } from "@angular/core";
import { Title } from "@angular/platform-browser";
import { RouterStateSnapshot, TitleStrategy } from "@angular/router";
import { AppService } from "./app.service";

@Injectable({ providedIn: 'root' })
export class PagesTitleStrategy extends TitleStrategy {
  constructor(
    private readonly title: Title,
    private appService: AppService
  ) {
    super();
  }

  override updateTitle(routerState: RouterStateSnapshot) {
    const title = this.buildTitle(routerState);
    if (title !== undefined) {
      const dtapStage = this.appService.dtapStage;
      const instanceName = this.appService.instanceName;
      this.title.setTitle(`${dtapStage}-${instanceName} | ${title}`);
    }
  }
}
