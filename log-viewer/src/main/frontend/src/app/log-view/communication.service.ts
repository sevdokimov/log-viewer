import {Injectable, OnDestroy, RendererFactory2} from '@angular/core';
import {BackendEvent} from './backend-events';
import {SlUtils} from '@app/utils/utils';
import {ConnectionService} from '@app/log-view/connection.service';
import {WebSocketConnection} from '@app/log-view/web-socket-connection';
import {WebsocketEmulatorConnection} from '@app/log-view/websocket-emulator-connection';
import {HttpClient} from '@angular/common/http';

@Injectable()
export class CommunicationService implements OnDestroy {

    private connection: ConnectionService;
    private disconnected: boolean;

    private eventHandlers: { [key: string]: any };

    constructor(private http: HttpClient, private rendererFactory2: RendererFactory2) {
    }

    public close(): void {
        if (this.connection && !this.disconnected) {
            this.disconnected = true;
            this.connection.close();

            if (this.eventHandlers) {
                let disconnectListener = this.eventHandlers['disconnected'];
                if (disconnectListener) {
                    disconnectListener();
                }
            }
        }
    }

    private onData(event: BackendEvent) {
        if (!this.eventHandlers) {
            console.warn('Event come, but listener is not set: ' + event);
            return;
        }

        let fun = this.eventHandlers[event.name];

        if (!fun) {
            console.error('Unknown Event type: ' + event);
            return;
        }

        fun.apply(null, [event]);
    }

    private onError(e: any) {
        console.warn('Web socket has disconnected', e);

        this.close();
    }

    startup(eventHandlers: { [key: string]: any }) {
        SlUtils.assert(!this.disconnected);
        SlUtils.assert(!this.connection && !this.eventHandlers);

        let webSocketPath = (<any>window).webSocketPath;
        if (webSocketPath) {
            this.connection = new WebSocketConnection(webSocketPath, (d) => this.onData(d),
                (e) => this.onError(e),
                () => this.close());
        } else {
            this.connection = new WebsocketEmulatorConnection(this.http, this.rendererFactory2,
                (d) => this.onData(d),
                (e) => this.onError(e),
                () => this.close());
        }

        this.eventHandlers = eventHandlers;

        this.connection.startup();
    }

    send(command: Command) {
        this.connection.send(command);
    }

    ngOnDestroy(): void {
        if (this.connection) {
            this.connection.close();
        }
    }
}

export class Command {
    constructor(private methodName: string, private args?: {[key: string]: any}) {}
}
