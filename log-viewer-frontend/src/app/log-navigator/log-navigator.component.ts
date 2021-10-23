import {AfterViewInit, Component, ElementRef, EventEmitter, OnInit, Output, ViewChild} from '@angular/core';
import {RequestState} from '../utils/request-state';
import {FavoritesService, RestFileState} from '../services/favorites.service';
import {HttpClient} from '@angular/common/http';
import {LvUtils} from '@app/utils/utils';

@Component({
    selector: 'lv-navigator',
    templateUrl: './log-navigator.template.html',
    styleUrls: ['./log-navigator.style.scss'],
})
export class LogNavigatorComponent implements OnInit, AfterViewInit {

    @ViewChild('searchField')
    searchField: ElementRef;
    @ViewChild('rootElement', {static: true})
    rootElement: ElementRef;
    @ViewChild('pathInput', {static: false})
    pathInput: ElementRef;

    @Output() openFile = new EventEmitter<OpenEvent>();

    initialLoading: RequestState = new RequestState(true);

    typedText: string = '';
    filterOpened: boolean;

    init: boolean;

    favorites: RestFileState[];
    favoritesEditable: boolean;
    showFileTree: boolean;

    editingCurrentDir: boolean;
    editedCurrentDirValue: string;

    currentDir: string;
    currentDirItems: string[];

    selectedPath: string;

    dirContent: DirContent;

    visibleDirItems: {item: FsItem, nameHtml: string}[];

    dirContentLoading: RequestState = new RequestState(true);

    constructor(
        private http: HttpClient,
        public fwService: FavoritesService,
    ) {

    }

    ngAfterViewInit(): void {
        this.rootElement.nativeElement.focus();
    }

    ngOnInit() {
        this.initialLoading.process(this.http.get<RestInitState>('rest/navigator/initState'), res => {
            this.init = true;

            this.favorites = res.favorites;
            this.favoritesEditable = res.favoritesEditable;
            this.showFileTree = res.showFileTree;

            if (res.showFileTree) {
                this.setCurrentDir(res.initPath);
                this.setDirContent(res.initDirContent);
            }

            this.fwService.editable = this.favoritesEditable;
        });
    }

    private setCurrentDir(dir: string) {
        this.currentDir = dir;
        this.currentDirItems = LogNavigatorComponent.parsePath(dir);
    }

    cancelEditing() {
        this.editingCurrentDir = false;
        this.rootElement.nativeElement.focus();
    }

    removeFromFavorites(path: string) {
        this.fwService.setFavorites(path, false);

        let idx = this.favorites.findIndex(p => {
            return p.path === path;
        });
        if (idx >= 0) {
            this.favorites.splice(idx, 1);
        }
    }

    constructPathFromPrefix(idx: number): string {
        return LvUtils.normalizePath(this.currentDirItems.slice(0, idx + 1).join('/'));
    }

    directoryNameClick(dir: string) {
        this.selectDir(dir);
        return false;
    }

    selectDir(dir: string) {
        if (dir === this.currentDir) {
            return;
        }

        this.setCurrentDir(dir);

        this.dirContent = null;
        this.visibleDirItems = null;

        this.closeSearch();

        this.dirContentLoading.process(
            this.http.get<DirContent>('rest/navigator/listDir', {params: {dir}}),
            items => {
                if (dir !== this.currentDir) {
                    return;
                }

                this.setDirContent(items);
            }
        );
    }

    private closeSearch() {
        this.typedText = '';
        this.filterOpened = false;
        this.rootElement.nativeElement.focus();
        this.doSearch();
    }

    private openSearch() {
        if (this.filterOpened) {
            return;
        }

        this.typedText = '';
        this.filterOpened = true;

        setTimeout(() => {
            this.searchField.nativeElement.focus();
        }, 0);
    }

    private setDirContent(dirContent: DirContent) {
        this.dirContent = dirContent;

        this.doSearch();
    }

