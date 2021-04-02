import {ConnectionService} from '@app/log-view/connection.service';
import {HttpClient} from '@angular/common/http';
import {BackendEvent} from '@app/log-view/backend-events';
import {SlUtils} from '@app/utils/utils';
import {Renderer2, RendererFactory2} from '@angular/core';

export class WebsocketEmulatorConnection implements ConnectionService {

    private toBackendQueue: ToBackendMessage[];
    private backendMessageCounter = 0;

    private sessionId: string;

    private closed: boolean;

    private toUiQueue: ToUiMessage[] = [];
    private uiMessageCounter: number = 0;

    private processingRequests = 0;

    private renderer: Renderer2;
    private listenerDestroyer: () => void;

    constructor(private http: HttpClient,
                rendererFactory2: RendererFactory2,
                private onData: (data: BackendEvent) => void,
                private onError: (error: any) => void,
                private onComplete: () => void,
    ) {
        let array = new Uint32Array(2);
        window.crypto.getRandomValues(array);
        this.sessionId = array[0].toString(16) + array[1].toString(16);

        this.renderer = rendererFactory2.createRenderer(null, null);
        this.listenerDestroyer = this.renderer.listen('window', 'beforeunload', event => {
            this.close();
        });
    }

    startup(): void {
        
    }

    send(event: any): void {
        if (this.closed) {
            return;
        }

        let messageNumber = this.backendMessageCounter++;
        let msg: ToBackendMessage = {event, messageNumber};

        if (this.backendMessageCounter === 1) {
            SlUtils.assert(this.toBackendQueue == null);
            // This is the first message to backend, next message should be queued before receiving the response for
            // the first message
            this.toBackendQueue = [];
        } else if (this.toBackendQueue != null) {
            this.toBackendQueue.push(msg);
            return;
        }

        this.sendMessages([msg]);
    }

    private sendMessages(messages: ToBackendMessage[]) {
        if (this.closed) {
            return;
        }

        let body: RestRequestBody = {sessionId: this.sessionId, messages};

        this.processingRequests++;

        this.http.post('rest/ws-emulator/request', body).subscribe(
            (resp: ToUiMessage[]) => {
                if (closed) {
                    return;
                }

                this.handleResponse(resp);

                if (this.toBackendQueue != null) {
                    this.sendMessages(this.toBackendQueue);
                    this.toBackendQueue = null;
                }
            },

            error => {
                if (closed) {
                    return;
                }

                if (error.status === 410 || error.status === 0) {
                    this.onComplete();
                } else {
                    this.onError(error.message);
                }
            },

            () => {
                if (closed) {
                    return;
                }

                if (--this.processingRequests === 0) {
                    this.sendMessages([]);
                }
            }
        );
    }

    private handleResponse(resp: ToUiMessage[]) {
        this.toUiQueue.push(...resp);

        while (true) {
            let idx = this.toUiQueue.findIndex(e => e.messageNumber === this.uiMessageCounter);
            if (idx < 0) {
                break;
            }

            let req = this.toUiQueue[idx];

            this.toUiQueue[idx] = this.toUiQueue[this.toUiQueue.length - 1];
            this.toUiQueue.length--;

            this.uiMessageCounter++;

            try {
                this.onData(req.event);
            } catch (e) {
                console.error(e);
            }
        }
    }

    close(): void {
        if (!this.closed) {
            this.closed = true;

            this.listenerDestroyer();
            this.renderer.destroy();

            this.http.post('rest/ws-emulator/closeSession', this.sessionId).subscribe();
        }
    }
}

interface ToBackendMessage {
    messageNumber: number;

    event: any;
}

interface ToUiMessage {
    messageNumber: number;

    event: BackendEvent;
}

interface RestRequestBody {
    sessionId: string;

    messages: ToBackendMessage[];
}
