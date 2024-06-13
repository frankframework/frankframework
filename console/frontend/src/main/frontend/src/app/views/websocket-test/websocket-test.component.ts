import { CommonModule } from '@angular/common';
import {
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { IMessage } from '@stomp/stompjs';
import { Subscription } from 'rxjs';
import { WebsocketService } from 'src/app/services/websocket.service';

@Component({
  selector: 'app-websocket-test',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './websocket-test.component.html',
  styleUrl: './websocket-test.component.scss',
})
export class WebsocketTestComponent implements OnInit, OnDestroy {
  @ViewChild('wsLog')
  private wsLog!: ElementRef<HTMLPreElement>;

  protected message: string = '';

  private _subscriptions = new Subscription();

  /* private client: Client = new Client({
    brokerURL: 'ws://localhost:4200/iaf/api/ws',
    reconnectDelay: 20_000,
    connectionTimeout: 60_000,
    debug: (message) => console.debug(message),
    onConnect: () => this.onConnected(),
    onDisconnect: () => this.printToHtml('Disconnected'),
    onWebSocketClose: () => this.printToHtml('WebSocket closed'),
    onWebSocketError: (event) => this.onError(event),
  }); */

  constructor(private websocketService: WebsocketService) {}

  ngOnInit(): void {
    this._subscriptions.add(
      this.websocketService.onConnected$.subscribe(() => this.onConnected()),
    );
    this._subscriptions.add(
      this.websocketService.onDisconnected$.subscribe(() =>
        this.printToHtml('Disconnected'),
      ),
    );
    this._subscriptions.add(
      this.websocketService.onWebSocketClose$.subscribe(() =>
        this.printToHtml('WebSocket closed'),
      ),
    );
    this._subscriptions.add(
      this.websocketService.onWebSocketError$.subscribe((event) =>
        this.onError(event),
      ),
    );
    this._subscriptions.add(
      this.websocketService.onMessage$.subscribe((event) =>
        this.debugHandler(event.channel, event.message),
      ),
    );
  }

  ngOnDestroy(): void {
    this._subscriptions.unsubscribe();
  }

  onConnected(): void {
    this.websocketService.publish('/hello', JSON.stringify({ name: 'Vivy' }));
  }

  push(): void {
    this.websocketService
      .push(JSON.stringify({ message: this.message }))
      .subscribe(() => {
        this.message = '';
      });
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
