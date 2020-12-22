import {
    AfterViewChecked,
    ChangeDetectorRef,
    Component,
    ElementRef,
    HostListener,
    OnDestroy,
    OnInit,
    ViewChild
} from '@angular/core';
import {ActivatedRoute, Params, Router} from '@angular/router';
import {BackendEventHandler, BackendEventHandlerHolder, Command, CommunicationService} from './communication.service';
import {Record} from './record';
import {ViewConfigService} from './view-config.service';
import {SearchPattern, SearchUtils} from './search';
import {NotPredicate, Predicate, SubstringPredicate} from './predicates';
import {SlUtils} from '../utils/utils';
import {Md5} from 'ts-md5/dist/md5';
import {RecordRendererService} from './record-renderer.service';
import {ViewStateService} from './view-state.service';
import {FavoritesService} from '../services/favorites.service';
import {State} from './log-view-states';
import {LogFile} from './log-file';
import {Position} from './position';
import {
    BackendErrorEvent,
    EventInitByPermalink,
    EventNextDataLoaded,
    EventResponseAfterFilterChanged,
    EventResponseAfterFilterChangedSingle,
    EventScrollToEdgeResponse,
    EventSearchResponse,
    EventSetViewState,
    EventsLogChanged,
    StatusHolderEvent,
    UiConfig,
    UiConfigValidator
} from './backend-events';
import * as $ from 'jquery';
import {BsDropdownDirective} from 'ngx-bootstrap';
import {ToastrService} from 'ngx-toastr';
import {HttpClient} from '@angular/common/http';
import {FilterPanelStateService, FilterState} from '@app/log-view/filter-panel-state.service';
import {Subscription} from 'rxjs';
import {ContextMenuComponent, ContextMenuService} from 'ngx-contextmenu';

@Component({
    selector: 'sl-log-view',
    templateUrl: './log-view.template.html',
    styleUrls: ['./log-view.style.scss'],
    providers: [
        CommunicationService,
        ViewConfigService,
        RecordRendererService,
        ViewStateService,
        FilterPanelStateService,
    ],
})
export class LogViewComponent implements OnInit, OnDestroy, AfterViewChecked, BackendEventHandlerHolder {
    private static lineHeight: number = 16;

    @ViewChild('logView', {static: true})
    logView: ElementRef;
    @ViewChild('logPane', {static: true})
    logPane: ElementRef;
    @ViewChild('records', {static: true})
    records: ElementRef;
    @ViewChild('loadingProgressTop', {static: true})
    loadingProgressTop: ElementRef;

    @ViewChild('filtersDd', { read: BsDropdownDirective })
    namedFilterDropDown: BsDropdownDirective;

    @ViewChild('eventContextMenu', {static: true}) public eventContextMenu: ContextMenuComponent;

    logs: LogFile[] = [];

    backendError: string;
    disconnectMessage: string;

    state: State = State.STATE_INIT;
    stateVersion: number = 0;

    m: Record[] = [];

    hasRecordBefore: boolean = true;
    hasRecordAfter: boolean = true;

    shiftView: number = 0;

    searchRequest: SearchRequest;
    searchRequestIdCounter = 1;

    searchInputState: SearchPattern;

    searchPattern: SearchPattern;
    searchMatchCase: boolean;
    searchRegex: boolean;
    searchRegexError: string;

    searchHideUnmatched: boolean;

    savedFilterStates: { [key: string]: FilterState } = {};
    selectedFilterStateName: string;

    effectiveFilters: Predicate[] = [];

    private filterStateSbscr: Subscription;

    modalWindow: string;

    inFavorites: boolean;

    anchor: number;

    filterErrorText: string;

    recordWithDetails: Record;

    constructor(
        private changeDetectorRef: ChangeDetectorRef,
        private http: HttpClient,
        private route: ActivatedRoute,
        private router: Router,
        private commService: CommunicationService,
        private viewConfig: ViewConfigService,
        private recRenderer: RecordRendererService,
        public vs: ViewStateService,
        public fwService: FavoritesService,
        private toastr: ToastrService,
        private contextMenuService: ContextMenuService,
        private filterPanelStateService: FilterPanelStateService,
    ) {
    }

    private fail(error: string) {
        console.error(error);
        this.state = State.STATE_DISCONNECTED;
        this.backendError = error;
        this.commService.close();
    }

    private static parseFilterState(stateJson: string): FilterState {
        if (!stateJson) { return null; }

        try {
            let filterState = JSON.parse(stateJson);

            if (typeof filterState !== 'object') {
                console.error('Filter panel state is not an object');
                return null;
            }

            return filterState;
        } catch (e) {
            console.error('Failed to parse filter panel state', e);
            return null;
        }
    }

    @HostListener('window:resize', ['$event'])
    onResize() {
        if (this.state !== State.STATE_OPENED) { return; }

        this.tryGrow();
    }

    goToTailAfterBrokenLink() {
        let p = this.createParams();
        this.router.navigate([], {queryParams: p});
        this.modalWindow = null;
        this.cleanAndScrollToEdge();
    }

