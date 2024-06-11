import { CommonModule } from '@angular/common';
import {
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { RouterModule } from '@angular/router';
import { Client } from '@stomp/stompjs';

@Component({
  selector: 'app-websocket-test',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './websocket-test.component.html',
  styleUrl: './websocket-test.component.scss',
})
export class WebsocketTestComponent implements OnInit, OnDestroy {
  @ViewChild('wsLog')
  private wsLog!: ElementRef<HTMLPreElement>;

  private client: Client = new Client({
    brokerURL: 'ws://localhost:4200/iaf/api/ws',
    connectionTimeout: 60_000,
    debug: (message) => console.debug(message),
    onConnect: () => this.onConnected(),
  });

  ngOnInit(): void {
    this.client.activate();
  }

  ngOnDestroy(): void {
    this.client.deactivate();
  }

  onConnected(): void {
    const htmlLogElement: HTMLPreElement = this.wsLog.nativeElement;
    this.client.subscribe('/topic/greetings', (message) => {
      console.log(`Received: ${message.body}`);
      htmlLogElement.innerHTML += `<p>${message.body}</p>`;
    });
    this.client.publish({
      destination: '/hello',
      body: JSON.stringify({ name: 'Vivy' }),
    });
  }
}
