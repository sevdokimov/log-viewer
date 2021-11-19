import {FieldDescr} from './view-config.service';

export interface LogFile {
    node: string;
    path: string;
    url: string;
    id: string;
    connected: boolean;
    hasFullDate: boolean;

    format: string;

    fields: FieldDescr[];
}

export interface RestStatus {
    hash: string;
    size: number;
    lastModification: number;

    errorType: ErrorType;
    errorMessage: string;
    detailedErrorMessage: string;
    metainfo: any;
}

export enum ErrorType {
    FILE_NOT_FOUND = 'NoSuchFileException', // Not found
    ACCESS_DENIED = 'AccessDeniedException', // Access Deny
    DIR_NOT_VISIBLE = 'DirectoryNotVisibleException', //
    CONNECTION_PROBLEM = 'ConnectionProblem', //
    IO_EXCEPTION = 'IOException', //
    NoDateField = 'NoDateField', //
    INCORRECT_FORMAT = 'IncorrectFormatException',

    LOG_CRASHED_EXCEPTION = 'LogCrashedException',
    INTERNAL_ERROR = 'internal_error',
}
