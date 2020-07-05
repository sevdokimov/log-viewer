import {FieldDescr} from './view-config.service';

export interface LogFile {
    node: string;
    path: string;
    url: string;
    id: string;
    connected: boolean;

    fields: FieldDescr[];
}

export interface RestStatus {
    error: string;
    errorMessage: string;
    errorType: string;
    hash: string;
    size: number;
    lastModification: number;
}

export enum ErrorType {
    FILE_NOT_FOUND = 'NoSuchFileException', // Not found
    ACCESS_DENIED = 'AccessDeniedException', // Access Deny
    IO_EXCEPTION = 'IOException', //
    DIR_NOT_VISIBLE = 'DirectoryNotVisibleException', //
    CONNECTION_PROBLEM = 'ConnectionProblem', //
    NoDateField = 'NoDateField' //
}
