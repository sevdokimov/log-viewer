import {Component, ElementRef, ViewChild} from '@angular/core';
import {FilterPanelStateService, FilterState} from '@app/log-view/filter-panel-state.service';
import {FilterWithDropdown} from '@app/log-view/top-filters/filter-with-dropdown';
import {LvUtils} from '@app/utils/utils';
import {LanguageService} from "@app/log-view/language-service";

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
    languageService: LanguageService;

    constructor(filterPanelStateService: FilterPanelStateService, languageService: LanguageService) {
        super(filterPanelStateService);
        this.languageService = languageService;
    }

    protected getDropdownDiv(): ElementRef {
        return this.dropDownElement;
    }

    protected loadComponentState(state: FilterState) {
        this.title = LvThreadFilterComponent.calculateTitle(state, this.languageService);

        this.includes = [...(state.thread?.includes ?? [])];
        this.excludes = [...(state.thread?.excludes ?? [])];
    }

    static calculateTitle(state: FilterState, languageService: LanguageService): string {
        if (!state.thread?.includes?.length && !state.thread?.excludes?.length) {
            return languageService.getTranslate('THREAD_FILTER.EMPTY_THREAD_FILTER');
        }

        const threadIncludesCount = state.thread.includes?.length;
        const threadExcludesCount = state.thread.excludes?.length;

        if (threadIncludesCount === 1 && !threadExcludesCount) {
            return state.thread.includes[0];
        }

        if (threadExcludesCount === 1 && !threadIncludesCount) {
            return '- ' + state.thread.excludes[0];
        }

        let res = '';

        if (threadIncludesCount) {
            const visible = languageService.getTranslate('THREAD_FILTER.VISIBLE',
                {count: threadIncludesCount});

            res += `${threadIncludesCount} ${visible}`;
        }

        if (threadExcludesCount) {
            if (res.length > 0) {
                res += ', ';
            }
            const hidden = languageService.getTranslate('THREAD_FILTER.HIDDEN',
                {count: threadExcludesCount});

            res += `${threadExcludesCount} ${hidden}`;
        }

        if (res.length > 0) {
            const countThreads = threadExcludesCount ? threadExcludesCount : threadIncludesCount;
            const threads = languageService.getTranslate('THREAD_FILTER.HIDDEN_AND_VISIBLE_THREADS',
                {count: countThreads});

            res += ` ${threads}`;
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

            LvUtils.delete(state.thread.includes, thread);
            LvUtils.delete(state.thread.excludes, thread);
        });

        LvUtils.delete(this.excludes, thread);
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
                LvUtils.delete(state.thread.excludes, thread);

                if (!state.thread.includes) {
                    state.thread.includes = [thread];
                } else {
                    LvUtils.addIfNotExist(state.thread.includes, thread);
                }
            } else {
                LvUtils.delete(state.thread.includes, thread);

                if (!state.thread.excludes) {
                    state.thread.excludes = [thread];
                } else {
                    LvUtils.addIfNotExist(state.thread.excludes, thread);
                }
            }

            this.threadInput = '';
            this.dropdownShown = false;
        });
    }
}
