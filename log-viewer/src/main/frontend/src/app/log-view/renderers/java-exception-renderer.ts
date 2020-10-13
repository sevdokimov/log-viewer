import {Highlight, Text2HtmlConverter, TextRenderer} from './renderer';
import {SlElement} from '@app/utils/sl-element';
import {SlUtils} from '@app/utils/utils';

const exceptionMessageClass = 'exception-message';
const exceptionFileNameClass = 'ex-stacktrace-source';

const MAX_MESSAGE_LENGTH = 1024;

export class JavaExceptionRenderer implements TextRenderer {

    private rgxHeader: RegExp;
    private rgxCausedBy: RegExp;
    private rgxItem: RegExp;
    private rgxItemSearch: RegExp;
    private rgxEnd: RegExp = /\n\t\.\.\. (\d+) common frames omitted$/ymg;

    private static EX_I_CLASSLOADER = 1;
    private static EX_I_MODULE = 2;
    private static EX_I_PACKAGE = 3;
    private static EX_I_CLASS = 4;
    private static EX_I_METHOD = 5;
    private static EX_I_SUBMITTED_FROM = 6;
    private static EX_I_SOURCE = 7;
    private static EX_I_JARNAME = 8;

    private readonly homePackages: string[];

    constructor(args: {homePackages: string[]}) {
        this.homePackages = args.homePackages || [];

        for (let i = 0; i < this.homePackages.length; i++) {
            if (!this.homePackages[i].endsWith('.')) { this.homePackages[i] += '.'; }
        }

        let rgxIdent = '[a-zA-Z_$][a-zA-Z_$0-9]*';
        let rgxHeader = `((?:${rgxIdent}\\.)+${rgxIdent})(?:: ([^\n]*))?`;

        this.rgxHeader = new RegExp(`^${rgxHeader}$`, 'mg');
        this.rgxCausedBy = new RegExp(`\nCaused by: ${rgxHeader}$`, 'ymg');

        let rgxItem: string = '\n\tat ' +
            '(?:' +
            '([^/@\n]+/)??' +
            '([^/@\n]+(?:@[^/\n]+)?/)?' +
            `((?:${rgxIdent}\\.)+)?(${rgxIdent})\\.(${rgxIdent}|<\\w+>)` +
            '|' +
            '(-+ submitted from -+)\\.' +
            ')' +
            '\\(' +
            '(' +
            '[a-zA-Z_$][a-zA-Z_$0-9]*\\.[a-zA-Z]+(?::-?\\d{1,9})?|<\\w+>|Native Method|Unknown Source' +
            ')' +
            '\\)' +
            '( ~?\\[[^\\[\\]\\s]+:[^\\[\\]\\s]+\\])?' + // EX_I_JARNAME
            '$';

        this.rgxItem = new RegExp(rgxItem, 'ymg');
        this.rgxItemSearch = new RegExp(rgxItem, 'mg');
    }

