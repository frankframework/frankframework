import { Component, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService, Configuration } from 'src/app/app.service';
import { ConfigurationsService } from '../configurations.service';
import { copyToClipboard } from 'src/app/utils';
import { MonacoEditorComponent } from '../../../components/monaco-editor/monaco-editor.component';

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
export class ConfigurationsShowComponent implements OnInit {
  @ViewChild('editor') editor!: MonacoEditorComponent;

  configurations: Configuration[] = [];
  configuration: string = '';
  selectedConfiguration: string = 'All';
  loadedConfiguration: boolean = false;
  anchor = '';

  private selectedAdapter?: string;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private configurationsService: ConfigurationsService,
    private appService: AppService,
  ) {}

  ngOnInit(): void {
    this.configurations = this.appService.configurations;
    this.appService.configurations$.subscribe(() => {
      this.configurations = this.appService.configurations;
    });

    this.route.fragment.subscribe((hash) => {
      this.anchor = hash ?? '';
    });

    this.route.queryParamMap.subscribe((parameters) => {
      this.selectedConfiguration =
        parameters.has('name') && parameters.get('name') != ''
          ? parameters.get('name')!
          : 'All';
      this.loadedConfiguration = !(
        parameters.has('loaded') && parameters.get('loaded') == 'false'
      );
      if (parameters.has('adapter'))
        this.selectedAdapter = parameters.get('adapter')!;

      this.getConfiguration();
    });
  }

  update(loaded: boolean): void {
    this.loadedConfiguration = loaded;
    this.anchor = '';
    this.getConfiguration();
  }

  changeConfiguration(name: string): void {
    this.selectedConfiguration = name;
    this.selectedAdapter = undefined;
    this.router.navigate([], { relativeTo: this.route, fragment: '' });
    this.anchor = ''; //unset hash anchor
    this.getConfiguration();
  }

  updateQueryParams(): void {
    const transitionObject: TransitionObject = {};
    if (this.selectedConfiguration != 'All')
      transitionObject.name = this.selectedConfiguration;
    if (!this.loadedConfiguration)
      transitionObject.loaded = this.loadedConfiguration;
    if (this.selectedAdapter) transitionObject.adapter = this.selectedAdapter;

    const fragment = this.anchor == '' ? undefined : this.anchor;

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: transitionObject,
      fragment,
    });
  }

  clipboard(): void {
    if (this.configuration) {
      copyToClipboard(this.configuration);
    }
  }

  getConfiguration(): void {
    this.updateQueryParams();

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
      `<adapter.*? name="${this.selectedAdapter}".*?>(?:.|\\n)*?<\\/adapter>`,
    )?.[0];
    if (match) {
      this.editor.setLineNumberInRoute(
        match.range.startLineNumber,
        match.range.endLineNumber,
      );
    }
  }
}
