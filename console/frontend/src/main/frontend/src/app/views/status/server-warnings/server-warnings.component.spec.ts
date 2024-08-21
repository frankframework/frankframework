import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ServerWarningsComponent } from './server-warnings.component';

describe('ServerWarningsComponent', () => {
  let component: ServerWarningsComponent;
  let fixture: ComponentFixture<ServerWarningsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ServerWarningsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ServerWarningsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
