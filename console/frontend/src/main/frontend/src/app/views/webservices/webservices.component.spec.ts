import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WebservicesComponent } from './webservices.component';

describe('WebservicesComponent', () => {
  let component: WebservicesComponent;
  let fixture: ComponentFixture<WebservicesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ WebservicesComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(WebservicesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
