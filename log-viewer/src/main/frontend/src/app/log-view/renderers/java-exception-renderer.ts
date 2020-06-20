import {Highlight, Text2HtmlConverter, TextRenderer} from './renderer';
import {SlElement} from '@app/utils/sl-element';
import {SlUtils} from '@app/utils/utils';

const exceptionMessageClass = 'exception-message';

export class JavaExceptionRenderer implements TextRenderer {
    private static exceptionRegexp: RegExp = new RegExp(
        /((?:[a-zA-Z_$][a-zA-Z_$0-9]*\.)+[a-zA-Z_$][a-zA-Z_$0-9]*): ([^\n]*)\n\tat ((?:[a-zA-Z_$][a-zA-Z_$0-9]*\.)*)([a-zA-Z_$][a-zA-Z_$0-9]*)\.([a-zA-Z_$][a-zA-Z_$0-9]*|<\w+>)\(([a-zA-Z_$][a-zA-Z_$0-9]*\.[a-zA-Z]+(?::\d+)?|<\w+>|Native Method|Unknown Source)\)/
    );

    private static EX_I_PACKAGE = 1;
    private static EX_I_CLASS = 2;
    private static EX_I_METHOD = 3;

    private static EX_I_SUBMITTED_FROM = 4;
    private static EX_I_SOURCE = 5;
    private static EX_I_CAUSED_BY_CLASS = 6;
    private static EX_I_CAUSED_BY_MSG = 7;
    private static EX_I_FRAMES_COUNT = 8;

    private static exceptionItemRegexp: RegExp = new RegExp(
        /^\n(?:(?:\tat (?:((?:[a-zA-Z_$][a-zA-Z_$0-9]*\.)*)([a-zA-Z_$][a-zA-Z_$0-9]*)\.([a-zA-Z_$][a-zA-Z_$0-9]*|<\w+>)|(-+ submitted from -+)\.)\(([a-zA-Z_$][a-zA-Z_$0-9]*\.[a-zA-Z]+(?::\d+)?|<\w+>|Native Method|Unknown Source)\))|(?:Caused by: ((?:[a-zA-Z_$][a-zA-Z_$0-9]*\.)+[a-zA-Z_$][a-zA-Z_$0-9]*): ([^\n]*))|\t... (\d+) common frames omitted)/
    );

    private homePackages: string[];

    constructor(args: {homePackages: string[]}) {
        this.homePackages = args.homePackages || [];

        for (let i = 0; i < this.homePackages.length; i++) {
            if (!this.homePackages[i].endsWith('.')) { this.homePackages[i] += '.'; }
        }
    }

