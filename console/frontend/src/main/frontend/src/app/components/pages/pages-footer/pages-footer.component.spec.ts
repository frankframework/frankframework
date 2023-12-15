import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PagesFooterComponent } from './pages-footer.component';

describe('PagesFooterComponent', () => {
  let component: PagesFooterComponent;
  let fixture: ComponentFixture<PagesFooterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ PagesFooterComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PagesFooterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
