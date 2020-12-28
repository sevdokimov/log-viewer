import {Component} from '@angular/core';
import {FilterPanelStateService} from '@app/log-view/filter-panel-state.service';
import * as $ from 'jquery';

@Component({
    selector: 'lv-top-filter',
    templateUrl: './top-filter-list.component.html',
    styleUrls: ['./top-filter-list.component.scss'],
})
export class TopFilterListComponent {

    constructor(public filterPanelStateService: FilterPanelStateService) {
    }

    addDateFilter() {
        this.filterPanelStateService.updateFilterState(state => {
            state.date = {};
        });

        setTimeout(() => $('lv-date-interval .closeable-filter > span')[0]?.click(), 0);

        return false;
    }

    addThreadFilter() {
        this.filterPanelStateService.updateFilterState(state => {
            state.thread = {};
        });

        setTimeout(() => $('lv-thread-filter .closeable-filter > span')[0]?.click(), 0);

        return false;
    }

    addStacktraceFilter() {
        this.filterPanelStateService.updateFilterState(state => {
            state.exceptionsOnly = true;
        });

        return false;
    }
}
