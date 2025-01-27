import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { WebsocketTestComponent } from './websocket-test.component';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('WebsocketTestComponent', () => {
  let component: WebsocketTestComponent;
  let fixture: ComponentFixture<WebsocketTestComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WebsocketTestComponent],
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(WebsocketTestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