    hideUnmatchedChanged() {
        this.checkQuickSearch();
        this.applySearchFilter();
    }

    applySearchFilter() {
        if (this.state !== State.STATE_OPENED) { return; }

        this.searchPattern = this.searchInputState;
        this.rehighlightSearchOccurrences();
        this.activeFilterChanged();
    }

    private filtersChanged() {
        let p = this.createParams();
        this.router.navigate([], {queryParams: p});
        this.activeFilterChanged();
    }

    setSelectedLine(idx: number) {
        if (this.vs.selectedLine != null) {
            if (Record.containPosition(this.vs.selectedLine, this.m[idx])) { return; }
        }

        $('.record.selected-line', this.records.nativeElement).removeClass(
            'selected-line'
        );
        $(this.records.nativeElement.children[idx]).addClass('selected-line');

        this.vs.selectedLine = Position.recordStart(this.m[idx]);
    }

    private static getLineIndex(e: Element) {
        let res = 0;

        let s = e;
        while (s.previousSibling) {
            res++;
            s = <Element>s.previousSibling;
        }

        return res;
    }

    contextMenu(event: MouseEvent) {
        if (event.shiftKey && event.ctrlKey) {
            return true; // Open default menu for debugging.
        }

        if (this.state !== State.STATE_OPENED || this.modalWindow != null) { return false; }

        for (let e = <Element>event.target; e; e = e.parentElement) {
            if (e.parentElement && e.parentElement.id === 'records') {
                let index = LogViewComponent.getLineIndex(e);

                this.setSelectedLine(index);

                this.contextMenuService.show.next({
                    contextMenu: this.eventContextMenu,
                    event: event,
                    item: this.m[index],
                });

                event.preventDefault();
                event.stopPropagation();
                break;
            }
        }

        return false;
    }

    showEventDetails(record: Record) {
        this.recordWithDetails = record;
        this.modalWindow = 'event-details';
    }

    clickRecord(event: MouseEvent) {
        if (this.state !== State.STATE_OPENED || this.modalWindow != null) { return; }

        if (window.getSelection().toString().length > 0) { return; }

        if (this.recRenderer.handleClick(event)) {
            return;
        }

        for (let e = <Element>event.target; e; e = e.parentElement) {
            if (e.classList.contains('filtering-error')) {
                this.modalWindow = 'filter-error';
                this.filterErrorText = (<any>e).errorText;
                return;
            }

            if (e.parentElement && e.parentElement.id === 'records') {
                let index = LogViewComponent.getLineIndex(e);

                this.setSelectedLine(index);

                if ((<Element>event.target).classList.contains('rec-pointer')) {
                    this.showEventDetails(this.m[index]);
                }

                break;
            }
        }
    }

    private getLogViewHeight(): number {
        return this.records.nativeElement.clientHeight - 1;
    }

    private getPageHeight(): number {
        return Math.floor(this.logPane.nativeElement.clientHeight / LogViewComponent.lineHeight) * LogViewComponent.lineHeight;
    }

    ngAfterViewChecked() {
        if (this.stateVersion === 0) {
            this.stateVersion = 1;

            let params = this.route.snapshot.queryParams;

            if (params.state) {
                this.commService.send(
                    new Command('initPermalink', {
                        recordCount: this.visibleRecordCount() * 2,
                        linkHash: SlUtils.lastParam(params.state),
                    })
                );
            } else {
                this.commService.send(
                    new Command('init', {
                        paths: LogViewComponent.getLogPaths(params),
                        savedFiltersName: this.selectedFilterStateName,
                        filterStateHash: SlUtils.lastParam(params.filters),
                    })
                );
            }
        }

        if (this.state !== State.STATE_OPENED) { return; }

        this.logView.nativeElement.style.marginTop = -this.shiftView - this.loadingProgressTop.nativeElement.clientHeight + 'px';
    }

    ngOnInit() {
        let params = this.route.snapshot;

        this.commService.startup(this);

        this.selectedFilterStateName = params.queryParams.filterSetName || 'default';
    }

    ngOnDestroy(): void {
        if (this.filterStateSbscr) {
            this.filterStateSbscr.unsubscribe();
        }
    }

    pageDown() {
        let offset = this.getPageHeight() - LogViewComponent.lineHeight;

        this.doDown(offset);
    }

    down() {
        this.doDown(LogViewComponent.lineHeight);
    }

    doDown0(offset: number) {
        let logViewHeight = this.getLogViewHeight();
        if (logViewHeight - this.shiftView < this.logPane.nativeElement.clientHeight - LogViewComponent.lineHeight + 1) {
            return;
        }

        this.shiftView += offset;

        if (!this.hasRecordAfter
            && logViewHeight - this.shiftView < this.logPane.nativeElement.clientHeight - LogViewComponent.lineHeight * 2 + 1) {
            let newVisibleHeight = this.getPageHeight() - LogViewComponent.lineHeight;

            this.shiftView = logViewHeight - newVisibleHeight;
        }

        this.tryGrow();
    }

    doDown(offset: number) {
        this.doDown0(offset);
        this.removeHead();
    }

