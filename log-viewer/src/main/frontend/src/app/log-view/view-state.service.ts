import {Injectable} from '@angular/core';
import {Position} from './position';

@Injectable()
export class ViewStateService {
    selectedLine: Position;
}
