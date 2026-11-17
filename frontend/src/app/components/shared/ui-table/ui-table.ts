
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-ui-table',
  standalone: true,
  templateUrl: './ui-table.html',
  styleUrl: './ui-table.scss'
})
export class UiTable {
  @Input() columns: string[] = [];
  @Input() rows: Record<string, string>[] = [];
}
