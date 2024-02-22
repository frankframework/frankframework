import { CommonModule } from "@angular/common";
import { Component, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges, ViewChild } from "@angular/core";
import mermaid from 'mermaid';
import { v4 as uuidv4 } from 'uuid';


@Component({
  standalone: true,
  selector: 'ng-mermaid',
  imports: [
    CommonModule
  ],
  template: `
    <div
      class="{{is_mermaid}}"
      #mermaidPre
    >Loading...</div>
  `,
  styles: [`
    div {
      width: 100%;
      height: 100%;
    }
  `]
})
export class NgMermaidComponent implements OnInit, OnChanges, OnDestroy {
  @Input() nmModel?: any;
  @Input() nmRefreshInterval?: number;
  @Input() divId?: string;
  @Output() nmInitCallback = new EventEmitter();

  @ViewChild('mermaidPre') mermaidEl!: ElementRef<HTMLElement>;

  protected interval = 2000;
  protected is_mermaid = 'mermaid';
  protected initialized = false;
  protected firstRender = true;
  protected timeout?: number;

  constructor() {
    mermaid.initialize({
      startOnLoad: false,
      maxTextSize: 70 * 1000,
      maxEdges: 600,
      flowchart: {
        diagramPadding: 8,
        htmlLabels: true,
        curve: 'basis',
      },
    });
  }

  ngOnInit() {
    this.render();
    this.initialized = true;
  }

  ngOnChanges(changes: SimpleChanges) {
    if (this.initialized)
      this.render();
  }

  ngOnDestroy() {
    this.mermaidEl.nativeElement.innerHTML = 'Loading...'
  }

  render() {
    if (this.nmRefreshInterval) {
      this.interval = this.nmRefreshInterval || this.interval;
    }

    if (this.nmModel) {
      if (this.timeout) {
        window.clearTimeout(this.timeout);
      }

      this.timeout = window.setTimeout(() => {
        try {
          const mermaidContainer = this.mermaidEl.nativeElement;
          mermaidContainer.innerHTML = 'Loading...';
          const uid = `m${uuidv4()}`;
          mermaid.render(uid, this.nmModel, mermaidContainer).then(({ svg }) => {
            mermaidContainer.innerHTML = svg;
            const svgElement = mermaidContainer.firstChild as HTMLElement;
            svgElement.setAttribute('height', '100%');
            svgElement.setAttribute('style', "");
          }).catch(e => { this.handleError(e) })
            .finally(() => {
              this.firstRender = false;
              this.nmInitCallback.emit();
            });
        } catch (e) {
          this.handleError(e as Error);
        }
      }, this.firstRender ? 0 : this.interval);
    }
  }

  handleError(e: Error) {
    console.error(e);
    let errorContainer = '';
    errorContainer += `<div style="display: inline-block; text-align: left; color: red; margin: 8px auto; font-family: Monaco,Consolas,Liberation Mono,Courier New,monospace">`;
    e.message.split('\n').forEach((v) => {
      errorContainer += `<span>${v}</span><br/>`;
    });
    errorContainer += `</div>`;
    this.mermaidEl.nativeElement.innerHTML += errorContainer;
  }
}
