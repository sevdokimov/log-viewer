import {ConnectionService} from '@app/log-view/connection.service';
import {WebSocketSubject} from 'rxjs/observable/dom/WebSocketSubject';
import {Observable, Subscription} from 'rxjs/Rx';
import {BackendEvent} from '@app/log-view/backend-events';
import {SlUtils} from '@app/utils/utils';

export class WebSocketConnection implements ConnectionService {

    private ws: WebSocketSubject<Object>;
    private socket: Subscription;

    constructor(private webSocketPath: string,
                private onData: (data: BackendEvent) => void,
                private onError: (error: any) => void,
                private onComplete: () => void,
                ) {
    }

    close(): void {
        if (this.socket) {
            this.socket.unsubscribe();
            this.ws.complete();
        }
    }

    private getWsUrl(): string {
        let loc = window.location;

        let new_uri;
        if (loc.protocol === 'https:') {
            new_uri = 'wss:';
        } else {
            if (loc.protocol !== 'http:') {
                throw new Error('Failed to construct WebSocket URL, strange protocol in URL: ' + loc.hash);
            }

            new_uri = 'ws:';
        }

        return new_uri + '//' + loc.host + this.webSocketPath;
    }

    startup(): void {
        SlUtils.assert(!this.ws);

        this.ws = Observable.webSocket(this.getWsUrl());

        this.socket = this.ws.subscribe({
            next: this.onData,
            error: this.onError,
            complete: this.onComplete,
        });
    }

    send(event: any): void {
        this.ws.next(event);
    }
}
