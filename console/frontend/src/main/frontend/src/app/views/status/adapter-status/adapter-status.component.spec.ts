import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdapterStatusComponent } from './adapter-status.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('AdapterStatusComponent', () => {
  let component: AdapterStatusComponent;
  let fixture: ComponentFixture<AdapterStatusComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [AdapterStatusComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(AdapterStatusComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
