import {Highlight, TextRenderer} from '@app/log-view/renderers/renderer';

const activeBracketStyle = 'lv-bracket-active';

const bracketStyle = 'lv-bracket';

export class BracketsHighlighter implements TextRenderer {

    constructor(args: any) {
    }

    tryRender(s: string): Highlight[] {
        let res: Highlight[] = [];

        for (let p of BracketsHighlighter.findPairs(s)) {
            let start = document.createElement('SPAN');
            start.textContent = s[p[0]];
            start.className = bracketStyle;

            let end = document.createElement('SPAN');
            end.textContent = s[p[1]];
            end.className = bracketStyle;

            let onMouseEnter = (e: MouseEvent) => {
                start.classList.add(activeBracketStyle);
                end.classList.add(activeBracketStyle);
            };

            let onMouseLeave = (e: MouseEvent) => {
                start.classList.remove(activeBracketStyle);
                end.classList.remove(activeBracketStyle);
            };

            start.onmouseenter = onMouseEnter;
            end.onmouseenter = onMouseEnter;

            start.onmouseleave = onMouseLeave;
            end.onmouseleave = onMouseLeave;

            res.push({start: p[0], end: p[0] + 1, e: start});
            res.push({start: p[1], end: p[1] + 1, e: end});
        }

        return res.sort((a, b) => a.start - b.start);
    }

    private static findPairs(s: string): number[][] {
        let stackChar: string[] = [];
        let stackIndex: number[] = [];

        let pairs: number[][] = [];

        for (let i = 0; i < s.length; i++) {
            let c = s[i];

            let isOpen: boolean = null;
            let opposite: string;

            switch (c) {
                case '[':
                    isOpen = true;
                    opposite = ']';
                    break;
                case '(':
                    isOpen = true;
                    opposite = ')';
                    break;
                case '{':
                    isOpen = true;
                    opposite = '}';
                    break;
                case ']':
                    isOpen = false;
                    opposite = '[';
                    break;
                case '}':
                    isOpen = false;
                    opposite = '{';
                    break;
                case ')':
                    isOpen = false;
                    opposite = '(';
                    break;

                default:
                    continue;
            }

            if (isOpen) {
                stackChar.push(c);
                stackIndex.push(i);
            } else {
                if (stackChar.pop() === opposite) {
                    pairs.push([stackIndex.pop(), i]);
                } else {
                    stackChar.length = 0;
                    stackIndex.length = 0;
                }
            }
        }

        return pairs;
    }
}
