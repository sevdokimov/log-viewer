import {Component} from '@angular/core';
import {FilterPanelStateService} from '@app/log-view/filter-panel-state.service';

@Component({
    selector: 'lv-top-filter',
    templateUrl: './top-filter-list.component.html',
    styleUrls: ['./top-filter-list.component.scss'],
})
export class TopFilterListComponent {

    constructor(public filterPanelStateService: FilterPanelStateService) {
    }

}