    tryRender(s: string, textRenderer: Text2HtmlConverter): Highlight[] {
        let res = JavaExceptionRenderer.exceptionRegexp.exec(s);

        if (res) {
            let e = document.createElement('SPAN');
            e.className = 'ex-wrapper';

            let img: HTMLImageElement = <HTMLImageElement>(
                document.createElement('IMG')
            );
            img.src = 'assets/java-exception.png';
            (<SlElement>(<any>img)).virtual = true;
            e.appendChild(img);

            let exClass = document.createElement('SPAN');
            exClass.className = 'exception-class';
            exClass.innerText = res[1];

            e.appendChild(exClass);

            e.appendChild(document.createTextNode(': '));

            let exMessage = document.createElement('SPAN');
            exMessage.className = exceptionMessageClass;
            textRenderer(exMessage, 'text', res[2]);
            
            e.appendChild(exMessage);

            let traceItems = [[null, res[3], res[4], res[5], null, res[6]]];

            let idx = res.index + res[0].length;

            while (true) {
                let itemRes = JavaExceptionRenderer.exceptionItemRegexp.exec(
                    s.substring(idx)
                );

                if (itemRes) {
                    if (itemRes[JavaExceptionRenderer.EX_I_SOURCE]) {
                        traceItems.push(itemRes);
                    } else if (itemRes[JavaExceptionRenderer.EX_I_CAUSED_BY_CLASS]) {
                        this.appendStacktraceItems(e, traceItems, true);
                        traceItems.length = 0;

                        e.appendChild(document.createTextNode('\nCaused by: '));

                        let exCauseClass = document.createElement('SPAN');
                        exCauseClass.className = 'exception-class';
                        exCauseClass.innerText =
                            itemRes[JavaExceptionRenderer.EX_I_CAUSED_BY_CLASS];

                        e.appendChild(exCauseClass);

                        e.appendChild(document.createTextNode(': '));

                        let exMessage = document.createElement('SPAN');
                        exMessage.className = exceptionMessageClass;
                        textRenderer(exMessage, 'text', itemRes[JavaExceptionRenderer.EX_I_CAUSED_BY_MSG]);

                        e.appendChild(exMessage);
                    } else {
                        // text like "... 83 common frames omitted"
                        this.appendStacktraceItems(e, traceItems, false);
                        traceItems.length = 0;

                        let val = itemRes[0];
                        SlUtils.assert(val.startsWith('\n'));

                        e.appendChild(document.createTextNode('\n'));

                        let dots = document.createElement('DIV');
                        dots.style.whiteSpace = 'pre-wrap';
                        dots.innerText = itemRes[0].substr(1);

                        e.appendChild(dots);
                    }

                    idx += itemRes[0].length;
                } else {
                    break;
                }
            }

            if (
                idx < s.length &&
                !(idx === s.length - 1 && s[idx] === '\n') // See ExceptionRendererTest.exceptionWithLineEnd()
            ) {
                return null;
            }

            this.appendStacktraceItems(e, traceItems, true);

            return [{start: res.index, end: idx, e}];
        }

        return null;
    }

    private appendStacktraceItems(e: HTMLElement,
                                  m: string[][],
                                  bigExpanderAtEnd: boolean) {
        if (m.length === 0) { return; }

        let nonHomeLines: string[][] = [];

        for (let line of m) {
            if (this.isHomePackage(line[JavaExceptionRenderer.EX_I_PACKAGE])) {
                if (nonHomeLines.length <= 3) {
                    for (let l of nonHomeLines) { this.appendStacktraceItem(e, l); }
                } else {
                    this.appendStacktraceItem(e, nonHomeLines[0]);
                    this.appendCollapsableStackTrace(
                        e,
                        nonHomeLines,
                        1,
                        nonHomeLines.length - 1,
                        false
                    );
                    this.appendStacktraceItem(e, nonHomeLines[nonHomeLines.length - 1]);
                }

                nonHomeLines.length = 0;

                this.appendStacktraceItem(e, line);
            } else {
                nonHomeLines.push(line);
            }
        }

        if (nonHomeLines.length > 0) {
            this.appendStacktraceItem(e, nonHomeLines[0]);

            if (nonHomeLines.length > 1) {
                let i = 1;

                if (nonHomeLines.length === m.length) {
                    // All lines are non-home
                    this.appendStacktraceItem(e, nonHomeLines[1]);
                    i = 2;
                }

                if (nonHomeLines.length - i > (bigExpanderAtEnd ? 2 : 1)) {
                    this.appendCollapsableStackTrace(
                        e,
                        nonHomeLines,
                        i,
                        nonHomeLines.length,
                        bigExpanderAtEnd
                    );
                } else {
                    for (; i < nonHomeLines.length; i++) {
                        this.appendStacktraceItem(e, nonHomeLines[i]);
                    }
                }
            }
        }
    }

