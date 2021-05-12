import {Params} from '@angular/router';
import {LvUtils} from '@app/utils/utils';

export class LogPathUtils {

    private static toStringArray(p: any): string[] {
        if (typeof p === 'string') {
            return [p];
        }

        if (p && Array.isArray(p)) {
            return p;
        }

        return [];
    }

    public static addParam(params: Params, name: string, value: string) {
        let v =  params[name];

        if (!v) {
            params[name] = value;
        } else if (typeof v === 'string') {
            params[name] = [v, value];
        } else {
            LvUtils.assert(Array.isArray(v));
            v.push(value);
        }
    }

    public static getListParamMap(params: Params): {[key: string]: string} {
        let res: {[key: string]: string} = {};

        if (params.log) {
            res.log = params.log;
        }
        if (params.path) {
            res.path = params.path;
        }
        if (params.f) {
            res.f = params.f;
        }
        if (params.ssh) {
            res.ssh = params.ssh;
        }
        
        return res;
    }

    public static extractLogList(params: Params): LogList {
        let pathsInLegacyFormat: string[] = [];

        let p = LogPathUtils.toStringArray(params.path);
        if (p) {
            pathsInLegacyFormat.push(...p);
        }

        p = LogPathUtils.toStringArray(params.log);
        if (p) {
            pathsInLegacyFormat.push(...p);
        }

        return {
            pathsInLegacyFormat,
            ssh: LogPathUtils.toStringArray(params.ssh),
            files: LogPathUtils.toStringArray(params.f),
            bookmarks: LogPathUtils.toStringArray(params.bookmark),
        };
    }


}

export interface LogList {
    pathsInLegacyFormat: string[];
    ssh: string[];
    files: string[];
    bookmarks: string[];
}