    scrollBegin() {
        if (!this.hasRecordBefore) {
            this.shiftView = 0;
        } else {
            this.cleanAndScrollToEdge(this.recordCountToLoad(), true);
        }
    }

    private scrollEnd() {
        if (!this.hasRecordAfter) {
            this.shiftView = this.getLogViewHeight() + LogViewComponent.lineHeight - this.getPageHeight();
            this.onViewMovedTop();
        } else {
            this.cleanAndScrollToEdge();
        }
    }

    up() {
        this.doUp(LogViewComponent.lineHeight);
    }

    pageUp() {
        let offset = this.getPageHeight() - LogViewComponent.lineHeight;

        this.doUp(offset);
    }

    doUp(offset: number) {
        if (this.shiftView < 0) { return; }

        this.shiftView -= offset;

        if (this.shiftView < 0) {
            if (!this.hasRecordBefore) {
                this.shiftView = 0;
                return;
            }
        }

        if (this.shiftView < this.logPane.nativeElement.clientHeight) {
            if (this.m.length > 0) {
                if (this.hasRecordBefore) {
                    let recordCount =
                        Math.ceil((this.logPane.nativeElement.clientHeight - this.shiftView) / LogViewComponent.lineHeight) + 1;

                    this.commService.send(
                        new Command('loadNext', {
                            start: Position.recordStart(this.m[0]),
                            backward: true,
                            recordCount,
                            hashes: this.vs.hashes,
                            stateVersion: this.stateVersion,
                        })
                    );

                    this.removeTail();
                }
            }
        }
    }

    copyPermalink() {
        if (this.state !== State.STATE_OPENED || this.m.length === 0) { return false; }

        let children: HTMLCollection = this.records.nativeElement.children;

        SlUtils.assert(children.length > 0);

        let firstRecordIdx = 0;
        let shiftView = this.shiftView;

        while (firstRecordIdx < children.length && shiftView >= children[firstRecordIdx].clientHeight + 1) {
            shiftView -= children[firstRecordIdx].clientHeight + 1;
            firstRecordIdx++;
        }

        let param = this.createParams();

        let linkData = JSON.stringify({
            paths: param.log,
            searchPattern: this.searchPattern,
            hideUnmatched: this.searchHideUnmatched,
            savedFiltersName: param.filterSetName,
            filterState: this.filterPanelStateService.stateStr,
            offset: Position.recordStart(this.m[firstRecordIdx]),
            hashes: this.vs.hashes,
            selectedLine: this.vs.selectedLine,
            filterPanelFilters: this.filterPanelStateService.getActiveFilters(),
            shiftView: shiftView
        });
        let linkHash = Md5.hashStr(linkData)
            .toString()
            .substr(0, 10);

        this.http
            .post('rest/log-view/generatePermalink', [linkHash, linkData])
            .subscribe(
                res => {
                    this.toastr.success('Permalink has been copied to the clipboard', null, {closeButton: true});
                },
                error => {
                    this.toastr.error('Failed to generate permalink', null, {closeButton: true});
                }
            );

        let l = window.location;
        let link = l.protocol + '//' + l.host + l.pathname + '?' + SlUtils.buildQueryString({state: linkHash, path: param.log});

        console.info('Permalink to current view has been copied: ' + link);

        let input = $('<input id="permalinkInput" type="text" style="position: absolute; top: 0; left: 0; z-index: 1045">')
            .val(link)
            .appendTo($('body'))[0];

        (<HTMLInputElement>input).select();
        document.execCommand('Copy');
        input.remove();

        return false;
    }

    appliedFilterOutdated() {
        return !SearchUtils.equals(this.searchPattern, this.searchInputState);
    }

    filterInputKeyUp(event: KeyboardEvent) {
        if (this.modalWindow) {
            event.preventDefault();
            return;
        }

        this.checkQuickSearch();
    }

    filterInputKeyDown(event: KeyboardEvent) {
        if (this.modalWindow) {
            event.preventDefault();
            return;
        }

        switch (event.which) {
            case 27: // Escape
                let input: HTMLInputElement = <HTMLInputElement>event.target;

                if (input.selectionStart !== input.selectionEnd) {
                    input.setSelectionRange(input.selectionEnd, input.selectionEnd);
                } else {
                    input.blur();
                }
                break;

            case 13:
            case 114: // (Shift) F3
                if (!event.ctrlKey) {
                    if (this.searchHideUnmatched) {
                        this.applySearchFilter();
                    } else {
                        this.findNext(event.shiftKey ? -1 : 1);
                    }
                    break;
                }

                return;

            case 70: // F
                if (event.ctrlKey) {
                    break;
                }
                return;

            default:
                return;
        }

        event.preventDefault();
    }