    tryRender(s: string, textRenderer: Text2HtmlConverter): Highlight[] {
        this.rgxHeader.lastIndex = 0;

        let headerMatch = this.rgxHeader.exec(s);
        if (!headerMatch) { return null; }

        while (true) { // find last match
            let next = this.rgxHeader.exec(s);
            if (!next) { break; }
            headerMatch = next;
        }

        let e = document.createElement('SPAN');
        e.className = 'ex-wrapper';

        let img: HTMLImageElement = <HTMLImageElement>(document.createElement('IMG'));
        img.src = 'assets/java-exception.png';
        (<SlElement>(<any>img)).virtual = true;
        e.appendChild(img);

        let exClass = document.createElement('SPAN');
        exClass.className = 'exception-class';
        exClass.innerText = headerMatch[1];

        e.appendChild(exClass);

        let headerEnd = headerMatch.index + headerMatch[0].length;

        this.rgxItemSearch.lastIndex = headerEnd;
        let firstItem = this.rgxItemSearch.exec(s);
        if (!firstItem) { return null; }
        if (firstItem.index - headerEnd > MAX_MESSAGE_LENGTH) { return null; }

        if (headerMatch[2] != null) {
            e.appendChild(document.createTextNode(': '));

            let exMessage = document.createElement('SPAN');
            exMessage.className = exceptionMessageClass;
            textRenderer(exMessage, 'text/exception-message', headerMatch[2] + s.substring(headerEnd, firstItem.index));
            e.appendChild(exMessage);
        }

        let traceItems = [firstItem];

        let idx = this.rgxItemSearch.lastIndex;

        while (idx < s.length) {
            this.rgxItem.lastIndex = idx;
            let itemRes = this.rgxItem.exec(s);
            if (itemRes) {
                traceItems.push(itemRes);
            } else {
                this.rgxCausedBy.lastIndex = idx;
                itemRes = this.rgxCausedBy.exec(s);
                if (itemRes) {
                    this.appendStacktraceItems(e, traceItems, true);
                    traceItems.length = 0;

                    e.appendChild(document.createTextNode('\nCaused by: '));

                    let exCauseClass = document.createElement('SPAN');
                    exCauseClass.className = 'exception-class';
                    exCauseClass.innerText = itemRes[1];

                    e.appendChild(exCauseClass);

                    let message = itemRes[2];
                    if (message != null) {
                        e.appendChild(document.createTextNode(': '));

                        let exMessage = document.createElement('SPAN');
                        exMessage.className = exceptionMessageClass;
                        textRenderer(exMessage, 'text', message);
                        e.appendChild(exMessage);
                    }
                } else {
                    this.rgxEnd.lastIndex = idx;
                    itemRes = this.rgxEnd.exec(s);
                    if (itemRes) {
                        // text like "... 83 common frames omitted"
                        this.appendStacktraceItems(e, traceItems, false);
                        traceItems.length = 0;

                        e.appendChild(document.createTextNode('\n'));

                        let dots = document.createElement('DIV');
                        dots.style.whiteSpace = 'pre-wrap';
                        dots.innerText = itemRes[0].substr(1);

                        e.appendChild(dots);
                    } else {
                        break;
                    }
                }
            }

            idx += itemRes[0].length;
        }

        if (
            idx < s.length &&
            !(idx === s.length - 1 && s[idx] === '\n') // See ExceptionRendererTest.exceptionWithLineEnd()
        ) {
            return null;
        }

        this.appendStacktraceItems(e, traceItems, true);

        return [{start: headerMatch.index, end: idx, e}];
    }

    private appendStacktraceItems(e: HTMLElement, m: string[][], bigExpanderAtEnd: boolean) {
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
            line.appendChild(document.createTextNode(m[JavaExceptionRenderer.EX_I_SUBMITTED_FROM]));
            methodName = '';
        } else {
            if (m[JavaExceptionRenderer.EX_I_CLASSLOADER]) {
                let classLoaderSpan = document.createElement('SPAN');
                classLoaderSpan.innerText = m[JavaExceptionRenderer.EX_I_CLASSLOADER];
                classLoaderSpan.classList.add('ex-classloader');
                line.appendChild(classLoaderSpan);
            }
            if (m[JavaExceptionRenderer.EX_I_MODULE]) {
                let moduleSpan = document.createElement('SPAN');
                moduleSpan.innerText = m[JavaExceptionRenderer.EX_I_MODULE];
                moduleSpan.classList.add('ex-module');
                line.appendChild(moduleSpan);
            }

            let packageName = m[JavaExceptionRenderer.EX_I_PACKAGE];

            if (packageName) {
                SlUtils.assert(packageName.endsWith('.'));

                let packageSpan = document.createElement('SPAN');
                packageSpan.innerText = packageName.substring(0, packageName.length - 1);
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
        srcSpan.className = exceptionFileNameClass;

        line.appendChild(srcSpan);

        let finalBracketSpan = document.createElement('SPAN');
        finalBracketSpan.innerText = ')';
        finalBracketSpan.className = 'ex-stacktrace-method';

        line.appendChild(finalBracketSpan);

        if (m[JavaExceptionRenderer.EX_I_JARNAME]) {
            let jarnameSpan = document.createElement('SPAN');
            jarnameSpan.innerText = m[JavaExceptionRenderer.EX_I_JARNAME];
            jarnameSpan.className = exceptionFileNameClass;

            line.appendChild(jarnameSpan);
        }

        e.appendChild(line);
    }

    private isHomePackage(pkg: string): boolean {
        if (!pkg) { return false; }

        for (let p of this.homePackages) {
            if (pkg.startsWith(p)) { return true; }
        }

        return false;
    }
}
