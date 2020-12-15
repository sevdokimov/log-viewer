import {SlStyle} from '../log-view/renderers/style';

export class SlUtils {
    static EMPTY_ARRAY: any[] = [];

    static renderFileSize(size: number): string {
        if (size < 4 * 1024) { return size + ' bytes'; }

        if (size < 1024 * 1024) { return Math.round(size / 1024) + 'kb'; }

        if (size < 3 * 1024 * 1024) {
            return Math.round((size * 10) / (1024 * 1024)) / 10 + 'Mb';
        }

        return Math.round(size / (1024 * 1024)) + 'Mb';
    }

    static renderTimePeriod(x: number): string {
        if (x < 1000) {
            return x + 'ms';
        }

        if (x < 10000) {
            return (x / 1000) + 's';
        }

        if (x < 60000) {
            return Math.round(x / 1000) + 's';
        }

        if (x < 60 * 60000) {
            let m = Math.floor(x / 60000);
            let s = Math.round(x / 1000) % 60;

            if (s === 0) {
                return m + 'min';
            }

            return m + 'min ' + s + 's';
        }

        let d = Math.floor(x / (24 * 60 * 60000));
        x = x % (24 * 60 * 60000);

        let h = Math.floor(x / (60 * 60000));
        x = x % (60 * 60000);

        let m = Math.floor(x / 60000);
        x = x % 60000;
        let s = Math.floor(x / 1000);

        let time = h.toString().padStart(2, '0') + ':' + m.toString().padStart(2, '0') + ':' + s.toString().padStart(2, '0');
        if (d === 0) {
            return time;
        }

        let days = d + (d === 1 ? 'day' : 'days');

        if (time === '00:00:00') {
            return days;
        }

        return days + ' ' + time;
    }

    static applyStyle(e: HTMLElement, style: SlStyle) {
        if (!style) {
            return;
        }

        if (style.color !== undefined) {
            e.style.color = style.color;
        }
        if (style.fontWeight !== undefined) {
            e.style.fontWeight = style.fontWeight;
        }
        if (style.fontStyle !== undefined) {
            e.style.fontStyle = style.fontStyle;
        }
        if (style.className) {
            e.classList.add(style.className);
        }
    }

    static clearElement(e: HTMLElement) {
        while (e.firstChild) { e.removeChild(e.firstChild); }
    }

    static assert(f: boolean, message?: string) {
        if (!f) {
            throw new Error(message);
        }
    }

    static lastParam(param: any) {
        if (!param) { return null; }

        if (param instanceof Array) {
            if (param.length === 0) { return null; }

            return param[param.length - 1];
        }

        return param;
    }

    static buildQueryString(param: any) {
        let res = [];
        for (let name of Object.keys(param)) {
            let val = param[name];
            if (Array.isArray(val)) {
                for (let v of val) {
                    res.push(name + '=' + encodeURIComponent(v));
                }
            } else {
                res.push(name + '=' + encodeURIComponent(val));
            }
        }

        return res.join('&');
    }

    static formatDate(date: number | Date): string {
        if (typeof date === 'number') {
            date = new Date(date);
        }

        return date.toLocaleString('en-CA', {timeZoneName: 'short', hour12: false});
    }

    static distinct(m: string[]): string[] {
        let set = {};

        for (let s of m) {
            if (s) { set[s] = true; }
        }

        return Object.keys(set);
    }

    static normalizePath(path: string): string {
        path = path.replace(/\\\\/g, '/').replace(/\/{2,}/g, '/');

        if (path.length > 1 && path.endsWith('/')) {
            path = path.substr(0, path.length - 1);
        }

        return path;
    }

    static isChild(parent: string, child: string): boolean {
        parent = SlUtils.normalizePath(parent);
        child = SlUtils.normalizePath(child);

        if (!child.startsWith(parent)) {
            return false;
        }

        if (child.length === parent.length) {
            return true;
        }

        return child.charAt(parent.length) === '/';
    }

    static escapeHtml(s: string): string {
        return s.replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/&/g, '&amp;');
    }

    static binarySearch<T>(m: T[], key: T): number {
        let low = 0;
        let high = m.length - 1;

        while (low <= high) {
            let mid = Math.floor((low + high) / 2);
            let midVal = m[mid];

            if (midVal < key) {
                low = mid + 1;
            } else if (midVal > key) {
                high = mid - 1;
            } else {
                return mid; // key found
            }
        }
        return -(low + 1);  // key not found
    }

    static extractName(path: string): string {
        path = SlUtils.normalizePath(path);
        let idx = path.lastIndexOf('/');
        if (idx < 0) {
            return path;
        }

        return path.substring(idx + 1);
    }
}