    @HostListener('document:keydown', ['$event'])
    logKeyDown(event: KeyboardEvent) {
        if (this.state !== State.STATE_OPENED) {
            return;
        }
        if (this.modalWindow) {
            if (event.which === 114 && !event.ctrlKey) {
                // F3
                event.preventDefault();
            }
            return;
        }

        if ((<Element>event.target).tagName === 'INPUT') { return; }

        switch (event.which) {
            case 38:
                this.up();
                break;

            case 40:
                this.down();
                break;

            case 34:
                this.pageDown();
                break;

            case 33:
                this.pageUp();
                break;

            case 35: // end
                this.scrollEnd();

                break;

            case 36: // home
                this.scrollBegin();

                break;

            case 70: // F
                if (event.ctrlKey) {
                    this.resetSearch();
                    break;
                }

                return;

            case 81:
                // if (e.ctrlKey) {
                //     toggleSettingDialog()
                //     break
                // }

                return;

            case 114: // (Shift) F3
                if (!event.ctrlKey) {
                    this.findNext(event.shiftKey ? -1 : 1);
                    break;
                }

                return;

            default:
                return;
        }

        event.preventDefault();
    }

    private showNotFoundMessage(d: number) {
        if (d > 0) {
            this.toastr.info('Not found. Try find to another direction (press Shift+F3 to search back)');
        } else {
            this.toastr.info('Not found. Try find to another direction (press F3 to search forward)');
        }
    }

    findNext(d: number) {
        if (this.modalWindow || this.state !== State.STATE_OPENED) { return; }

        if (!this.searchPattern) { return; }

        let idx = this.getMainRecord(d < 0);

        if (idx == null) { return; }

        for (let i = idx + d; i >= 0 && i < this.m.length; i += d) {
            if (this.m[i].searchRes && this.m[i].searchRes.length > 0) {
                this.setSelectedLine(i);
                this.scrollToLine(i);
                this.recRenderer.expandSelected(
                    this.m[i],
                    this.records.nativeElement.children[i]
                );
                return;
            }
        }

        let recordCount = this.recordCountToLoad();

        let start: Position;

        if (d > 0) {
            if (!this.hasRecordAfter) {
                this.showNotFoundMessage(d);
                return;
            }

            start = Position.recordEnd(this.m[this.m.length - 1]);
        } else {
            if (!this.hasRecordBefore) {
                this.showNotFoundMessage(d);
                return;
            }

            start = Position.recordStart(this.m[0]);
        }

        let req = {
            id: this.searchRequestIdCounter++,
            mainRecord: idx,
            searchPattern: this.searchPattern,
            start: start,
            d: d
        };

        this.searchRequest = req;

        this.modalWindow = 'transparent';

        this.commService.send(
            new Command('searchNext', {
                start,
                backward: d < 0,
                recordCount,
                pattern: this.searchPattern,
                hashes: this.vs.hashes,
                stateVersion: this.stateVersion,
                requestId: req.id,
            })
        );

        setTimeout(() => {
            if (this.searchRequest === req) {
                this.modalWindow = 'findProgress';
                this.changeDetectorRef.detectChanges();
            }
        }, 400);
    }

    cancelSearch() {
        if (this.searchRequest) {
            this.commService.send(new Command('cancelSearch'));
            this.searchRequest = null;
            this.modalWindow = null;
        }
    }

    scrollToLine(offset: number) {
        if (offset < 0 || offset >= this.m.length) { throw 'Error'; }

        let newShiftView = this.headHeight(offset) - Math.round(this.getPageHeight() / 3);

        if (newShiftView > this.shiftView) {
            this.doDown0(newShiftView - this.shiftView);
        } else {
            this.doUp(this.shiftView - newShiftView);
        }
    }

    wheelRoll(event: WheelEvent) {
        if (this.state !== State.STATE_OPENED) { return; }

        let value;
        if (event.deltaMode === WheelEvent.DOM_DELTA_PIXEL) {
            value = Math.round(event.deltaY / LogViewComponent.lineHeight) * LogViewComponent.lineHeight;
        } else if (event.deltaMode === WheelEvent.DOM_DELTA_LINE) {
            value = event.deltaY * LogViewComponent.lineHeight;
        } else {
            console.error('Unsupported WheelEvent.deltaMode: ' + event.deltaMode);
            return;
        }

        if (event.deltaY > 0) {
            this.doDown(value);
        } else {
            this.doUp(-value);
        }
    }

    addToFavorites() {
        this.inFavorites = !this.inFavorites;

        if (this.logs.length === 1) {
            this.fwService.setFavorites(this.logs[0].path, this.inFavorites); // todo!
        }
    }

    caseSensitiveClick() {
        this.searchMatchCase = !this.searchMatchCase;
        this.searchFlagsChanged();
    }

    regexModeClick() {
        this.searchRegex = !this.searchRegex;
        this.searchFlagsChanged();
    }

    private searchFlagsChanged() {
        this.checkQuickSearch();
        setTimeout(() => $('#filterInput').focus(), 1);
    }

    private static getLogPaths(params: Params): string[] {
        let logPath: string[] = [];

        let p = params.path;
        if (typeof p === 'string') {
            logPath.push(p);
        } else if (p && Array.isArray(p)) {
            logPath.push(...p);
        }

        p = params.log;
        if (typeof p === 'string') {
            logPath.push(p);
        } else if (p && Array.isArray(p)) {
            logPath.push(...p);
        }

        return logPath;
    }

