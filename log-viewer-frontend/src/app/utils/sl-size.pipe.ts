import {Pipe, PipeTransform} from '@angular/core';

@Pipe({
    name: 'slSize'
})
export class SlSizePipe implements PipeTransform {
    transform(x: number): string {
        if (x == null || x < 0) { return ''; }

        if (x < 1000) { return x + ' bytes'; }

        if (x <= 1024) { return '1.0kB'; }

        if (x < 1024 * 3) { return (x / 1024).toFixed(1) + 'kB'; }

        if (x < 1024 * 1024) { return Math.round(x / 1024) + 'kB'; }

        if (x < 3 * 1024 * 1024) { return (x / (1024 * 1024)).toFixed(1) + 'MB'; }

        if (x < 1024 * 1024 * 1024) { return Math.floor(x / (1024 * 1024)) + 'MB'; }

        if (x < 3 * 1024 * 1024 * 1024) {
            return (x / (1024 * 1024 * 1024)).toFixed(1) + 'GB';
        }

        return Math.floor(x / (1024 * 1024 * 1024)) + 'GB';
    }
}
