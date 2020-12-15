import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {RequestState} from '../utils/request-state';
import {Router} from '@angular/router';
import {FavoritesService, RestFileState} from '../services/favorites.service';
import {HttpClient} from '@angular/common/http';
import {SlUtils} from '@app/utils/utils';

@Component({
    selector: 'sl-navigator',
    templateUrl: './log-navigator.template.html',
    styleUrls: ['./log-navigator.style.scss'],
})
export class LogNavigatorComponent implements OnInit {

    @ViewChild('searchField', {static: false})
    searchField: ElementRef;
    @ViewChild('rootElement', {static: true})
    rootElement: ElementRef;

    initialLoading: RequestState = new RequestState(true);

    typedText: string = '';
    filterOpened: boolean;

    init: boolean;

    favorites: RestFileState[];
    favoritesEditable: boolean;
    showFileTree: boolean;

    currentDir: string;
    currentDirItems: string[];

    selectedPath: string;

    dirContent: FsItem[];

    visibleDirItems: {item: FsItem, nameHtml: string}[];

    dirContentLoading: RequestState = new RequestState(true);

    constructor(
        private http: HttpClient,
        public fwService: FavoritesService,
        private router: Router
    ) {

    }

    ngOnInit() {
        this.initialLoading.process(this.http.get<RestInitState>('rest/navigator/initState'), res => {
            this.init = true;

            this.favorites = res.favorites;
            this.favoritesEditable = res.favoritesEditable;
            this.showFileTree = res.showFileTree;

            if (res.showFileTree) {
                this.currentDir = res.initPath;
                this.currentDirItems = LogNavigatorComponent.parsePath(res.initPath);
                this.setDirContent(res.initDirContent);
            }

            this.fwService.editable = this.favoritesEditable;
        });
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
        return this.currentDirItems.slice(0, idx + 1).join('/');
    }

    selectDir(dir: string) {
        if (dir === this.currentDir) {
            return;
        }

        this.currentDir = dir;
        this.currentDirItems = LogNavigatorComponent.parsePath(dir);

        this.dirContent = null;
        this.visibleDirItems = null;

        this.closeSearch();

        this.dirContentLoading.process(
            this.http.get<FsItem[]>('rest/navigator/listDir', {params: {dir}}),
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

    private setDirContent(dirContent: FsItem[]) {
        this.dirContent = dirContent;

        this.doSearch();

        this.adjustSelection();
    }

    private doSearch() {
        this.visibleDirItems = [];

        if (!this.dirContent) { return; }

        let filter = this.typedText;

        for (let item of this.dirContent) {
            let name = this.currentDir ? item.name : item.path;
            
            let nameHtml: string;

            if (!filter) {
                nameHtml = SlUtils.escapeHtml(name);
            } else {
                nameHtml = LogNavigatorComponent.highlightOccurrence(name, filter);
                if (!nameHtml) { continue; }
            }

            this.visibleDirItems.push({item, nameHtml});
        }
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
            res += SlUtils.escapeHtml(s.substring(idx, k));
            res += '<span class="occurrence">' + SlUtils.escapeHtml(s.substring(k, k + filter.length)) + '</span>';

            idx = k + filter.length;
            k = lowerCaseS.indexOf(filter, idx);
        }

        res += SlUtils.escapeHtml(s.substring(idx));

        return res;
    }

    private adjustSelection() {
        SlUtils.assert(this.visibleDirItems != null);

        if (this.visibleDirItems.length === 0) {
            return;
        }

        if (!this.selectedPath) {
            this.selectedPath = this.visibleDirItems[0].item.path;
            return;
        }

        if (SlUtils.isChild(this.currentDir, this.selectedPath)) {
            if (this.currentDir === this.selectedPath) {
                this.selectedPath = this.visibleDirItems[0].item.path;
                return;
            }

            let idx = this.visibleDirItems.findIndex(fs => SlUtils.isChild(fs.item.path, this.selectedPath));
            if (idx >= 0) {
                this.selectedPath = this.visibleDirItems[idx].item.path;
                return;
            }

            let orderedFiles = this.visibleDirItems.map(fs => fs.item.name).sort();
            idx = SlUtils.binarySearch(orderedFiles, this.selectedPath.substring(this.currentDir.length + 1));
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

        path = path.replace(/\\\\/g, '/').replace(/\/{2,}/g, '/');

        if (path === '/') {
            return ['/'];
        }

        if (path.endsWith('/')) {
            path = path.substr(0, path.length - 1);
        }

        let res: string[] = [];

        if (path.startsWith('/')) {
            res.push('/');
            path = path.substring(1);
        }

        res.push(...path.split('/'));

        return res;
    }

    select(evt: MouseEvent, path: string) {
        this.selectedPath = path;
        this.searchField.nativeElement.focus();
    }

    private openItem(fsItem: FsItem, inNewWindow: boolean) {
        if (fsItem.isDirectory) {
            this.selectDir(fsItem.path);
        } else {
            let fileType = (<FileItem>fsItem).type;

            if (fileType === 'log' || fileType === 'out' || fileType === 'text') {
                if (inNewWindow) {
                    window.open('log?path=' + encodeURI(fsItem.path));
                } else {
                    this.router.navigate(['/log'], {
                        queryParams: {path: fsItem.path}
                    });
                }
            }
        }
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
        if (event.charCode !== 0) {
            if (!this.filterOpened) {
                this.openSearch();
                this.typedText = event.key;
                this.doSearch();
            }
        }
    }

    fakeInputPressed(event: KeyboardEvent) {
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
        this.adjustSelection();
    }
}

interface RestInitState {
    favorites: RestFileState[];

    favoritesEditable: boolean;

    showFileTree: boolean;
    initPath?: string;

    initDirContent: FsItem[];
}

export interface FsItem {
    name: string;
    path: string;

    icon: string;

    isDirectory: boolean;

    attr: { [key: string]: any };
}

export interface FileItem extends FsItem {
    type: string;
    size: number;
    modificationTime: number;
}