    private appendCollapsableStackTrace(e: HTMLElement,
                                        m: string[][],
                                        start: number,
                                        end: number,
                                        bigExpander: boolean) {
        if (start === end) { return; }

        SlUtils.assert(start < end);

        let collWrapper = document.createElement('DIV');
        collWrapper.className = 'coll-wrapper collapsed';

        if (bigExpander) {
            let expanderDown = document.createElement('DIV');
            expanderDown.className = 'ex-coll-expander-down coll-expander';
            expanderDown.title = 'Expand ' + (end - start) + ' hidden elements';
            (<SlElement>expanderDown).virtual = true;
            collWrapper.appendChild(expanderDown);
        } else {
            let expanderWrapper = document.createElement('DIV');
            expanderWrapper.className = 'ex-coll-plus-wrapper';
            (<SlElement>expanderWrapper).virtual = true;

            let expander = document.createElement('DIV');
            expander.className = 'coll-expander';
            expander.title = 'Expand ' + (end - start) + ' hidden elements';

            expanderWrapper.appendChild(expander);

            collWrapper.appendChild(expanderWrapper);
        }

        let collBody = document.createElement('DIV');
        collBody.className = 'coll-body coll-body-1px ex-coll-body';

        let collapser = document.createElement('DIV');
        collapser.className = 'coll-collapser';

        let collapserTop = document.createElement('DIV');
        collapserTop.className = 'top';
        collapser.appendChild(collapserTop);

        let collapserMiddle = document.createElement('DIV');
        collapserMiddle.className = 'middle';
        collapser.appendChild(collapserMiddle);

        let collapserBottom = document.createElement('DIV');
        collapserBottom.className = 'down';
        collapser.appendChild(collapserBottom);

        collBody.appendChild(collapser);

        for (let i = start; i < end; i++) {
            this.appendStacktraceItem(collBody, m[i]);
        }

        collWrapper.appendChild(collBody);

        e.appendChild(collWrapper);
    }

    private appendStacktraceItem(e: HTMLElement, m: string[]) {
        e.appendChild(document.createTextNode('\n'));

        let line = document.createElement('DIV');
        line.className = 'ex-stacktrace-line';

        line.appendChild(document.createTextNode('\tat '));

        let methodName;

        if (m[JavaExceptionRenderer.EX_I_SUBMITTED_FROM] != null) {
            line.appendChild(
                document.createTextNode(m[JavaExceptionRenderer.EX_I_SUBMITTED_FROM])
            );
            methodName = '';
        } else {
            let packageName = m[JavaExceptionRenderer.EX_I_PACKAGE];

            if (packageName) {
                SlUtils.assert(packageName.endsWith('.'));

                let packageSpan = document.createElement('SPAN');
                packageSpan.innerText = packageName.substring(
                    0,
                    packageName.length - 1
                );
                packageSpan.classList.add('ex-stacktrace-package');
                if (this.isHomePackage(packageName)) {
                    packageSpan.classList.add('ex-stacktrace-package-home');
                }

                line.appendChild(packageSpan);

                let dotBeforeClassSpan = document.createElement('SPAN');
                dotBeforeClassSpan.innerText = '.';
                dotBeforeClassSpan.className = 'dot-before-class';
                line.appendChild(dotBeforeClassSpan);
            }

            let classSpan = document.createElement('SPAN');
            classSpan.innerText = m[JavaExceptionRenderer.EX_I_CLASS];
            classSpan.className = 'ex-stacktrace-class';

            line.appendChild(classSpan);

            methodName = m[JavaExceptionRenderer.EX_I_METHOD];
        }

        let methodSpan = document.createElement('SPAN');
        methodSpan.innerText = '.' + methodName + '(';
        methodSpan.className = 'ex-stacktrace-method';

        line.appendChild(methodSpan);

        let srcSpan = document.createElement('SPAN');
        srcSpan.innerText = m[JavaExceptionRenderer.EX_I_SOURCE];
        srcSpan.className = 'ex-stacktrace-source';

        line.appendChild(srcSpan);

        let finalBracketSpan = document.createElement('SPAN');
        finalBracketSpan.innerText = ')';
        finalBracketSpan.className = 'ex-stacktrace-method';

        line.appendChild(finalBracketSpan);

        e.appendChild(line);
    }

    private isHomePackage(pkg: string): boolean {
        if (!pkg) { return false; }

        for (let p of this.homePackages) {
            if (pkg.startsWith(p)) { return true; }
        }

        return false;
    }

    private static stackTraceItemLength(m: string[][], idx: number): number {
        if (idx < 0 || idx >= m.length) { return 0; }

        let item = m[idx];

        return (
            4 /* \tat */ +
            (item[1] ? item[1].length : 0) /*package*/ +
            item[2].length /* className */ +
            1 +
            item[3].length +
            1 +
            item[4].length +
            1
        );
    }
}