    private onViewMovedTop() {
        if (this.shiftView < 0) {
            if (!this.hasRecordBefore) {
                this.shiftView = 0;
            } else {
                if (this.m.length > 0) {
                    this.commService.send(
                        new Command('loadNext', {
                            start: Position.recordStart(this.m[0]),
                            backward: true,
                            recordCount: this.recordCountToLoad(),
                            hashes: this.vs.hashes,
                            stateVersion: this.stateVersion,
                        })
                    );
                }
            }
        }
    }

    private handleStatuses(event: StatusHolderEvent): boolean {
        if (event.stateVersion !== this.stateVersion) { return false; }

        if (!this.vs.handleStatuses(event)) {
            this.cleanAndScrollToEdge();
            return false;
        }

        return true;
    }

    private createParams(): Params {
        let params = this.route.snapshot.queryParams;

        let res: Params = {log: LogViewComponent.getLogPaths(params)};

        if (this.selectedFilterStateName !== 'default') {
            res.filterSetName = this.selectedFilterStateName;
        }

        let originalFilters = this.savedFilterStates[this.selectedFilterStateName];

        if (!this.filterPanelStateService.isStateEquals(originalFilters)) {
            res.filters = this.filterPanelStateService.stateHash;
        }

        return res;
    }

    private loadEffectiveFilters(): Predicate[] {
        let res: Predicate[] = this.filterPanelStateService.getActiveFilters();

        if (this.searchHideUnmatched && this.searchPattern != null) {
            let sf: SubstringPredicate = {type: 'SubstringPredicate', search: this.searchPattern};
            res = [<NotPredicate>{type: 'NotPredicate', delegate: sf}, ...res];
        }

        return res;
    }

    activeFilterChanged() {
        this.effectiveFilters = this.loadEffectiveFilters();

        let mainRecordIdx = this.getMainRecord();

        if (mainRecordIdx == null) {
            this.cleanAndScrollToEdge();
            return;
        }

        this.state = State.STATE_WAIT_FOR_NEW_FILTERS;
        this.stateVersion++;

        let backwardOnly =
            !this.hasRecordAfter &&
            !Record.containPosition(this.vs.selectedLine, this.m[mainRecordIdx]) &&
            this.getLogViewHeight() - this.shiftView <
            this.logPane.nativeElement.clientHeight;

        if (backwardOnly) {
            this.commService.send(
                new Command('loadingDataAfterFilterChangedSingle', {
                    recordCount: this.recordCountToLoad(),
                    stateVersion: this.stateVersion,
                    filter: this.effectiveFilters,
                })
            );
        } else {
            let topRecordCount = Math.ceil((this.headHeight(mainRecordIdx) - this.shiftView) / LogViewComponent.lineHeight) + 1;

            this.commService.send(
                new Command('loadingDataAfterFilterChanged', {
                    topRecordCount,
                    bottomRecordCount: this.recordCountToLoad() - topRecordCount + 1,
                    stateVersion: this.stateVersion,
                    hashes: this.vs.hashes,
                    filter: this.effectiveFilters,
                    start: Position.recordStart(this.m[mainRecordIdx]),
                })
            );

            this.anchor = mainRecordIdx;
        }

        setTimeout(() => {
            if (this.state === State.STATE_WAIT_FOR_NEW_FILTERS) {
                this.modalWindow = 'filterChanging';
                this.changeDetectorRef.detectChanges();
            }
        }, 600);
    }

    private getMainRecord(revert?: boolean): number {
        let offset = 0;

        let firstVisible = null;
        let firstPiraticallyVisible = null;
        let lastVisible = null;
        let lastPiraticallyVisible = null;

        let paneHeight = this.logPane.nativeElement.clientHeight;

        let children: HTMLCollection = this.records.nativeElement.children;

        for (let i = 0; i < this.m.length; i++) {
            let e = children[i];

            let eHeight = e.clientHeight + 1; // +1 - border at the top of row

            if (offset + eHeight - this.shiftView <= 0) {
                offset += eHeight;
                continue;
            }

            if (offset - this.shiftView >= paneHeight) { break; }

            if (offset - this.shiftView >= 0 && offset + eHeight - this.shiftView <= paneHeight) {
                if (firstVisible === null) { firstVisible = i; }

                lastVisible = i;
            } else {
                if (firstPiraticallyVisible === null) { firstPiraticallyVisible = i; }

                lastPiraticallyVisible = i;
            }

            if (Record.containPosition(this.vs.selectedLine, this.m[i])) { return i; }

            offset += eHeight;
        }

        if (revert) {
            if (lastVisible !== null) { return lastVisible; }

            return lastPiraticallyVisible;
        }

        if (firstVisible !== null) { return firstVisible; }

        return firstPiraticallyVisible;
    }

    private visibleRecordCount(): number {
        return Math.ceil(this.logPane.nativeElement.clientHeight / LogViewComponent.lineHeight);
    }

    private recordCountToLoad() {
        return this.visibleRecordCount() + 1;
    }

