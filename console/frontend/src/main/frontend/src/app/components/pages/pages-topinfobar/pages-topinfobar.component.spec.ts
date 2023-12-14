import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PagesTopinfobarComponent } from './pages-topinfobar.component';

describe('PagesTopinfobarComponent', () => {
  let component: PagesTopinfobarComponent;
  let fixture: ComponentFixture<PagesTopinfobarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ PagesTopinfobarComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PagesTopinfobarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
