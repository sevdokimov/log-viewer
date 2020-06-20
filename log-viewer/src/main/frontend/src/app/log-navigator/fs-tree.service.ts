import {Injectable} from '@angular/core';
import {RestFileState} from '../services/favorites.service';

@Injectable()
export class FsTreeService {
    selectedPath: string;

    favoriteListener: (newState: RestFileState[]) => void;

    static collapse(dir: DirItem) {
        dir.items = null;
    }

    static findItem(dir: DirItem, path: string): FsItem {
        if (path === '') return dir;

        if (dir.items === null) return null;

        for (let d of dir.items) {
            if (d.name === path) return d;

            if (d.isDirectory) {
                let itemPath = d.name.endsWith('/') ? d.name : d.name + '/';
                if (path.startsWith(itemPath)) {
                    let restPath = path.substring(itemPath.length);
                    return this.findItem(<DirItem>d, restPath);
                }
            }
        }

        return null;
    }

    findItemBottomItem(dir: DirItem): FsItem {
        if (dir.items === null || dir.items.length === 0) return dir;

        let last = dir.items[dir.items.length - 1];

        return last.isDirectory ? this.findItemBottomItem(<DirItem>last) : last;
    }
}

export interface FsItem {
    name: string;
    path: string;

    icon: string;

    isDirectory: boolean;

    attr: { [key: string]: any };
}

export interface DirItem extends FsItem {
    items: FsItem[];
}

export interface FileItem extends FsItem {
    type: string;
    size: number;
    modificationTime: number;
}
