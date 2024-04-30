// eslint-disable-next-line @typescript-eslint/triple-slash-reference
/// <reference path="../../../../node_modules/monaco-editor/monaco.d.ts" />
import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild,
} from '@angular/core';

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
  @ViewChild('editor')
  editorContainer!: ElementRef;
  @Input()
  content?: string;
  @Input()
  options?: monaco.editor.IEditorOptions;

  private codeEditorInstance!: monaco.editor.IStandaloneCodeEditor;

  ngAfterViewInit(): void {
    this.loadMonaco();
  }

  ngOnChanges(changes: SimpleChanges): void {
    this.setContent(changes['content'].currentValue);
  }

  ngOnDestroy(): void {
    this.codeEditorInstance.dispose();
  }

  private loadMonaco(): void {
    if (typeof window.monaco === 'object') {
      this.initializeMonaco();
      return;
    }

    if (window.require) {
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
    window.require.config({ paths: { vs: 'assets/monaco/vs' } });
    window.require(['vs/editor/editor.main'], () => {
      this.initializeMonaco();
    });
  }

  private initializeMonaco(): void {
    this.initializeEditor();
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
  }

  private setContent(content: string): void {
    this.codeEditorInstance?.getModel()?.setValue(content);
  }
}
