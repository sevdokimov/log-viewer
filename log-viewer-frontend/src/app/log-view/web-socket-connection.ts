import {ConnectionService} from '@app/log-view/connection.service';
import {BackendEvent} from '@app/log-view/backend-events';
import {LvUtils} from '@app/utils/utils';

export class WebSocketConnection implements ConnectionService {

    private ws: WebSocket;

    private sendQueue: any[];

    constructor(private webSocketPath: string,
                private onData: (data: BackendEvent) => void,
                private onError: (error: any) => void,
                private onComplete: () => void,
                ) {
    }

    close(): void {
        if (this.ws) {
            this.ws.close(1000);
            this.ws = null;
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

        return new_uri + '//' + loc.host + loc.pathname + (loc.pathname.endsWith('/') ? '' : '/../') + this.webSocketPath;
    }

    startup(): void {
        LvUtils.assert(!this.ws);

        let ws = new WebSocket(this.getWsUrl());

        this.sendQueue = [];

        ws.onopen = () => {
            if (this.sendQueue) {
                this.sendQueue.forEach(e => ws.send(e));
                this.sendQueue = null;
            }

            ws.onmessage = (event) => {
                if (this.ws === ws) {
                    this.onData(JSON.parse(event.data));
                }
            };
        };

        ws.onerror = (event) => {
            if (this.ws === ws) {
                let message = null;
                if (ws.onmessage == null) {
                    message = 'Failed to open websocket: <a href="#">' + ws.url + '</a>\n'
                        + 'Probably, you use a proxy that doesn\'t support HTTP 1.1,\nyou can switch LogViewer to ' +
                        'no-websocket mode using "<b>log-viewer.use-web-socket=false</b>" property';
                }

                this.onError(message);
            }
        };

        ws.onclose = (event) => {
            if (this.ws === ws) {
                this.onComplete();
            }
        };

        this.ws = ws;
    }

    send(event: any): void {
        if (typeof event !== 'string') {
            event = JSON.stringify(event);
        }

        if (this.ws == null) {
            throw new Error('Connection is not opened');
        }

        if (this.sendQueue) {
            this.sendQueue.push(event);
        } else {
            this.ws.send(event);
        }
    }
}
