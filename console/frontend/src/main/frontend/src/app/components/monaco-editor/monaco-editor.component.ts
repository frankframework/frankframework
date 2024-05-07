// eslint-disable-next-line @typescript-eslint/triple-slash-reference
/// <reference path="../../../../node_modules/monaco-editor/monaco.d.ts" />
import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { first, ReplaySubject } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { line } from 'd3';

interface AMDRequire {
  require: {
    (imports: string[], callback: () => void): void;
    config(config: { paths: Record<string, string> }): void;
  };
}

@Component({
  selector: 'app-monaco-editor',
  standalone: true,
  imports: [],
  templateUrl: './monaco-editor.component.html',
  styleUrls: ['./monaco-editor.component.scss'],
})
export class MonacoEditorComponent
  implements AfterViewInit, OnChanges, OnDestroy
{
  private codeEditorInstanceSubject =
    new ReplaySubject<monaco.editor.IStandaloneCodeEditor>(1);
  codeEditorInstance$ = this.codeEditorInstanceSubject.asObservable();

  @Input()
  content?: string;
  @Input()
  options?: monaco.editor.IEditorOptions;

  @ViewChild('editor')
  protected editorContainer!: ElementRef;

  private codeEditorInstance?: monaco.editor.IStandaloneCodeEditor;
  private decorationsDelta: string[] = [];

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private zone: NgZone,
  ) {}

  ngAfterViewInit(): void {
    this.loadMonaco();
  }

  ngOnChanges(changes: SimpleChanges): void {
    const contentChanges = changes['content'];
    if (contentChanges && !contentChanges.isFirstChange()) {
      this.setContent(changes['content'].currentValue);
    }
  }

  ngOnDestroy(): void {
    this.codeEditorInstance?.dispose();
  }

  private loadMonaco(): void {
    if (typeof window.monaco === 'object') {
      this.initializeMonaco();
      return;
    }

    if ((window as unknown as AMDRequire).require) {
      this.onAmdLoader();
    } else {
      const loaderScript: HTMLScriptElement = document.createElement('script');
      loaderScript.type = 'text/javascript';
      loaderScript.src = 'assets/monaco/vs/loader.js';
      loaderScript.addEventListener('load', () => this.onAmdLoader());
      document.body.append(loaderScript);
    }
  }

  onAmdLoader(): void {
    const windowRequire = (window as unknown as AMDRequire).require;
    windowRequire.config({ paths: { vs: 'assets/monaco/vs' } });
    windowRequire(['vs/editor/editor.main'], () => {
      this.initializeMonaco();
    });
  }

  private initializeMonaco(): void {
    this.initializeEditor();
    this.initializeMouseEvents();
    this.selectLineNumberInRoute();
  }

  private initializeEditor(): void {
    this.codeEditorInstance = monaco.editor.create(
      this.editorContainer.nativeElement,
      {
        value: this.content,
        theme: 'vs-light',
        language: 'xml',
        inlineCompletionsAccessibilityVerbose: true,
        automaticLayout: true,
        selectOnLineNumbers: true,
        scrollBeyondLastLine: false,
        wordWrap: 'on',
        ...this.options,
      },
    );
    this.codeEditorInstanceSubject.next(this.codeEditorInstance);
  }

  private initializeMouseEvents(): void {
    this.codeEditorInstance?.onMouseDown((event) => {
      const element = event.target.element;
      if (element?.className === 'line-numbers') {
        const lineNumber = +(element.textContent || 0);
        if (lineNumber) {
          this.zone.run(() => {
            this.router
              .navigate([], {
                fragment: `L${lineNumber}`,
                queryParamsHandling: 'preserve',
              })
              .then(() => {
                this.highlightWholeLine(lineNumber);
                this.setPosition(lineNumber);
              });
          });
        }
      }
    });
  }

  private selectLineNumberInRoute(): void {
    this.route.fragment.pipe(first()).subscribe((hash) => {
      if (hash) {
        const lineNumber = +hash.replace('L', '');
        this.revealLineNearTop(lineNumber);
        this.highlightWholeLine(lineNumber);
        this.setPosition(lineNumber);
      }
    });
  }

  setContent(content: string): void {
    this.codeEditorInstance$.pipe(first()).subscribe((editor) => {
      editor.getModel()?.setValue(content);
    });
  }

  private revealLineNearTop(lineNumber: number): void {
    this.codeEditorInstance$.pipe(first()).subscribe((editor) => {
      editor.revealLineNearTop(lineNumber);
    });
  }

  private setPosition(lineNumber: number, columnNumber: number = 0): void {
    this.codeEditorInstance$.pipe(first()).subscribe((editor) => {
      editor.setPosition({ lineNumber: lineNumber, column: columnNumber });
    });
  }

  private highlightWholeLine(lineNumber: number): void {
    this.codeEditorInstance$.pipe(first()).subscribe((editor) => {
      this.decorationsDelta = editor.deltaDecorations(this.decorationsDelta, [
        {
          range: {
            startLineNumber: lineNumber,
            startColumn: 0,
            endLineNumber: lineNumber,
            endColumn: 0,
          },
          options: {
            isWholeLine: true,
            className: 'monaco-editor__line--highlighted',
          },
        },
      ]);
    });
  }
}
