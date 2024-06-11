import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { Client } from '@stomp/stompjs';

@Component({
  selector: 'app-websocket-test',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './websocket-test.component.html',
  styleUrl: './websocket-test.component.scss',
})
export class WebsocketTestComponent implements OnInit {
  private client: Client = new Client({
    brokerURL: 'ws://localhost:4200/iaf/api/ws',
    connectionTimeout: 60_000,
    debug: (message) => console.debug(message),
    onConnect: () => this.onConnected(),
  });

  ngOnInit(): void {
    this.client.activate();
  }

  onConnected(): void {
    this.client.subscribe('/topic/greetings', (message) => {
      console.log(`Received: ${message.body}`);
    });
    this.client.publish({
      destination: '/hello',
      body: JSON.stringify({ name: 'Vivy' }),
    });
  }
}
