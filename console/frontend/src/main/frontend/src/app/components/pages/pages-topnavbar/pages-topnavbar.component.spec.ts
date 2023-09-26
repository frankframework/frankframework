import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PagesTopnavbarComponent } from './pages-topnavbar.component';

describe('PagesTopnavbarComponent', () => {
  let component: PagesTopnavbarComponent;
  let fixture: ComponentFixture<PagesTopnavbarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ PagesTopnavbarComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PagesTopnavbarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
