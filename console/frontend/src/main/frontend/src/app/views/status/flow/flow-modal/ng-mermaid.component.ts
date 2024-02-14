import { CommonModule } from "@angular/common";
import { Component, ElementRef, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges, ViewChild } from "@angular/core";
import mermaid from 'mermaid';

type CSSRuleExtended = CSSRule & { selectorText: string };

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
  @Output() nmInitCallback = new EventEmitter();

  model = this.nmModel;
  interval = 2000;
  is_mermaid = 'mermaid';
  initialized = false;
  timeout?: number;

  @ViewChild('mermaidPre') mermaidEl!: ElementRef<HTMLElement>;

  private element = this.elRef.nativeElement;

  constructor(private elRef: ElementRef<HTMLElement>) { }

  ngOnInit() {
    this.render();
    this.initialized = true;
  }

  ngOnChanges(changes: SimpleChanges) {
    if(this.initialized)
      this.render();
  }

  render() {
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
          this.mermaidEl.nativeElement.innerHTML = this.nmModel;
          mermaid.initialize({
            startOnLoad: false, maxTextSize: 70 * 1000, maxEdges: 600, flowchart: {
              diagramPadding: 8,
              htmlLabels: true,
              curve: 'basis',
            },
          });
          mermaid.run({
            nodes: [this.mermaidEl.nativeElement]
          }).then(() => {
            const svgElement = this.mermaidEl.nativeElement.firstChild as HTMLElement;
            svgElement.setAttribute('height', '100%');
            svgElement.setAttribute('style', "");
          }).finally(() => {
            this.nmInitCallback.emit();
          });
        } catch (e) {
          if (e instanceof Error) {
            e.message.split('\n').forEach((v) => {
              this.mermaidEl.nativeElement.innerHTML = `<span>${v}</span><br/>`;
            });
          }
        }
      }, this.initialized ? this.interval : 0);
    }
  }

  cssReplace(cssRule: string) {
    return cssRule
      .replace('ng\:cloak', 'ng--cloak')
      .replace('ng\:form', 'ng--form');
  };
}
