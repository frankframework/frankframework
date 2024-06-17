import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { AppService } from '../app.service';
import { Observable, Subject } from 'rxjs';

type ChannelMessage = {
  channel: string;
  message: IMessage;
};

@Injectable({
  providedIn: 'root',
})
export class WebsocketService {
  private onConnectedSubject = new Subject<void>();
  private onDisconnectedSubject = new Subject<void>();
  private onWebSocketCloseSubject = new Subject<void>();
  private onWebSocketErrorSubject = new Subject<Error>();
  private onMessageSubject = new Subject<ChannelMessage>();

  onConnected$ = this.onConnectedSubject.asObservable();
  onDisconnected$ = this.onDisconnectedSubject.asObservable();
  onWebSocketClose$ = this.onWebSocketCloseSubject.asObservable();
  onWebSocketError$ = this.onWebSocketErrorSubject.asObservable();
  onMessage$ = this.onMessageSubject.asObservable();

  private client: Client = new Client({
    brokerURL: `ws://${window.location.host}${this.appService.absoluteApiPath}ws`,
    reconnectDelay: 20_000,
    connectionTimeout: 60_000,
    debug: (message) => console.debug(message),
    onConnect: () => this.onConnectedSubject.next(),
    onDisconnect: () => this.onDisconnectedSubject.next(),
    onWebSocketClose: () => this.onWebSocketCloseSubject.next(),
    onWebSocketError: (event) => this.onWebSocketErrorSubject.next(event),
  });
  private stompSubscriptions: Map<string, StompSubscription> = new Map<
    string,
    StompSubscription
  >();

  constructor(
    private http: HttpClient,
    private appService: AppService,
  ) {}

  activate(): void {
    if (!this.client.connected) {
      this.client.activate();
    }
  }

  deactivate(): void {
    if (this.client.connected) {
      this.client.deactivate();
    }
  }

  connected(): boolean {
    return this.client.connected;
  }

  publish(channel: string, message: string): void {
    this.client.publish({ destination: channel, body: message });
  }

  push(message: string): Observable<void> {
    return this.http.post<void>(
      `${this.appService.absoluteApiPath}event/push`,
      JSON.stringify({ message }),
      {
        headers: { 'Content-Type': 'application/json' },
      },
    );
  }

  subscribe<T>(channel: string, callback?: (message: T) => void): void {
    const subscription = this.client.subscribe(channel, (message: IMessage) => {
      this.onMessageSubject.next({ channel, message });
      if (callback) callback(JSON.parse(message.body) as T);
    });
    this.stompSubscriptions.set(channel, subscription);
  }

  unsubscribe(channel: string): void {
    const subscription = this.stompSubscriptions.get(channel);
    if (subscription) {
      subscription.unsubscribe();
      this.stompSubscriptions.delete(channel);
    }
  }
}