    private tryGrow() {
        if (this.getLogViewHeight() - this.shiftView < this.logPane.nativeElement.clientHeight * 2) {
            if (this.hasRecordAfter) {
                this.requestNextRecords();
            }
        }
    }

    private requestNextRecords() {
        let offset: Position;
        if (this.m.length === 0) {
            if (this.logs.length === 1) {
                offset = {logId: this.logs[0].id, time: 0, o: 0};
            } else {
                offset = {logId: '', time: 0, o: 0};
            }
        } else {
            offset = Position.recordEnd(this.m[this.m.length - 1]);
        }

        let recordsToLoad = Math.ceil(
            (this.logPane.nativeElement.clientHeight * 2 -
                this.getLogViewHeight() +
                this.shiftView) /
            LogViewComponent.lineHeight
        );

        if (recordsToLoad > 0) {
            this.commService.send(
                new Command('loadNext', {
                    start: offset,
                    backward: false,
                    recordCount: recordsToLoad,
                    hashes: this.vs.hashes,
                    stateVersion: this.stateVersion,
                })
            );
        }
    }

    private removeHead() {
        let toBeDelete = this.shiftView - this.logPane.nativeElement.clientHeight;
        if (toBeDelete > 0) {
            let children: HTMLCollection = this.records.nativeElement.children;

            let deletedRecords = 0;
            let deletedPixels = 0;

            for (let i = 0; i < children.length; i++) {
                let e = children[i];

                let elementHeight = e.clientHeight + 1; // +1 - border at the top of row
                if (deletedPixels + elementHeight > toBeDelete) { break; }
                deletedPixels += elementHeight;
                deletedRecords++;
            }
            if (deletedRecords > 0) {
                this.shiftView -= deletedPixels;
                this.deleteRecords(0, deletedRecords);
                this.hasRecordBefore = true;
            }
        }
    }

    private removeTail() {
        let toBeDelete =
            this.getLogViewHeight() -
            this.shiftView -
            this.logPane.nativeElement.clientHeight * 2 -
            LogViewComponent.lineHeight;

        if (toBeDelete > 0) {
            let children: HTMLCollection = this.records.nativeElement.children;

            let deletedRecords = 0;
            let deletedPixels = 0;

            for (let i = children.length - 1; i >= 0; i--) {
                let e = children[i];
                let elementHeight = e.clientHeight + 1; // +1 - border at the top of row
                if (deletedPixels + elementHeight > toBeDelete) { break; }
                deletedPixels += elementHeight;
                deletedRecords++;
            }
            if (deletedRecords > 0) {
                this.deleteRecords(this.m.length - deletedRecords, deletedRecords);
                this.hasRecordAfter = true;
            }
        }
    }

    private cleanAndScrollToEdge(recordCountToLoad: number = this.recordCountToLoad(), isScrollToBegin: boolean = false) {
        this.shiftView = 0;
        this.hasRecordBefore = !isScrollToBegin;
        this.hasRecordAfter = isScrollToBegin;
        this.clearRecords();

        this.state = State.STATE_INIT;
        this.stateVersion++;

        if (this.searchRequest != null) {
            this.searchRequest = null;
            this.modalWindow = null;
        }

        this.commService.send(
            new Command('scrollToEdge', {
                recordCount: recordCountToLoad,
                stateVersion: this.stateVersion,
                filter: this.effectiveFilters,
                isScrollToBegin,
            })
        );
    }

    private resetSearch() {
        let filterInput = $('#filterInput');

        if (!filterInput.is(':focus')) {
            let selText = window.getSelection().toString();
            if (
                selText.length > 0 &&
                selText.length < 200 &&
                selText.indexOf('\n') === -1
            ) {
                filterInput.val(selText);
            }
            filterInput.select();
        }

        filterInput.focus();

        this.checkQuickSearch();
    }

    private checkQuickSearch() {
        let s = <string>$('#filterInput').val();

        this.searchRegexError = null;
        if (s.length > 0 && this.searchRegex) {
            try {
                // tslint:disable-next-line:no-unused-expression
                new RegExp(s);
            } catch (e) {
                this.searchRegexError = e.message;
            }
        }

        let pattern: SearchPattern = null;
        if (s.length > 0 && this.searchRegexError == null) {
            pattern = {s, matchCase: this.searchMatchCase, regex: this.searchRegex};
        }

        if (!SearchUtils.equals(this.searchInputState, pattern)) {
            this.searchInputState = pattern;
        }

        if (!this.searchHideUnmatched) {
            if (!SearchUtils.equals(this.searchPattern, pattern)) {
                this.searchPattern = pattern;
                this.rehighlightSearchOccurrences();
            }
        }
    }

    private rehighlightSearchOccurrences() {
        let changed: number[] = [];
        SearchUtils.doSimpleSearch(this.m, this.searchPattern, changed);

        for (let idx of changed) {
            this.recRenderer.updateSearchResults(
                this.m[idx],
                this.records.nativeElement.children[idx]
            );
        }
    }

    private headHeight(recCount: number): number {
        if (recCount === 0) { return 0; }

        let res = 0;

        for (let e of this.records.nativeElement.children) {
            res += e.clientHeight + 1; // +1 - border at the top of row
            if (--recCount === 0) { break; }
        }

        return res;
    }

