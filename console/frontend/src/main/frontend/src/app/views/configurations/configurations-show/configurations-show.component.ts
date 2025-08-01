import { Component, inject, OnDestroy, OnInit, Signal, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService, Configuration } from 'src/app/app.service';
import { ConfigurationsService } from '../configurations.service';
import { copyToClipboard } from 'src/app/utils';
import { MonacoEditorComponent } from '../../../components/monaco-editor/monaco-editor.component';
import { Subscription } from 'rxjs';
import { ConfigurationTabListComponent } from '../../../components/tab-list/configuration-tab-list.component';
import { NgClass } from '@angular/common';

@Component({
  selector: 'app-configurations-show',
  imports: [ConfigurationTabListComponent, NgClass, MonacoEditorComponent],
  templateUrl: './configurations-show.component.html',
  styleUrls: ['./configurations-show.component.scss'],
})
export class ConfigurationsShowComponent implements OnInit, OnDestroy {
  @ViewChild('editor') editor!: MonacoEditorComponent;

  protected selectedConfiguration: string = 'All';
  protected loadedConfiguration: boolean = false;

  private readonly appService: AppService = inject(AppService);
  protected configurations: Signal<Configuration[]> = this.appService.configurations;

  private readonly router: Router = inject(Router);
  private readonly route: ActivatedRoute = inject(ActivatedRoute);
  private readonly configurationsService: ConfigurationsService = inject(ConfigurationsService);
  private configuration: string = '';
  private fragment?: string;
  private selectedAdapter?: string;
  private skipParamsUpdate: boolean = false;
  private initialized: boolean = false;
  private configsSubscription: Subscription | null = null;

  ngOnInit(): void {
    this.route.fragment.subscribe((fragment) => {
      this.fragment = fragment ?? undefined;
      this.removeAdapterAfterLineSelection(fragment);
    });

    this.route.queryParamMap.subscribe((parameters) => {
      if (this.skipParamsUpdate) {
        this.skipParamsUpdate = false;
        return;
      }
      this.selectedAdapter = parameters.get('adapter') ?? undefined;
      this.loadedConfiguration = parameters.get('loaded') !== 'false';
    });
  }

  ngOnDestroy(): void {
    this.configsSubscription?.unsubscribe();
  }

  update(loaded: boolean): void {
    this.loadedConfiguration = loaded;
    this.fragment = undefined;

    this.getConfiguration();
    this.updateQueryParams();
  }

  changeConfiguration(name: string): void {
    this.selectedConfiguration = name;
    if (this.initialized) {
      this.selectedAdapter = undefined;
      this.fragment = undefined; //unset hash anchor
    }
    this.getConfiguration();
    this.updateQueryParams();
  }

  updateQueryParams(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        loaded: this.loadedConfiguration ? null : 'false',
        adapter: this.selectedAdapter,
      },
      queryParamsHandling: 'merge',
      fragment: this.fragment,
      replaceUrl: true,
    });
  }

  clipboard(): void {
    if (this.configuration) {
      copyToClipboard(this.configuration);
    }
  }

  getConfiguration(): void {
    this.configurationsService
      .getConfiguration(this.selectedConfiguration, this.loadedConfiguration)
      .subscribe((data) => {
        this.configuration = data;
        this.editor.setValue(data).then(() => {
          this.initialized = true;
          this.highlightAdapter();
        });
      });
  }

  private highlightAdapter(): void {
    if (!this.selectedAdapter) {
      return;
    }
    const match = this.editor.findMatchForRegex(
      `<[aA]dapter.*? name="${this.selectedAdapter}".*?>(?:.|\\n)*?<\\/[aA]dapter>`,
    )?.[0];
    if (match) {
      this.editor.setLineNumberInRoute(match.range.startLineNumber, match.range.endLineNumber);
    }
  }

  private removeAdapterAfterLineSelection(fragment: string | null): void {
    if (this.selectedAdapter && fragment?.includes('L') && !fragment?.includes('-')) {
      this.selectedAdapter = undefined;
      this.skipParamsUpdate = true;
      this.updateQueryParams();
    }
  }
}