    private doSearch() {
        this.visibleDirItems = [];

        if (!this.dirContent?.content) { return; }

        let filter = this.typedText;

        for (let item of this.dirContent.content) {
            let name = this.currentDir ? item.name : item.path;
            
            let nameHtml: string;

            if (!filter) {
                nameHtml = LvUtils.escapeHtml(name);
            } else {
                nameHtml = LogNavigatorComponent.highlightOccurrence(name, filter);
                if (!nameHtml) { continue; }
            }

            this.visibleDirItems.push({item, nameHtml});
        }

        this.adjustSelection();
    }

    private static highlightOccurrence(s: string, filter: string): string {
        let lowerCaseS = s.toLowerCase();
        filter = filter.toLowerCase();

        let res = '';

        let idx = 0;

        let k = lowerCaseS.indexOf(filter);
        if (k < 0) {
            return null;
        }

        while (k >= 0) {
            res += LvUtils.escapeHtml(s.substring(idx, k));
            res += '<span class="occurrence">' + LvUtils.escapeHtml(s.substring(k, k + filter.length)) + '</span>';

            idx = k + filter.length;
            k = lowerCaseS.indexOf(filter, idx);
        }

        res += LvUtils.escapeHtml(s.substring(idx));

        return res;
    }

    private adjustSelection() {
        LvUtils.assert(this.visibleDirItems != null);

        if (this.visibleDirItems.length === 0) {
            return;
        }

        if (!this.selectedPath) {
            this.selectedPath = this.visibleDirItems[0].item.path;
            return;
        }

        if (LvUtils.isChild(this.currentDir, this.selectedPath)) {
            if (this.currentDir === this.selectedPath) {
                this.selectedPath = this.visibleDirItems[0].item.path;
                return;
            }

            let idx = this.visibleDirItems.findIndex(fs => LvUtils.isChild(fs.item.path, this.selectedPath));
            if (idx >= 0) {
                this.selectedPath = this.visibleDirItems[idx].item.path;
                return;
            }

            let orderedFiles = this.visibleDirItems.map(fs => fs.item.name.toLowerCase()).sort();
            idx = LvUtils.binarySearch(orderedFiles, this.selectedPath.substring(this.currentDir.length + 1).toLowerCase());
            if (idx >= 0) {
                this.selectedPath = this.visibleDirItems[idx].item.path;
                return;
            }

            idx = -idx - 1;
            this.selectedPath = this.visibleDirItems[Math.min(idx, this.visibleDirItems.length - 1)].item.path;
            return;
        }

        this.selectedPath = this.visibleDirItems[0].item.path;
    }

    private static parsePath(path: string): string[] {
        if (!path) {
            return [];
        }

        path = LvUtils.normalizePath(path);

        let res: string[] = [];

        if (path.startsWith('/')) {
            res.push('/');
            path = path.substring(1);
        } else if (/^[a-z]:\//i.test(path)) {
            res.push(path.substring(0, 3));
            path = path.substring(3);
        }

        if (path.length > 0) {
            res.push(...path.split('/'));
        }

        return res;
    }

    select(evt: MouseEvent, path: string) {
        this.selectedPath = path;
        this.searchField.nativeElement.focus({preventScroll: true});
    }

    private openItem(fsItem: FsItem, inNewWindow: boolean) {
        if (fsItem.isDirectory) {
            this.selectDir(fsItem.path);
        } else {
            let fileType = fsItem.type;

            if (fileType === 'log' || fileType === 'out' || fileType === 'text') {
                this.openFile.emit({path: fsItem.path, isCtrlClick: inNewWindow});
            }
        }
    }

    favoriteClick(path: string, event: MouseEvent) {
        this.openFile.emit({path: path, isCtrlClick: event.ctrlKey});
        return false;
    }

    dblClick(fsItem: FsItem, evt: MouseEvent) {
        this.openItem(fsItem, evt.ctrlKey);
    }

    toggleSearch() {
        if (this.filterOpened) {
            this.closeSearch();
        } else {
            this.openSearch();
        }
    }

    fakeKeyPressed(event: KeyboardEvent) {
        if ((<Element>event.target).id === 'path-input') {
            return;
        }

        if (event.charCode !== 0) {
            if (!this.filterOpened) {
                this.openSearch();
                this.typedText = event.key;
                this.doSearch();
            }
        }
    }

