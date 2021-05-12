import {Pipe, PipeTransform} from '@angular/core';

@Pipe({
    name: 'slDuration'
})
export class SlDurationPipe implements PipeTransform {
    transform(x: number): string {
        if (x < 1000) {
            return x + 'ms';
        }

        if (x < 3000) {
            return x / 1000 + 's';
        }

        if (x < 60000) {
            return Math.round(x / 1000) + 's';
        }

        if (x < 60000) {
            return Math.round(x / 1000) + 's';
        }

        return Math.floor(x / 60000) + 'min ' + (Math.round(x / 1000) % 60) + 's';
    }
}
