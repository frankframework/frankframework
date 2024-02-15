import { Injectable } from '@angular/core';
import mermaid from 'mermaid';

@Injectable({
  providedIn: 'root'
})
export class NgMermaidService {

  private MERMAID_WORKER: Worker;

  constructor() {
    console.log('import meta url', import.meta.url);
    this.MERMAID_WORKER = new Worker(new URL('./ng-mermaid.worker', import.meta.url));
    this.MERMAID_WORKER.onmessage = ({ data }) => {
      console.log(`page got message: ${data}`);
    };
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

  sendMermaidRenderRequest(/* flowData: string */): void {
    this.MERMAID_WORKER.postMessage(['Test', mermaid.render]);
  }

  render(id: string, flowData: string): Promise<string> {
    return mermaid.render(id, flowData).then(res => res.svg);
  }
}
