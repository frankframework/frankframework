/// <reference path="../../../../node_modules/monaco-editor/monaco.d.ts" />
import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { first, ReplaySubject } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';

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
export class MonacoEditorComponent implements AfterViewInit, OnChanges, OnDestroy {
  private editorSubject = new ReplaySubject<monaco.editor.IStandaloneCodeEditor>(1);
  editor$ = this.editorSubject.asObservable();

  @Input()
  value?: string;
  @Output()
  valueChange = new EventEmitter<string>();
  @Input()
  options?: Partial<monaco.editor.IStandaloneEditorConstructionOptions>;
  @Input()
  actions?: {
    ctrlEnter?: monaco.editor.IActionDescriptor;
  };

  @ViewChild('editor')
  protected editorContainer!: ElementRef;

  private editor?: monaco.editor.IStandaloneCodeEditor;
  private decorationsDelta: string[] = [];
  private skipFragmentUpdate: boolean = false;

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
      this.setValue(changes['content'].currentValue);
    }
  }

  ngOnDestroy(): void {
    this.editor?.dispose();
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
    this.initializeEvents();
    this.initializeActions();
    this.initializeMouseEvents();
    this.highlightTheLineNumberInRoute();
  }

  private initializeEditor(): void {
    this.editor = monaco.editor.create(this.editorContainer.nativeElement, {
      value: this.value,
      theme: 'vs-light',
      language: 'xml',
      automaticLayout: true,
      scrollBeyondLastLine: false,
      wordWrap: 'on',
      minimap: { enabled: false },
      ...this.options,
    });
    this.editorSubject.next(this.editor);
  }

  private initializeEvents(): void {
    this.editor$.pipe(first()).subscribe((editor) => {
      editor.onDidChangeModelContent(() => {
        this.valueChange.emit(editor.getValue());
      });
    });
  }

  private initializeActions(): void {
    const actionKeyBindings: Record<string, number[]> = {
      ctrlEnter: [monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter],
    };
    this.editor$.pipe(first()).subscribe((editor) => {
      for (const [action, descriptor] of Object.entries(this.actions || {})) {
        editor.addAction({
          id: descriptor.id,
          label: descriptor.label,
          keybindings: actionKeyBindings[action],
          run: descriptor.run,
        });
      }
    });
  }

  private initializeMouseEvents(): void {
    this.editor?.onMouseDown((event) => {
      const element = event.target.element;
      switch (element?.className) {
        case 'line-numbers': {
          this.handleLineNumberClick(+(element.textContent || 0));
          break;
        }
        default: {
          return;
        }
      }
    });
  }

  private handleLineNumberClick(lineNumber: number): void {
    if (lineNumber) {
      this.skipFragmentUpdate = true;
      this.setLineNumberInRoute(lineNumber);
      this.highlightLine(lineNumber);
      this.setPosition(lineNumber);
    }
  }

  setLineNumberInRoute(startLineNumber: number, endLineNumber: number | undefined = undefined): void {
    let fragment = `L${startLineNumber}`;
    if (endLineNumber) fragment += `-${endLineNumber}`;
    this.zone.run(() => {
      this.router.navigate([], {
        fragment: fragment,
        queryParamsHandling: 'preserve',
      });
    });
  }

  private highlightLine(lineNumber: number): void {
    this.highlightRange({
      startLineNumber: lineNumber,
      startColumn: 0,
      endLineNumber: lineNumber,
      endColumn: 0,
    });
  }

  private highlightRange(range: monaco.IRange): void {
    this.editor$.pipe(first()).subscribe((editor) => {
      this.decorationsDelta =
        editor.getModel()?.deltaDecorations(this.decorationsDelta, [
          {
            range: range,
            options: {
              isWholeLine: true,
              overviewRuler: {
                position: monaco.editor.OverviewRulerLane.Full,
                color: '#fdc300',
              },
              className: 'monaco-editor__line--highlighted',
            },
          },
        ]) ?? [];
    });
  }

  private setPosition(lineNumber: number, column: number = 0): void {
    this.editor$.pipe(first()).subscribe((editor) => {
      editor.setPosition({ lineNumber: lineNumber, column: column });
    });
  }

  private highlightTheLineNumberInRoute(): void {
    this.route.fragment.subscribe((fragment) => {
      if (!fragment || this.skipFragmentUpdate) {
        this.skipFragmentUpdate = false;
        return;
      }
      const range = fragment.replace('L', '');
      const [startLineNumber, endLineNumber]: string[] = range.split('-');
      this.revealLineNearTop(+startLineNumber);
      this.setPosition(+startLineNumber);
      if (endLineNumber) {
        this.highlightRange({
          startLineNumber: +startLineNumber,
          startColumn: 0,
          endLineNumber: +endLineNumber,
          endColumn: 0,
        });
      } else {
        this.highlightLine(+startLineNumber);
      }
    });
  }

  private revealLineNearTop(lineNumber: number): void {
    this.editor$.pipe(first()).subscribe((editor) => {
      editor.revealLineNearTop(lineNumber);
    });
  }

  async setValue(content: string): Promise<void> {
    return new Promise<void>((resolve) => {
      this.editor$.pipe(first()).subscribe((editor) => {
        editor.getModel()?.setValue(content);
        resolve();
      });
    });
  }

  findMatchForRegex(regexp: string): monaco.editor.FindMatch[] | undefined {
    let matches: monaco.editor.FindMatch[] | undefined;
    this.editor$.pipe(first()).subscribe((editor) => {
      matches = editor.getModel()?.findMatches(regexp, false, true, true, null, false);
    });
    return matches;
  }
}
