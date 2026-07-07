import { Component, inject, OnDestroy, OnInit, Signal, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { NgClass } from '@angular/common';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faClipboard } from '@fortawesome/free-regular-svg-icons';

import { AppService, Configuration } from '../../../app.service';
import { ConfigurationsService } from '../configurations.service';
import { copyToClipboard } from '../../../utilities';
import { MonacoEditorComponent } from '../../../components/monaco-editor/monaco-editor.component';
import { ConfigurationTabListComponent } from '../../../components/tab-list/configuration-tab-list.component';

@Component({
  selector: 'app-configurations-show',
  imports: [ConfigurationTabListComponent, NgClass, MonacoEditorComponent, FaIconComponent],
  templateUrl: './configurations-show.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./configurations-show.component.scss'],
})
export class ConfigurationsShowComponent implements OnInit, OnDestroy {
  @ViewChild('editor') editor!: MonacoEditorComponent;

  protected selectedConfiguration = 'All';
  protected loadedConfiguration = false;
  protected configurations: Signal<Configuration[]>;
  protected readonly faClipboard = faClipboard;

  private configuration = '';
  private fragment?: string;
  private selectedAdapter: string | null = null;
  private skipParamsUpdate = false;
  private initialized = false;
  private configsSubscription: Subscription | null = null;
  private readonly appService: AppService = inject(AppService);
  private readonly router: Router = inject(Router);
  private readonly route: ActivatedRoute = inject(ActivatedRoute);
  private readonly configurationsService: ConfigurationsService = inject(ConfigurationsService);

  constructor() {
    this.configurations = this.appService.configurations;
  }

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
      this.selectedAdapter = parameters.get('adapter');
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
      this.selectedAdapter = null;
      this.fragment = undefined; // unset hash anchor
    }
    this.getConfiguration();
  }

  updateQueryParams(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        loaded: this.loadedConfiguration ? null : 'false',
      },
      queryParamsHandling: 'merge',
      fragment: this.fragment,
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
      String.raw`<[aA]dapter.*? name="${this.selectedAdapter}".*?>(?:.|\n)*?<\/[aA]dapter>`,
    )?.[0];
    if (match) {
      this.editor.setLineNumberInRoute(match.range.startLineNumber, match.range.endLineNumber);
    }
  }

  private removeAdapterAfterLineSelection(fragment: string | null): void {
    if (!(this.selectedAdapter && fragment?.includes('L') && !fragment?.includes('-'))) return;
    this.selectedAdapter = null;
    this.skipParamsUpdate = true;
    this.updateQueryParams();
  }
}
