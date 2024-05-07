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
import {Predicate, SubstringPredicate} from './predicates';
import {LvUtils} from '../utils/utils';
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
    EventResponseAfterFilterScrollDown,
    EventResponseAfterLoadDataAroundPosition,
    EventScrollToEdgeResponse,
    EventSearchResponse,
    EventSetViewState,
    EventsLogChanged,
    LoadLogContentResponse,
    SetFilterStateEvent,
    StatusHolderEvent,
    UiConfig,
    UiConfigValidator
} from './backend-events';
import * as $ from 'jquery';
import {ToastrService} from 'ngx-toastr';
import {HttpClient} from '@angular/common/http';
import {FilterPanelStateService, FilterState} from '@app/log-view/filter-panel-state.service';
import {Subscription} from 'rxjs';
import {ContextMenuComponent, ContextMenuService} from '@perfectmemory/ngx-contextmenu';
import {ContextMenuHandler} from '@app/log-view/context-menu';
import {LogPathUtils} from '@app/utils/log-path-utills';
import {OpenEvent} from '@app/log-navigator/log-navigator.component';

@Component({
    selector: 'lv-log-view',
    templateUrl: './log-view.template.html',
    styleUrls: ['./log-view.style.scss'],
    providers: [
        CommunicationService,
        ViewConfigService,
        RecordRendererService,
        ViewStateService,
        FilterPanelStateService,
        ContextMenuHandler,
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

    @ViewChild('eventContextMenu', {static: true}) public eventContextMenu: ContextMenuComponent<any>;

    logs: LogFile[];

    backendErrorStacktrace: string;
    disconnectMessage: string;

    state: State = State.STATE_LOADING;
    private stateVersion: number = 0;
    private loadingNextBottom: Position;
    private loadingNextTop: Position;

    readonly m: Record[] = [];

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

    effectiveFilters: Predicate[] = [];

    private filterStateSbscr: Subscription;

    modalWindow: string;

    inFavorites: boolean;

    anchor: number;

    filterErrorText: string;

    recordWithDetails: Record;

    touch: Touch;

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
        private contextMenuService: ContextMenuService<any>,
        public filterPanelStateService: FilterPanelStateService,
        public contextMenuHandler: ContextMenuHandler,
    ) {
        this.filterPanelStateService.currentRecords = this.m;
    }

    @HostListener('window:resize', ['$event'])
    onResize() {
        if (this.state !== State.STATE_OPENED) { return; }

        this.loadRecordsIfNeeded();
    }

    goToTailAfterBrokenLink() {
        let p = this.createParams();
        this.router.navigate([], {queryParams: p, preserveFragment: true});
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
        this.router.navigate([], {queryParams: this.createParams(), preserveFragment: true});
        this.activeFilterChanged();
    }

    unselectedLine() {
        $('.record.selected-line', this.records.nativeElement).removeClass('selected-line');

        this.vs.selectedLine = null;
    }

    setSelectedLine(idx: number) {
        if (this.vs.selectedLine != null) {
            if (Position.containPosition(this.vs.selectedLine, this.m[idx])) { return; }

            $('.record.selected-line', this.records.nativeElement).removeClass('selected-line');
        }

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

                this.openContextMenu(index, event);
                break;
            }
        }

        return false;
    }

    private openContextMenu(index: number, event: MouseEvent) {
        this.contextMenuService.show(this.eventContextMenu, {
            value: this.contextMenuHandler.createItem(this.m[index], this.logs),
            y: event.pageY,
            x: event.pageX,
        });

        event.preventDefault();
        event.stopPropagation();
    }

    showEventDetails(record: Record) {
        this.recordWithDetails = record;
        this.modalWindow = 'event-details';
    }

    clickRecord(event: MouseEvent) {
        if (this.state !== State.STATE_OPENED || this.modalWindow != null) { return; }

        if (this.recRenderer.handleClick(event)) {
            this.loadRecordsIfNeededBottom()
            return;
        }

        if (window.getSelection().toString().length > 0) { return; }

        let target = <Element>event.target;

        if (target.classList.contains('has-more-load-more')) {
            let record = $(target).parents('div.record')[0]
            LvUtils.assert(record != null)
            let rec = this.m[LogViewComponent.getLineIndex(record)]
            if (rec.loadedTextLengthBytes >= rec.end - rec.start)
                return;

            target.replaceWith($('<img src="assets/progress.gif">')[0])

            this.commService.send(new Command('loadLogContent', {
                logId: rec.logId, recordStart: rec.start,
                offset: rec.start + rec.loadedTextLengthBytes, end: rec.end
            }))

            return;
        }

        for (let e = target; e; e = e.parentElement) {
            if (e.classList.contains('filtering-error')) {
                this.modalWindow = 'filter-error';
                this.filterErrorText = (<any>e).errorText;
                return;
            }

            if (e.parentElement && e.parentElement.id === 'records') {
                let index = LogViewComponent.getLineIndex(e);

                if (e.classList.contains('selected-line') && event.ctrlKey) {
                    this.unselectedLine()
                    return;
                }

                this.setSelectedLine(index);

                if ((<Element>event.target).classList.contains('rec-pointer')) {
                    this.openContextMenu(index, event);
                }

                break;
            }
        }
    }

    /**
     * It's good to have some loaded records outside the view to avoid blinking when the user scrolls up/down.
     */
    private spareDataOutsizeViewBorder() {
        return this.logPane.nativeElement.clientHeight;
    }

    private maxSpareDataOutsideViewBorder() {
        return this.logPane.nativeElement.clientHeight * 2;
    }

    private loadRecordsIfNeededTop() {
        if (!this.hasRecordBefore) {
            if (this.shiftView < 0) {
                this.shiftView = 0;
                return;
            }
        }

        if (this.shiftView > this.maxSpareDataOutsideViewBorder()) {
            let pixelsToDelete = this.shiftView - this.maxSpareDataOutsideViewBorder();

            let children: HTMLCollection = this.records.nativeElement.children;

            let deletedPixels = 0;

            let i = 0;
            for (; i < children.length; i++) {
                let elementHeight = children[i].clientHeight + 1; // +1 - border at the top of row
                if (deletedPixels + elementHeight > pixelsToDelete) { break; }
                deletedPixels += elementHeight;
            }
            if (i > 0) {
                this.shiftView -= deletedPixels;
                this.deleteRecords(0, i);
                this.hasRecordBefore = true;
            }
            return;
        }

        if (this.hasRecordBefore && this.shiftView < this.spareDataOutsizeViewBorder()) {
            let neededRecords = Math.ceil((this.spareDataOutsizeViewBorder() - this.shiftView) / LogViewComponent.lineHeight);
            neededRecords = Math.max(neededRecords, Math.ceil(this.recordCountToLoad() / 3));

            if (this.m.length > 0) {
                let start = Position.recordStart(this.m[0]);
                if (!Position.equals(start, this.loadingNextTop)) {
                    this.commService.send(
                        new Command('loadNext', {
                            start,
                            backward: true,
                            recordCount: neededRecords,
                            hashes: this.vs.hashes,
                            stateVersion: this.stateVersion,
                        })
                    );
                    this.loadingNextTop = start;
                }
            }
        }
    }

    private loadRecordsIfNeededBottom() {
        let spareDateBottom = this.getLogViewHeight() - this.shiftView - this.logPane.nativeElement.clientHeight;

        if (spareDateBottom > this.maxSpareDataOutsideViewBorder()) {
            let toDeletePixels = spareDateBottom - this.maxSpareDataOutsideViewBorder();

            let children: HTMLCollection = this.records.nativeElement.children;

            let i = 0;
            while (i < children.length) {
                let elementHeight = children[children.length - i - 1].clientHeight + 1; // +1 - border at the top of row

                if (elementHeight > toDeletePixels) {
                    break;
                }

                toDeletePixels -= elementHeight;
                i++;
            }

            if (i > 0) {
                this.deleteRecords(children.length - i, i);
                this.hasRecordAfter = true;
            }

            return;
        }

        if (this.hasRecordAfter && spareDateBottom < this.spareDataOutsizeViewBorder()) {
            this.requestNextRecords();
        }
    }

    private loadRecordsIfNeeded() {
        this.loadRecordsIfNeededTop();
        this.loadRecordsIfNeededBottom();
    }

    private getLogViewHeight(): number {
        return this.records.nativeElement.clientHeight - 1;
    }

    private getPageHeight(): number {
        return Math.floor(this.logPane.nativeElement.clientHeight / LogViewComponent.lineHeight) * LogViewComponent.lineHeight;
    }

    private incrementStateVersion() {
        this.stateVersion++;
        this.loadingNextTop = null;
        this.loadingNextBottom = null;
    }

    ngAfterViewChecked() {
        if (this.stateVersion === 0) {
            this.incrementStateVersion();

            let params = this.route.snapshot.queryParams;

            if (params.state) {
                this.commService.send(
                    new Command('initPermalink', {
                        recordCount: this.visibleRecordCount() * 2,
                        linkHash: LvUtils.lastParam(params.state),
                    })
                );
            } else {
                let filtersParam = LvUtils.lastParam(params.filters)
                if (filtersParam) {
                    if (filtersParam.match(/^[a-f0-9]+$/)) {
                        this.commService.send(new Command('loadFilterStateByHash', {hash: filtersParam}));
                    } else {
                        this.filterPanelStateService.setFilterStateFromUrlValue(filtersParam);
                    }
                }

                this.commService.send(new Command('init', {logList: LogPathUtils.extractLogList(params)}));
            }
        }

        if (this.state !== State.STATE_OPENED) { return; }

        this.logView.nativeElement.style.marginTop = -this.shiftView - this.loadingProgressTop.nativeElement.clientHeight + 'px';
    }

    ngOnInit() {
        this.commService.startup(this);
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

    doDown(offset: number) {
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

        this.loadRecordsIfNeeded();
    }

    scrollBegin() {
        if (!this.hasRecordBefore) {
            this.shiftView = 0;
        } else {
            this.cleanAndScrollToEdge(this.recordCountToLoad(), true);
        }
    }

    private moveLastRecordToBottom() {
        this.shiftView = this.getLogViewHeight() + LogViewComponent.lineHeight - this.getPageHeight();
    }

    private scrollEnd() {
        if (!this.hasRecordAfter) {
            this.moveLastRecordToBottom();
            this.loadRecordsIfNeeded();
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

        this.loadRecordsIfNeeded();
    }

    copyPermalink() {
        if (this.state !== State.STATE_OPENED || this.m.length === 0) { return false; }

        let children: HTMLCollection = this.records.nativeElement.children;

        LvUtils.assert(children.length > 0);

        let firstRecordIdx = 0;
        let shiftView = this.shiftView;

        while (firstRecordIdx < children.length && shiftView >= children[firstRecordIdx].clientHeight + 1) {
            shiftView -= children[firstRecordIdx].clientHeight + 1;
            firstRecordIdx++;
        }

        let param = this.createParams();

        let linkData = JSON.stringify({
            logListQueryParams: LogPathUtils.getListParamMap(param),
            logList: LogPathUtils.extractLogList(param),
            searchPattern: this.searchPattern,
            hideUnmatched: this.searchHideUnmatched,
            savedFiltersName: param.filterSetName,
            filterState: JSON.stringify(this.filterPanelStateService.getFilterState()),
            filterStateUrlParam: param.filters,
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
        let link = l.protocol + '//' + l.host + l.pathname + '?' + LvUtils.buildQueryString({state: linkHash});

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

        let targetTagName = (<Element>event.target).tagName;

        if (targetTagName === 'INPUT' || targetTagName === 'TEXTAREA') {
            return;
        }

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
                loadNext: true,
            })
        );

        setTimeout(() => {
            if (this.searchRequest === req && this.state === State.STATE_OPENED) {
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
            this.doDown(newShiftView - this.shiftView);
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

        if (this.logs?.length === 1) {
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

    private handleStatuses(event: StatusHolderEvent): boolean {
        if (event.stateVersion !== this.stateVersion) { return false; }

        if (!this.vs.handleStatuses(event)) {
            this.cleanAndScrollToEdge();
            return false;
        }

        return true;
    }

    private createParams(): Params {
        let res = LogPathUtils.getListParamMap(this.route.snapshot.queryParams);

        let savedFilterName = this.getSavedFilterName();
        if (savedFilterName !== 'default') {
            res.filterSetName = savedFilterName;
        }

        let originalFilters = this.savedFilterStates[savedFilterName];

        if (this.filterPanelStateService.urlParamValue && !this.filterPanelStateService.isStateEquals(originalFilters)) {
            res.filters = this.filterPanelStateService.urlParamValue;
        }

        return res;
    }

    private getSavedFilterName(): string {
        let savedFilterParam = LvUtils.lastParam(this.route.snapshot.queryParams.filterSetName)
        return savedFilterParam || 'default';
    }

    private loadEffectiveFilters(): Predicate[] {
        let res: Predicate[] = this.filterPanelStateService.getActiveFilters();

        if (this.searchHideUnmatched && this.searchPattern != null) {
            let sf: SubstringPredicate = {type: 'SubstringPredicate', search: this.searchPattern};
            res = [sf, ...res];
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
        this.incrementStateVersion();

        let backwardOnly =
            !this.hasRecordAfter &&
            !Position.containPosition(this.vs.selectedLine, this.m[mainRecordIdx]) &&
            this.getLogViewHeight() - this.shiftView < this.logPane.nativeElement.clientHeight;

        if (backwardOnly) {
            this.commService.send(
                new Command('changeFiltersAndScrollDown', {
                    recordCount: this.recordCountToLoad(),
                    stateVersion: this.stateVersion,
                    filter: this.effectiveFilters,
                })
            );
        } else {
            let topRecordCount = Math.ceil((this.headHeight(mainRecordIdx) - this.shiftView) / LogViewComponent.lineHeight) + 1;

            if (topRecordCount <= 0) {
                topRecordCount = 1;
            }

            this.commService.send(
                new Command('changeFiltersAndLoadData', {
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

    private hasVisibleSelectedLine(): boolean {
        if (!this.vs.selectedLine)
            return false;

        let mainRecord = this.getMainRecord(false)
        return mainRecord != null && Position.containPosition(this.vs.selectedLine, this.m[mainRecord]) // selected line may be out of the visible vew
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

            if (Position.containPosition(this.vs.selectedLine, this.m[i])) { return i; }

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

    private requestNextRecords(recordsToLoad?: number) {
        if (!(this.logs?.length > 0) || recordsToLoad <= 0) {
            return;
        }

        if (recordsToLoad == null) {
            let spareDataBottom = this.getLogViewHeight() - this.shiftView - this.logPane.nativeElement.clientHeight;
            recordsToLoad = Math.ceil((this.spareDataOutsizeViewBorder() - spareDataBottom) / LogViewComponent.lineHeight);
            if (recordsToLoad <= 0)
                return;

            recordsToLoad = Math.max(recordsToLoad, Math.ceil(this.recordCountToLoad() / 3));
        }

        let offset: Position;
        if (this.m.length === 0) {
            if (this.logs.length === 1) {
                offset = new Position(this.logs[0].id, null, 0);
            } else {
                offset = Position.firstLine();
            }
        } else {
            offset = Position.recordEnd(this.m[this.m.length - 1]);
        }

        if (!Position.equals(offset, this.loadingNextBottom)) {
            this.commService.send(
                new Command('loadNext', {
                    start: offset,
                    backward: false,
                    recordCount: recordsToLoad,
                    hashes: this.vs.hashes,
                    stateVersion: this.stateVersion,
                })
            );
            this.loadingNextBottom = offset;
        }
    }

    private cleanAndScrollToEdge(recordCountToLoad: number = this.recordCountToLoad(), isScrollToBegin: boolean = false) {
        this.shiftView = 0;
        this.hasRecordBefore = !isScrollToBegin;
        this.hasRecordAfter = isScrollToBegin;
        this.clearRecords();

        this.state = State.STATE_LOADING;
        this.incrementStateVersion();

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
        LvUtils.clearElement(this.records.nativeElement);
    }

    private deleteRecords(idx: number, count: number) {
        LvUtils.assert(idx >= 0 && idx + count <= this.m.length);
        this.m.splice(idx, count);

        let parentDiv = <HTMLDivElement>this.records.nativeElement;
        for (let i = 0; i < count; i++) {
            parentDiv.removeChild(parentDiv.childNodes[idx]);
        }

        LvUtils.assert(
            this.m.length === this.records.nativeElement.childElementCount
        );
    }

    private addRecords(m: Record[], idx?: number) {
        if (idx == null) {
            idx = this.m.length;
        } else {
            LvUtils.assert(idx >= 0 && idx <= this.m.length, 'Invalid index: ' + idx);
        }

        this.m.splice(idx, 0, ...m);
        this.recRenderer.renderRange(
            this.m,
            idx,
            idx + m.length,
            this.records.nativeElement
        );

        LvUtils.assert(
            this.m.length === this.records.nativeElement.childElementCount
        );
    }

    @BackendEventHandler()
    private onBackendError(event: BackendErrorEvent) {
        console.error(event.stacktrace);
        this.backendErrorStacktrace = event.stacktrace;
        this.commService.close('<h5 class="text-danger">Internal error</h5>');
    }

    disconnected(disconnectMessage?: string) {
        this.modalWindow = 'disconnected';
        this.state = State.STATE_DISCONNECTED;
        this.disconnectMessage = disconnectMessage || '<br><h3 class="text-danger">&nbsp;Disconnected&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</h3><br>';
    }

    @BackendEventHandler()
    private onLoadLogContentResponse(event: LoadLogContentResponse) {
        for (let idx = 0; idx < this.m.length; idx++) {
            let record = this.m[idx];

            if (record.logId === event.logId && record.start === event.recordStart) {
                if (record.start + record.loadedTextLengthBytes === event.offset) {
                    let originalTextLength = record.s.length

                    record.s += event.text;
                    record.loadedTextLengthBytes += event.textLengthBytes;

                    this.recRenderer.renderNewAppendedText(record, originalTextLength, idx, this.records.nativeElement);
                }
            }
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

        if (event.isScrollToBegin) {
            this.hasRecordBefore = false;
            this.hasRecordAfter = event.data.hasNextLine;
            this.shiftView = 0;
        } else {
            this.hasRecordAfter = false;
            this.hasRecordBefore = event.data.hasNextLine;
            this.moveLastRecordToBottom();
        }

        this.loadRecordsIfNeeded();
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
            if (Position.equals(event.start, this.loadingNextTop)) {
                this.loadingNextTop = null;
            }

            if (this.m.length > 0 && !Position.containPosition(event.start, this.m[0])) {
                return;
            }

            this.hasRecordBefore = event.data.hasNextLine;

            if (m.length > 0) {
                this.addRecords(m, 0);

                this.shiftView += this.headHeight(m.length);
            }
        } else {
            if (Position.equals(event.start, this.loadingNextBottom)) {
                this.loadingNextBottom = null;
            }

            if (this.m.length > 0 && !Position.containPosition(event.start, this.m[this.m.length - 1])) {
                return;
            }

            let scroll = !this.hasRecordAfter && !event.data.hasNextLine
                && !this.searchRequest && !this.hasVisibleSelectedLine() && this.hasEmptySpaceAtBottom()
                && this.viewConfig.isAutoscrollEnabled()

            this.hasRecordAfter = event.data.hasNextLine;

            if (m.length > 0) {
                if (this.m.length > 0 &&
                    LogViewComponent.isStartEquals(this.m[this.m.length - 1], m[0])) {
                    this.m.pop();
                    let parentDiv = <HTMLDivElement>this.records.nativeElement;
                    parentDiv.removeChild(parentDiv.childNodes[this.m.length]);
                }

                this.addRecords(m);

                if (scroll && !this.hasEmptySpaceAtBottom()) {
                    this.scrollEnd()
                }
            }
        }

        this.loadRecordsIfNeeded();
    }

    private static isStartEquals(a: Record, b: Record): boolean {
        return a.logId === b.logId && a.start === b.start;
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

        let nextOccurrenceIdx = event.foundIdx;
        LvUtils.assert(data[nextOccurrenceIdx].searchRes.length > 0);

        if (!event.hasSkippedLine) {
            if (req.d < 0) { // is backward search
                if (!Position.containPosition(req.start, this.m[0])) {
                    return;
                }

                this.addRecords(data, 0);
            } else {
                if (!Position.containPosition(req.start, this.m[this.m.length - 1])) {
                    return;
                }

                nextOccurrenceIdx += this.m.length;
                this.addRecords(data);
            }
        } else {
            this.hasRecordAfter = true;
            this.hasRecordBefore = true;

            this.clearRecords();
            this.addRecords(data);
        }

        if (req.d < 0) { // is backward search
            this.hasRecordBefore = event.hasNextLine;
        } else {
            this.hasRecordAfter = event.hasNextLine;
        }

        this.shiftView = 0;

        this.setSelectedLine(nextOccurrenceIdx);

        this.scrollToLine(nextOccurrenceIdx);
    }

    @BackendEventHandler()
    private onInitByPermalink(event: EventInitByPermalink) {
        if (!this.handleStatuses(event)) {
            return;
        }

        let m = event.data.records;

        this.searchPattern = event.searchPattern;
        this.searchInputState = event.searchPattern;
        this.searchHideUnmatched = event.hideUnmatched;

        if (event.searchPattern) {
            $('#filterInput').val(event.searchPattern.s);
            this.searchMatchCase = event.searchPattern.matchCase;
            this.searchRegex = event.searchPattern.regex;

            SearchUtils.doSimpleSearch(m, this.searchPattern);

            if (event.hideUnmatched) {
                this.effectiveFilters = this.loadEffectiveFilters()
            }
        }

        this.vs.setSelectedWithoutFragmentChange(event.selectedLine);

        LvUtils.assert(this.m.length === 0);
        this.addRecords(m);

        this.hasRecordAfter = event.data.hasNextLine;

        this.shiftView = event.shiftView;

        let queryParams = {...event.logListQueryParams};
        if (event.savedFilterName)
            queryParams.filterSetName = event.savedFilterName;

        if (event.filterStateUrlParam)
            queryParams.filters = event.filterStateUrlParam;

        this.router.navigate([], {queryParams, fragment: this.vs.createFragment()});

        this.state = State.STATE_OPENED;

        this.loadRecordsIfNeeded();
    }

    @BackendEventHandler()
    private onBrokenLink() {
        this.modalWindow = 'brokenLink';
    }

    @BackendEventHandler()
    private onResponseAfterFilterChangedScrollDown(event: EventResponseAfterFilterScrollDown) {
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

        this.loadRecordsIfNeeded();
    }

    @BackendEventHandler()
    private onResponseAfterLoadDataAroundPosition(event: EventResponseAfterLoadDataAroundPosition) {
        if (!this.handleStatuses(event)
            || (this.state !== State.STATE_WAIT_FOR_NEW_FILTERS && this.state !== State.STATE_LOADING)) {
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

        let selectedOffsetInUrl = this.getSelectedRowFromFragment();

        let selectedPosition = null;

        if (selectedOffsetInUrl) {
            let selectedRecordPredicate = (r: Record) => {
                return r.logId === selectedOffsetInUrl.logId && r.start <= selectedOffsetInUrl.offset && r.end >= selectedOffsetInUrl.offset
            };

            let selectedRecord = top.find(selectedRecordPredicate) || bottom.find(selectedRecordPredicate)
            if (selectedRecord) {
                selectedPosition = Position.recordStart(selectedRecord)
            }
        }

        this.vs.selectedLine = selectedPosition;

        if (this.anchor) {
            let anchorOffset = this.headHeight(this.anchor) - this.shiftView;

            this.recRenderer.replaceRange(top, this.m, 0, this.anchor, this.records.nativeElement);
            this.recRenderer.replaceRange(bottom, this.m, top.length, this.m.length, this.records.nativeElement);

            this.shiftView = this.headHeight(top.length) - anchorOffset;
        } else {
            this.clearRecords();
            this.addRecords(top)
            this.addRecords(bottom)
            this.shiftView = 0;
            if (top.length)
                this.scrollToLine(top.length - 1)
        }

        this.loadRecordsIfNeeded();
    }

    @BackendEventHandler()
    private onLogChanged(event: EventsLogChanged) {
        let hasNewChanges = this.vs.logChanged(event);

        if (this.state === State.STATE_OPENED && !this.hasRecordAfter && hasNewChanges && !this.loadingNextBottom) {
            if (this.searchRequest || this.hasVisibleSelectedLine() || !this.hasEmptySpaceAtBottom() || !this.viewConfig.isAutoscrollEnabled()) {
                this.requestNextRecords();
            } else {
                this.requestNextRecords(this.recordCountToLoad() * 5);
            }
        }
    }

    private hasEmptySpaceAtBottom() {
        return this.logView.nativeElement.clientHeight - this.shiftView < this.logPane.nativeElement.clientHeight
    }

    @BackendEventHandler()
    private onSetFilterState(event: SetFilterStateEvent) {
        this.filterPanelStateService.setFilterState(this.filterPanelStateService.parseFilterState(event.filterState), event.urlParamValue);
    }

    @BackendEventHandler()
    private onSetViewState(event: EventSetViewState) {
        LvUtils.assert(this.state === State.STATE_LOADING);

        this.logs = event.logs;

        let uiConfig: UiConfig = JSON.parse(event.uiConfig);

        let error = UiConfigValidator.validateUiConfig(uiConfig);
        if (error != null) {
            console.error(error);
            this.commService.close(error);
            return;
        }

        this.viewConfig.setRendererCfg(event.logs, uiConfig, event.localhostName);
        this.inFavorites = event.inFavorites;
        this.fwService.editable = event.favEditable;

        this.setGlobalSavedFilters(event.globalSavedFilters)

        this.filterPanelStateService.init(this.logs, uiConfig);

        this.effectiveFilters = this.loadEffectiveFilters();

        this.filterPanelStateService.filterChanges.subscribe(() => this.filtersChanged());

        if (!event.initByPermalink) {
            let selectedOffsetInUrl = this.getSelectedRowFromFragment();

            if (selectedOffsetInUrl) {
                this.commService.send(
                    new Command('loadDataAroundPosition', {
                        topRecordCount: this.recordCountToLoad(),
                        bottomRecordCount: this.recordCountToLoad(),
                        stateVersion: this.stateVersion,
                        hashes: this.vs.hashes,
                        logId: selectedOffsetInUrl.logId,
                        offset: selectedOffsetInUrl.offset,
                    })
                );

                this.anchor = null;
                return;
            }

            this.cleanAndScrollToEdge(this.visibleRecordCount() * 2);
        }
    }

    private getSelectedRowFromFragment(): {logId: string, offset: number} {
        let fragment = this.route.snapshot.fragment;
        if (!fragment)
            return null;

        let matcher = fragment.match(/^(?:(.+)-)?p(\d+)$/i)
        if (!matcher) {
            console.error('Invalid URL fragment: ' + fragment)
            return null;
        }

        let logId = matcher[1]
        if (!logId) {
            if (this.logs.length !== 1) {
                console.error('Invalid URL fragment, logId is missed: ' + fragment);
                return null;
            }
            logId = this.logs[0].id
        }

        return {logId, offset: parseInt(matcher[2], 10)}
    }

    private setGlobalSavedFilters(globalSavedFilters: { [p: string]: string }) {
        for (const [filterName, stateJson] of Object.entries(globalSavedFilters)) {
            let filterState = this.filterPanelStateService.parseFilterState(stateJson);
            if (filterState) {
                this.savedFilterStates[filterName] = filterState;
            }
        }

        if (this.route.snapshot.queryParams.filters == null) {
            let savedFilter = this.savedFilterStates[this.getSavedFilterName()]
            if (savedFilter) {
                this.filterPanelStateService.setFilterState(savedFilter, null);
            }
        }
    }

    showOpenLogDialog() {
        this.modalWindow = 'open-log';
    }

    addNewLog(event: OpenEvent) {
        if (this.logs?.find(l => (!l.node || l.node === this.viewConfig.localhostName) && l.path === event.path)) {
            this.toastr.info('"' + LvUtils.extractName(event.path) + '" is already present on the view');
            this.modalWindow = null;
            return;
        }

        let params = this.createParams();

        LogPathUtils.addParam(params, 'f', event.path);

        let l = window.location;
        window.location.href = l.protocol + '//' + l.host + l.pathname + '?' + LvUtils.buildQueryString(params);
    }

    onTouchStart(event: TouchEvent) {
        if (!this.touch)
            this.touch = event.changedTouches[0];
    }

    onTouchMove(event: TouchEvent) {
        if (this.touch) {
            let newTouch = this.findCurrentTouch(event.changedTouches);
            if (newTouch) {
                let d = newTouch.clientY - this.touch.clientY;

                if (d > 0) {
                    this.doUp(d);
                } else {
                    this.doDown(-d);
                }

                this.touch = newTouch;
                event.preventDefault();
            }
        }
    }

    onTouchEnd(event: TouchEvent) {
        if (this.touch && this.findCurrentTouch(event.changedTouches) != null) {
            this.touch = null;
        }
    }

    private findCurrentTouch(touchList: TouchList): Touch {
        for (let i = 0; i < touchList.length; i++) {
            if (touchList[i].identifier === this.touch.identifier) {
                return touchList[i];
            }
        }
        return null;
    }
}

interface SearchRequest {
    id: number;
    d: number;
    start: Position;
    mainRecord: number;
    searchPattern: SearchPattern;
}
