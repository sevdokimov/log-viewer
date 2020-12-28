import {Component, ElementRef, ViewChild} from '@angular/core';
import {FilterPanelStateService, FilterState} from '@app/log-view/filter-panel-state.service';
import {FilterWithDropdown} from '@app/log-view/top-filters/filter-with-dropdown';
import {SlUtils} from '@app/utils/utils';

@Component({
    selector: 'lv-thread-filter',
    templateUrl: './thread-filter.component.html',
    styleUrls: ['./thread-filter.component.scss'],
})
export class LvThreadFilterComponent extends FilterWithDropdown {

    @ViewChild('threadDropDown', {static: true})
    private dropDownElement: ElementRef;

    @ViewChild('inputElement', {static: false})
    private inputElement: ElementRef;

    title: string;

    includes: string[];
    excludes: string[];

    threadInput: string = '';

    constructor(filterPanelStateService: FilterPanelStateService) {
        super(filterPanelStateService);
    }

    protected getDropdownDiv(): ElementRef {
        return this.dropDownElement;
    }

    protected loadComponentState(state: FilterState) {
        this.title = LvThreadFilterComponent.calculateTitle(state);

        this.includes = [...(state.thread?.includes ?? [])];
        this.excludes = [...(state.thread?.excludes ?? [])];
    }

    static calculateTitle(state: FilterState): string {
        if (!state.thread?.includes?.length && !state.thread?.excludes?.length) {
            return 'Empty thread filter';
        }

        if (state.thread.includes?.length === 1 && !state.thread.excludes?.length) {
            return state.thread.includes[0];
        }

        if (state.thread.excludes?.length === 1 && !state.thread.includes?.length) {
            return '- ' + state.thread.excludes[0];
        }

        let res = '';

        if (state.thread.includes?.length) {
            res += state.thread.includes?.length + ' visible';
        }

        if (state.thread.excludes?.length) {
            if (res.length > 0) {
                res += ', ';
            }
            res += state.thread.excludes?.length + ' hidden';
        }

        if (res.length > 0) {
            res += ' threads';
        }

        return res;
    }

    toggleFilterPanel() {
        super.toggleFilterPanel();

        if (this.dropdownShown) {
            if (!this.excludes.length && !this.includes.length) {
                setTimeout(() => this.inputElement.nativeElement.focus(), 0);
            }
        }
    }

    removeFilter() {
        this.filterPanelStateService.updateFilterState(state => {
            state.thread = null;
        });
    }

    removeThread(thread: string) {
        this.filterPanelStateService.updateFilterState(state => {
            if (!state.thread) {
                return;
            }

            SlUtils.delete(state.thread.includes, thread);
            SlUtils.delete(state.thread.excludes, thread);
        });
        
        SlUtils.delete(this.excludes, thread);
    }

    addThreadName(show: boolean) {
        let thread = this.threadInput.trim();
        if (!thread) {
            return;
        }

        this.filterPanelStateService.updateFilterState(state => {
            if (!state.thread) {
                state.thread = {};
            }

            if (show) {
                SlUtils.delete(state.thread.excludes, thread);

                if (!state.thread.includes) {
                    state.thread.includes = [thread];
                } else {
                    SlUtils.addIfNotExist(state.thread.includes, thread);
                }
            } else {
                SlUtils.delete(state.thread.includes, thread);

                if (!state.thread.excludes) {
                    state.thread.excludes = [thread];
                } else {
                    SlUtils.addIfNotExist(state.thread.excludes, thread);
                }
            }

            this.threadInput = '';
            this.dropdownShown = false;
        });
    }
}
