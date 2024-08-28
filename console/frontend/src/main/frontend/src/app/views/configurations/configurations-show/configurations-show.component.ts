import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService, Configuration } from 'src/app/app.service';
import { ConfigurationsService } from '../configurations.service';
import { copyToClipboard } from 'src/app/utils';
import { MonacoEditorComponent } from '../../../components/monaco-editor/monaco-editor.component';
import { Subscription } from 'rxjs';

type TransitionObject = {
  name?: string;
  loaded?: boolean;
  adapter?: string;
};

@Component({
  selector: 'app-configurations-show',
  templateUrl: './configurations-show.component.html',
  styleUrls: ['./configurations-show.component.scss'],
})
export class ConfigurationsShowComponent implements OnInit, OnDestroy {
  @ViewChild('editor') editor!: MonacoEditorComponent;

  protected configurations: Configuration[] = [];
  protected selectedConfiguration: string = 'All';
  protected loadedConfiguration: boolean = false;

  private configuration: string = '';
  private fragment?: string;
  private selectedAdapter?: string;
  private skipParamsUpdate: boolean = false;
  private configsSubscription: Subscription | null = null;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private configurationsService: ConfigurationsService,
    private appService: AppService,
  ) {}

  ngOnInit(): void {
    this.configurations = this.appService.configurations;
    this.configsSubscription = this.appService.configurations$.subscribe(() => {
      this.configurations = this.appService.configurations;
    });

    this.route.fragment.subscribe((fragment) => {
      this.fragment = fragment ?? undefined;
      this.removeAdapterAfterLineSelection(fragment);
    });

    this.route.queryParamMap.subscribe((parameters) => {
      if (this.skipParamsUpdate) {
        this.skipParamsUpdate = false;
        return;
      }
      this.selectedConfiguration = parameters.get('name') || 'All';
      this.loadedConfiguration = parameters.get('loaded') !== 'false';
      this.selectedAdapter = parameters.get('adapter') ?? undefined;

      this.getConfiguration();
    });
  }

  ngOnDestroy(): void {
    this.configsSubscription?.unsubscribe();
  }

  update(loaded: boolean): void {
    this.loadedConfiguration = loaded;
    this.fragment = undefined;
    this.updateQueryParams();
  }

  changeConfiguration(name: string): void {
    this.selectedConfiguration = name;
    this.selectedAdapter = undefined;
    this.fragment = undefined; //unset hash anchor
    this.updateQueryParams();
  }

  updateQueryParams(): void {
    const transitionObject: TransitionObject = {};
    if (this.selectedConfiguration !== 'All') transitionObject.name = this.selectedConfiguration;
    if (!this.loadedConfiguration) transitionObject.loaded = this.loadedConfiguration;
    transitionObject.adapter ??= this.selectedAdapter;

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: transitionObject,
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
