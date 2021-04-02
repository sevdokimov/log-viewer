
export interface ConnectionService {
    startup(): void;

    send(event: any): void;

    close(): void;
}
