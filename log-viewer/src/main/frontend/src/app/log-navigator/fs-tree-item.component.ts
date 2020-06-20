import {Component, Input, OnChanges, SimpleChange} from '@angular/core';
import {RequestState} from '../utils/request-state';
import {Router} from '@angular/router';
import {DirItem, FileItem, FsItem, FsTreeService} from './fs-tree.service';
import {FavoritesService} from '../services/favorites.service';
import {HttpClient} from '@angular/common/http';

@Component({
    selector: 'sl-fs-tree-item',
    templateUrl: './fs-tree-item.template.html',
    styleUrls: ['./fs-tree-item.style.scss']
})
export class FsTreeItemComponent implements OnChanges {

    get dir(): DirItem {
        if (!this.item) {
            return null;
        }

        return this.isDir ? <DirItem>this.item : null;
    }

    get file(): FileItem {
        if (!this.item) {
            return null;
        }

        return this.isDir ? null : <FileItem>this.item;
    }
    private static favoriteAvailableTypesStatic: { [key: string]: boolean } = {
        log: true
    };

    favoriteAvailableTypes: { [key: string]: boolean } =
        FsTreeItemComponent.favoriteAvailableTypesStatic;

    @Input()
    item: FsItem;
    @Input()
    deep: number;

    isDir: boolean;

    navigationUrl: string;

    reqLoading: RequestState = new RequestState(true);

    constructor(private http: HttpClient,
                public fsTreeService: FsTreeService,
                private fwService: FavoritesService,
                private router: Router) {
    }

    ngOnChanges(changes: { [key: string]: SimpleChange }): any {
        if (changes['item']) {
            this.isDir = this.item.isDirectory;

            if (!this.isDir) {
                let fileType = (<FileItem>this.item).type;
                if (fileType === 'log' || fileType === 'out' || fileType === 'text') {
                    this.navigationUrl = '/log';
                }
            }
        }
    }

    static isClickToDirExpander(evt: MouseEvent) {
        return (
            evt.target &&
            (<Element>evt.target).tagName === 'SPAN' &&
            (<Element>evt.target).parentElement.className === 'dir-tool-icon'
        );
    }

    dblClick(evt: MouseEvent) {
        if (this.isDir) {
            if (!FsTreeItemComponent.isClickToDirExpander(evt)) {
                this.toggleDirState();
                window.getSelection().removeAllRanges();
            }
        } else {
            let fileType = (<FileItem>this.item).type;
            if (fileType === 'log' || fileType === 'out' || fileType === 'text') {
                if (evt.ctrlKey) {
                    window.open('log?path=' + encodeURI(this.item.path));
                } else {
                    this.router.navigate([this.navigationUrl], {
                        queryParams: {path: this.item.path}
                    });
                }
            }
        }
    }

    onClick(evt: MouseEvent) {
        if (this.isDir && FsTreeItemComponent.isClickToDirExpander(evt)) {
            this.toggleDirState();
        }

        this.fsTreeService.selectedPath = this.item.path;
    }

    changeFavorites() {
        if (!this.fwService.editable) { return; }

        this.item.attr.favorite = !this.item.attr.favorite;
        this.fwService.setFavorites(
            this.item.path,
            this.item.attr.favorite,
            newState => {
                this.fsTreeService.favoriteListener(newState);
            }
        );
    }

    private expand() {
        if (!this.isDir) { return; }

        if (this.dir.items != null) { return; }

        this.reqLoading.process(
            this.http.get<FsItem[]>('rest/navigator/listDir', {
                params: {dir: this.dir.path}
            }),
            res => {
                this.dir.items = res;
            }
        );
    }

    private toggleDirState() {
        if (this.dir.items == null) {
            this.expand();
        } else {
            FsTreeService.collapse(this.dir);
        }
    }
}
