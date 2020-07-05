import {Component, OnInit} from '@angular/core';
import {RequestState} from '../utils/request-state';
import {Params, Router} from '@angular/router';
import {DirItem, FsTreeService} from './fs-tree.service';
import {FavoritesService, RestFileState} from '../services/favorites.service';
import {HttpClient} from '@angular/common/http';

@Component({
  selector: 'sl-navigator',
  templateUrl: './log-navigator.template.html',
  styles: [
    `
      .log-size {
        font-size: 12px;
        color: #999;
        margin-left: 4px;
      }

      .file-non-exist {
        font-size: 12px;
        color: red;
        display: none;
      }

      .not-exist .log-size {
        display: none;
      }

      .not-exist .file-non-exist {
        display: inline;
      }

      .fv-actions-block {
        display: inline-block;
        padding-left: 10px;
        visibility: hidden;
      }

      .favorite-item:hover .fv-actions-block {
        visibility: visible;
      }

      .fv-actions-block .fa-times {
        color: #b00;
        cursor: pointer;
        font-size: 12px;
      }
    `
  ],
  providers: [FsTreeService]
})
export class LogNavigatorComponent implements OnInit {
  state: RestInitState;

  loading: RequestState = new RequestState(true);

  pathToOpen: string;

  constructor(
    private http: HttpClient,
    private fsTreeService: FsTreeService,
    private fwService: FavoritesService,
    private router: Router
  ) {
    this.fsTreeService.favoriteListener = newState => {
      this.state.favorites.length = 0;
      this.state.favorites.push(...newState);
    };
  }

  ngOnInit() {
    this.loading.process(this.http.get<RestInitState>('rest/navigator/initState'), res => {
      this.state = res;
      this.fwService.editable = this.state.favoritesEditable;
    });
  }

  openByPath() {
    let path = (this.pathToOpen || '').trim();
    if (!path) { return; }

    let queryParams: Params = {
      log: path
    };

    this.router.navigate(['/log'], { queryParams: queryParams });
  }

  removeFromFavorites(path: string) {
    this.fwService.setFavorites(path, false);

    let idx = this.state.favorites.findIndex(p => {
      return p.path === path;
    });
    if (idx >= 0) { this.state.favorites.splice(idx, 1); }

    if (this.state.treeRoot) {
      let item = FsTreeService.findItem(this.state.treeRoot, path);
      if (item) {
        delete item.attr.favorite;
      }
    }
  }
}

interface RestInitState {
  favorites: RestFileState[];
  disabledNew: boolean;
  favoritesEditable: boolean;

  treeRoot: DirItem;
}
