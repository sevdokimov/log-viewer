import {Injectable, OnDestroy, RendererFactory2} from '@angular/core';
import {BackendEvent} from './backend-events';
import {LvUtils} from '@app/utils/utils';
import {ConnectionService} from '@app/log-view/connection.service';
import {WebSocketConnection} from '@app/log-view/web-socket-connection';
import {WebsocketEmulatorConnection} from '@app/log-view/websocket-emulator-connection';
import {HttpClient} from '@angular/common/http';
import 'reflect-metadata';

const EVENT_NAME_META_KEY = 'event_name';

@Injectable()
export class CommunicationService implements OnDestroy {

    private connection: ConnectionService;
    private disconnected: boolean;

    private handler: BackendEventHandlerHolder;

    constructor(private http: HttpClient, private rendererFactory2: RendererFactory2) {
    }

    public close(disconnectMessage?: any): void {
        if (this.connection && !this.disconnected) {
            this.disconnected = true;
            this.connection.close();

            if (this.handler) {
                this.handler.disconnected(disconnectMessage);
            }
        }
    }

    private onData(event: BackendEvent) {
        if (!this.handler) {
            console.warn('Event come, but listener is not set: ' + event);
            return;
        }

        let handler = this.handler[event.name];
        if (typeof handler === 'function') {
            if (Reflect.getMetadata(EVENT_NAME_META_KEY, this.handler, event.name)) {
                handler.apply(this.handler, [event]);
                return;
            }
        }

        console.error('Unknown Event type: ' + event);
    }

    private onError(e: any) {
        console.warn('Web socket has disconnected', e);

        this.close(e);
    }

    startup(handler: BackendEventHandlerHolder) {
        LvUtils.assert(!this.disconnected);
        LvUtils.assert(!this.connection && !this.handler);

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

        this.handler = handler;

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
    constructor(public methodName: string, public args?: {[key: string]: any}) {}
}

export interface BackendEventHandlerHolder {
    disconnected(disconnectMessage?: string): any;
}

export function BackendEventHandler(eventName?: string): any {
    return (target: Function, propertyName: string) => {
        Reflect.defineMetadata(EVENT_NAME_META_KEY, true, target, eventName || propertyName);
    };
}
