import {Observable} from 'rxjs';
import {HttpErrorResponse} from '@angular/common/http';

export class RequestState {

    get success(): boolean {
        return !this.loading && !this.errorStatus;
    }
    loading: boolean;
    errorStatus: number;
    errorMsg: string;
    done = false;

    listeners: ((r: RequestState) => void)[] = [];

    private counter: number = 0;

    constructor(private oneRequestAtSameTime: boolean = false) {
    }

    process<T>(o: Observable<T>,
               successHandler?: (value: T) => void,
               errorHandler?: (err: any) => void) {
        if (this.oneRequestAtSameTime && this.loading) { return; }

        this.errorStatus = null;
        this.errorMsg = null;
        this.loading = true;

        this.notifyListeners();

        let reqNumber = ++this.counter;

        o.subscribe(
            res => {
                // Request is obsolete. Only latest request should be handled.
                if (this.counter !== reqNumber) { return; }

                this.loading = false;

                this.notifyListeners();

                if (successHandler) {
                    successHandler(res);
                }
            },
            (err: HttpErrorResponse) => {
                // Request is obsolete. Only latest request should be handled.
                if (this.counter !== reqNumber) { return; }

                this.errorMsg = err.statusText;
                this.errorStatus = err.status;
                this.loading = false;

                this.notifyListeners();

                if (errorHandler) {
                    errorHandler(err);
                }
            }
        );
    }

    addListener(listener: ((r: RequestState) => void)) {
        this.listeners.push(listener);
    }

    private notifyListeners() {
        for (let l of this.listeners) {
            l(this);
        }
    }
}