    private clearRecords() {
        this.m.length = 0;
        SlUtils.clearElement(this.records.nativeElement);
    }

    private deleteRecords(idx: number, count: number) {
        SlUtils.assert(idx >= 0 && idx + count <= this.m.length);
        this.m.splice(idx, count);

        let parentDiv = <HTMLDivElement>this.records.nativeElement;
        for (let i = 0; i < count; i++) {
            parentDiv.removeChild(parentDiv.childNodes[idx]);
        }

        SlUtils.assert(
            this.m.length === this.records.nativeElement.childElementCount
        );
    }

    private addRecords(m: Record[], idx?: number) {
        if (idx == null) {
            idx = this.m.length;
        } else {
            SlUtils.assert(idx >= 0 && idx <= this.m.length, 'Invalid index: ' + idx);
        }

        this.m.splice(idx, 0, ...m);
        this.recRenderer.renderRange(
            this.m,
            idx,
            idx + m.length,
            this.records.nativeElement
        );

        SlUtils.assert(
            this.m.length === this.records.nativeElement.childElementCount
        );
    }

    @BackendEventHandler()
    private onBackendError(event: BackendErrorEvent) {
        this.fail(event.stacktrace);
    }

    disconnected(disconnectMessage?: string) {
        if (this.state !== State.STATE_NO_LOGS) {
            this.modalWindow = 'disconnected';
            this.state = State.STATE_DISCONNECTED;
            this.disconnectMessage = disconnectMessage || 'Disconnected';
        }
    }

    @BackendEventHandler()
    private onScrollToEdgeResponse(event: EventScrollToEdgeResponse) {
        if (!this.handleStatuses(event)) {
            return;
        }

        let m = event.data.records;

        this.state = State.STATE_OPENED;

        if (this.searchPattern) {
            SearchUtils.doSimpleSearch(m, this.searchPattern);
        }

        this.clearRecords();
        this.addRecords(m);

        this.shiftView = 0;

        if (event.isScrollToBegin) {
            this.hasRecordBefore = false;
            this.hasRecordAfter = event.data.hasNextLine;

            this.tryGrow();
        } else {
            this.hasRecordAfter = false;
            this.hasRecordBefore = event.data.hasNextLine;

            this.scrollEnd();
        }
    }

    @BackendEventHandler()
    private onNextDataLoaded(event: EventNextDataLoaded) {
        if (!this.handleStatuses(event)) {
            return;
        }

        let m = event.data.records;

        if (this.searchPattern) {
            SearchUtils.doSimpleSearch(m, this.searchPattern);
        }

        if (event.backward) {
            if (this.m.length > 0 && !Record.containPosition(event.start, this.m[0])) {
                return;
            }

            this.hasRecordBefore = event.data.hasNextLine;

            if (m.length > 0) {
                this.addRecords(m, 0);

                this.shiftView += this.headHeight(m.length);
            }

            this.onViewMovedTop();
        } else {
            if (this.m.length > 0 &&
                !Record.containPosition(event.start, this.m[this.m.length - 1])) {
                return;
            }

            this.hasRecordAfter = event.data.hasNextLine;

            if (m.length > 0) {
                if (this.m.length > 0 &&
                    Record.compareTo(this.m[this.m.length - 1], m[0]) === 0) {
                    this.m.pop();
                    let parentDiv = <HTMLDivElement>this.records.nativeElement;
                    parentDiv.removeChild(parentDiv.childNodes[this.m.length]);
                }

                this.addRecords(m);
            }

            this.tryGrow();
        }
    }

    @BackendEventHandler()
    private onSearchResponse(event: EventSearchResponse) {
        if (this.searchRequest == null || this.searchRequest.id !== event.requestId) {
            return;
        }

        this.modalWindow = null;

        let req = this.searchRequest;
        this.searchRequest = null;

        if (!this.handleStatuses(event)) {
            return;
        }

        if (this.getMainRecord(req.d < 0) !== req.mainRecord
            || !SearchUtils.equals(req.searchPattern, this.searchPattern)) {
            return;
        }

        let data = event.records;

        if (!data) {
            this.showNotFoundMessage(req.d);
            return;
        }

        SearchUtils.doSimpleSearch(data, this.searchPattern);

        let nextOccurrenceIds = req.d > 0 ? data.length - 1 : 0;

        SlUtils.assert(data[nextOccurrenceIds].searchRes.length > 0);

        if (!event.hasSkippedLine) {
            if (req.d < 0) {
                if (!Record.containPosition(req.start, this.m[0])) {
                    return;
                }

                this.addRecords(data, 0);

                this.hasRecordBefore = true;
            } else {
                if (!Record.containPosition(req.start, this.m[this.m.length - 1])) {
                    return;
                }

                nextOccurrenceIds += this.m.length;
                this.addRecords(data);

                this.hasRecordAfter = true;
            }
        } else {
            this.hasRecordAfter = true;
            this.hasRecordBefore = true;

            this.clearRecords();
            this.addRecords(data);
        }

        this.shiftView = 0;

        this.setSelectedLine(nextOccurrenceIds);

        this.scrollToLine(nextOccurrenceIds);
    }

