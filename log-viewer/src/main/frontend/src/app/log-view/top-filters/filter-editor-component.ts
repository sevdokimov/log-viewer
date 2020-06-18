import {Subscription} from 'rxjs';
import {OnDestroy, OnInit} from '@angular/core';
import {FilterPanelStateService, FilterState} from '@app/log-view/filter-panel-state.service';

export abstract class FilterEditorComponent implements OnInit, OnDestroy {

    private filterStateSbscr: Subscription;

    constructor(protected filterPanelStateService: FilterPanelStateService) {
    }

    ngOnInit() {
        this.filterStateSbscr = this.filterPanelStateService.filterChanges.subscribe((state: FilterState) => {
            this.loadComponentState(state);
        });
        this.loadComponentState(this.filterPanelStateService.getFilterState());
    }

    ngOnDestroy(): void {
        if (this.filterStateSbscr) {
            this.filterStateSbscr.unsubscribe();
        }
    }

    protected abstract loadComponentState(state: FilterState): void;
}
