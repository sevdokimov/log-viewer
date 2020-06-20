import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';

@Injectable()
export class FavoritesService {
    editable: boolean = true;

    constructor(private http: HttpClient) {
    }

    setFavorites(logPath: string,
                 add: boolean,
                 listener?: (newState: RestFileState[]) => void) {
        if (!this.editable) { throw 'Favorites list is not editable'; }

        let url: string;

        if (add) {
            url = 'rest/navigator/addFavoriteLog';
        } else {
            url = 'rest/navigator/removeFavoriteLog';
        }

        this.http.post<RestFileState[]>(url, logPath).subscribe(res => {
            if (listener) {
                listener(res);
            }
        });
    }
}

export interface RestFileState {
    path: string;
    size?: number;
    lastModification?: number;
}