    @BackendEventHandler()
    private onInitByPermalink(event: EventInitByPermalink) {
        if (!this.handleStatuses(event)) {
            return;
        }

        this.searchPattern = event.searchPattern;
        this.searchInputState = event.searchPattern;
        this.searchHideUnmatched = event.hideUnmatched;

        if (event.searchPattern) {
            $('#filterInput').val(event.searchPattern.s);
            this.searchMatchCase = event.searchPattern.matchCase;
            this.searchRegex = event.searchPattern.regex;
        }

        let m = event.data.records;

        if (this.searchPattern) {
            SearchUtils.doSimpleSearch(m, this.searchPattern);
        }

        this.vs.selectedLine = event.selectedLine;

        SlUtils.assert(this.m.length === 0);
        this.addRecords(m);

        this.hasRecordAfter = event.data.hasNextLine;

        this.shiftView = event.shiftView;

        let p = this.createParams();
        this.router.navigate([], {queryParams: p});

        this.state = State.STATE_OPENED;

        this.tryGrow();
    }

    @BackendEventHandler()
    private onBrokenLink() {
        this.modalWindow = 'brokenLink';
    }

    @BackendEventHandler()
    private onResponseAfterFilterChangedSingle(event: EventResponseAfterFilterChangedSingle) {
        if (!this.handleStatuses(event) || this.state !== State.STATE_WAIT_FOR_NEW_FILTERS) {
            return;
        }

        let m = event.data.records;

        if (this.searchPattern) {
            SearchUtils.doSimpleSearch(m, this.searchPattern);
        }

        this.state = State.STATE_OPENED;
        this.modalWindow = null;

        this.hasRecordBefore = event.data.hasNextLine;
        this.hasRecordAfter = false;

        this.recRenderer.replaceRange(m, this.m, 0, this.m.length, this.records.nativeElement);

        this.shiftView = Math.max(0, this.getLogViewHeight() + LogViewComponent.lineHeight - this.getPageHeight());

        this.requestNextRecords();
    }

    @BackendEventHandler()
    private onResponseAfterFilterChanged(event: EventResponseAfterFilterChanged) {
        if (!this.handleStatuses(event) || this.state !== State.STATE_WAIT_FOR_NEW_FILTERS) {
            return;
        }

        let top = event.topData.records;
        let bottom = event.bottomData.records;

        if (this.searchPattern) {
            SearchUtils.doSimpleSearch(top, this.searchPattern);
            SearchUtils.doSimpleSearch(bottom, this.searchPattern);
        }

        this.state = State.STATE_OPENED;

        this.modalWindow = null;

        this.hasRecordBefore = event.topData.hasNextLine;
        this.hasRecordAfter = event.bottomData.hasNextLine;

        let anchorOffset = this.headHeight(this.anchor) - this.shiftView;

        this.recRenderer.replaceRange(top, this.m, 0, this.anchor, this.records.nativeElement);
        this.recRenderer.replaceRange(bottom, this.m, top.length, this.m.length, this.records.nativeElement);

        this.shiftView = this.headHeight(top.length) - anchorOffset;

        this.onViewMovedTop();
        this.requestNextRecords();
    }

    @BackendEventHandler()
    private onLogChanged(event: EventsLogChanged) {
        let hasNewChanges = this.vs.logChanged(event);

        if (this.state === State.STATE_OPENED && !this.hasRecordAfter && hasNewChanges) {
            this.requestNextRecords();
        }
    }

    @BackendEventHandler()
    private onSetViewState(event: EventSetViewState) {
        SlUtils.assert(this.state === State.STATE_INIT);

        this.logs = event.logs;

        let uiConfig: UiConfig = JSON.parse(event.uiConfig);

        let error = UiConfigValidator.validateUiConfig(uiConfig);
        if (error != null) {
            this.fail(error);
            return;
        }

        this.viewConfig.setRendererCfg(event.logs, uiConfig);
        this.inFavorites = event.inFavorites;
        this.fwService.editable = event.favEditable;

        if (this.logs.length === 0) {
            this.state = State.STATE_NO_LOGS;
            this.stateVersion++;
            this.commService.close();
            return;
        }

        for (const [filterName, stateJson] of Object.entries(event.globalSavedFilters)) {
            let filterState = LogViewComponent.parseFilterState(stateJson);
            if (filterState) {
                this.savedFilterStates[filterName] = filterState;
            }
        }

        let filterState = LogViewComponent.parseFilterState(event.filterState) || {};
        this.filterPanelStateService.init(this.logs, filterState);

        this.effectiveFilters = this.loadEffectiveFilters();

        this.filterPanelStateService.filterChanges.subscribe(() => this.filtersChanged());

        if (!event.initByPermalink) {
            this.cleanAndScrollToEdge(this.visibleRecordCount() * 2);
        }
    }
}

interface SearchRequest {
    id: number;
    d: number;
    start: Position;
    mainRecord: number;
    searchPattern: SearchPattern;
}
