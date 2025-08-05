import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { IMessage } from '@stomp/stompjs';
import { Subscription } from 'rxjs';
import { WebsocketService } from 'src/app/services/websocket.service';
import { QuickSubmitFormDirective } from '../../components/quick-submit-form.directive';

@Component({
  selector: 'app-websocket-test',
  imports: [RouterModule, FormsModule, QuickSubmitFormDirective],
  templateUrl: './websocket-test.component.html',
  styleUrl: './websocket-test.component.scss',
})
export class WebsocketTestComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('wsLog')
  private wsLog!: ElementRef<HTMLPreElement>;

  protected message = '';

  private _subscriptions = new Subscription();

  constructor(private websocketService: WebsocketService) {}

  ngOnInit(): void {
    this._subscriptions.add(this.websocketService.onConnected$.subscribe(() => this.onConnected()));
    this._subscriptions.add(this.websocketService.onDisconnected$.subscribe(() => this.printToHtml('Disconnected')));
    this._subscriptions.add(
      this.websocketService.onWebSocketClose$.subscribe(() => this.printToHtml('WebSocket closed')),
    );
    this._subscriptions.add(this.websocketService.onWebSocketError$.subscribe((event) => this.onError(event)));
    this._subscriptions.add(
      this.websocketService.onMessage$.subscribe((event) => this.debugHandler(event.channel, event.message)),
    );
  }

  ngAfterViewInit(): void {
    if (this.websocketService.connected()) {
      this.printToHtml('Connected');
      this.onConnected();
    }
  }

  ngOnDestroy(): void {
    this.websocketService.unsubscribe('/event/test');
    this._subscriptions.unsubscribe();
  }

  onConnected(): void {
    this.websocketService.subscribe('/event/test');
  }

  push(): void {
    this.websocketService.publish('/debug', JSON.stringify({ message: this.message }));
    this.message = '';
  }

  onError(event: unknown): void {
    this.printToHtml('WebSocket error');
    if (event) {
      this.printToHtml(JSON.stringify(event));
    }
  }

  private printToHtml(message: string): void {
    const htmlLogElement: HTMLPreElement = this.wsLog.nativeElement;
    htmlLogElement.innerHTML += `<p>${message}</p>`;
    htmlLogElement.scrollTo(0, htmlLogElement.scrollHeight);
  }

  private debugHandler(channel: string, message: IMessage): void {
    this.printToHtml(`${channel} | ${message.body}`);
  }
}
