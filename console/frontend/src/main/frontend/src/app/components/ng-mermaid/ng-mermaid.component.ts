import { CommonModule } from "@angular/common";
import { Component, ElementRef, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges, ViewChild } from "@angular/core";
import mermaid from 'mermaid';

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
export class NgMermaidComponent implements OnInit, OnChanges {
  @Input() nmModel?: any;
  @Input() nmRefreshInterval?: number;
  @Input() id?: string;
  @Output() nmInitCallback = new EventEmitter();

  model = this.nmModel;
  interval = 2000;
  is_mermaid = 'mermaid';
  initialized = false;
  timeout?: number;

  @ViewChild('mermaidPre') mermaidEl!: ElementRef<HTMLElement>;

  private element = this.elRef.nativeElement;

  constructor(
    private elRef: ElementRef<HTMLElement>,
  ) { }

  ngOnInit() {
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

    this.renderAsync();
    this.initialized = true;
  }

  ngOnChanges(changes: SimpleChanges) {
    if (this.initialized)
      this.renderAsync();
  }

  renderAsync() {
    if (this.nmRefreshInterval) {
      this.interval = this.nmRefreshInterval || this.interval;
    }

    if (this.nmModel) {
      this.model = this.nmModel;
      this.element.querySelectorAll("[data-processed]").forEach((v, k) => {
        v.removeAttribute("data-processed");
      });
      if (this.timeout)
        window.clearTimeout(this.timeout);
      this.timeout = window.setTimeout(() => {
        try {
          const uid = 'm' + (this.id ?? +(new Date).getTime());
          mermaid.render(uid, this.nmModel).then(({ svg }) => {
            this.mermaidEl.nativeElement.innerHTML = svg;
            const svgElement = this.mermaidEl.nativeElement.firstChild as HTMLElement;
            svgElement.setAttribute('height', '100%');
            svgElement.setAttribute('style', "");
          }).catch(e => { this.handleError(e) })
            .finally(() => {
              this.nmInitCallback.emit();
            });
        } catch (e) {
          this.handleError(e as Error);
        }
      }, this.initialized ? this.interval : 0);
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