    fakeInputPressed(event: KeyboardEvent) {
        if ((<Element>event.target).id === 'path-input') {
            return;
        }

        if (event.charCode !== 0) {
            return;
        }

        let searchFieldFocus = (<HTMLElement>event.target).tagName === 'INPUT';

        switch (event.keyCode) {
            case 13: // ENTER
                this.enterPressed(event);
                break;
            case 38: // UP_ARROW
                this.navigateUp();
                break;
            case 40: // DOWN_ARROW
                this.navigateDown();
                break;
            case 8: // BACKSPACE
                if (searchFieldFocus) { return; }

                this.goToParent();
                break;
            case 27: // ESCAPE
                if (!searchFieldFocus) { return; }

                this.closeSearch();
                break;
            default:
                return;
        }
        
        event.preventDefault();
    }

    private findSelectedIndex(): number {
        if (!this.visibleDirItems || !this.selectedPath) {
            return -1;
        }

        return this.visibleDirItems.findIndex(fs => fs.item.path === this.selectedPath);
    }

    private enterPressed(event: KeyboardEvent) {
        let idx = this.findSelectedIndex();
        if (idx >= 0) {
            this.openItem(this.visibleDirItems[idx].item, event.ctrlKey);
        }
    }

    private navigateUp() {
        let idx = this.findSelectedIndex();
        if (idx > 0) {
            this.selectedPath = this.visibleDirItems[idx - 1].item.path;
        }
    }

    private goToParent() {
        if (this.currentDirItems.length > 1) {
            this.selectDir(this.constructPathFromPrefix(this.currentDirItems.length - 2));
        }
    }

    private navigateDown() {
        let idx = this.findSelectedIndex();
        if (idx >= 0 && idx < this.visibleDirItems.length - 1) {
            this.selectedPath = this.visibleDirItems[idx + 1].item.path;
        }
    }

    filterChanged() {
        this.doSearch();
    }

    startPathEditing() {
        this.editedCurrentDirValue = this.currentDir;
        this.editingCurrentDir = true;

        setTimeout(() => {
            this.pathInput.nativeElement.focus();
            this.pathInput.nativeElement.select();
        }, 0);
    }

    pathInputKeyDown(event: KeyboardEvent) {
        if (event.keyCode === 27) { // escape
            this.cancelEditing();
            return false;
        } else if (event.keyCode === 13) {
            this.openEditedPath();
            return false;
        }
    }

    openEditedPath() {
        if (this.editedCurrentDirValue !== this.currentDir) {
            this.setCurrentDir(this.editedCurrentDirValue);

            this.dirContent = null;
            this.visibleDirItems = null;

            this.closeSearch();

            this.dirContentLoading.process(
                this.http.get<OpenCustomDirResponse>('rest/navigator/openCustomDir', {params: {dir: this.currentDir}}),
                resp => {
                    if (resp.newCurrentDir && this.currentDir !== resp.newCurrentDir) {
                        this.setCurrentDir(resp.newCurrentDir);
                    }

                    this.setDirContent(resp.content);

                    if (resp.selectedPath) {
                        this.selectedPath = resp.selectedPath;
                    } else {
                        this.adjustSelection();
                    }
                }
            );
        }

        this.cancelEditing();
    }
}

interface RestInitState {
    favorites: RestFileState[];

    favoritesEditable: boolean;

    showFileTree: boolean;
    initPath?: string;

    initDirContent: DirContent;
}

export interface FsItem {
    name: string;
    path: string;

    icon: string;

    isDirectory: boolean;

    attr: { [key: string]: any };

    type?: string;
    size?: number;
    modificationTime?: number;
}

interface OpenCustomDirResponse {
    newCurrentDir: string;
    selectedPath?: string;
    content: DirContent;
}

interface DirContent {
    content: FsItem[];
    error: string;
}

export interface OpenEvent {
    path: string;
    isCtrlClick: boolean;
}
