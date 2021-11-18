import {Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {LogNavigatorComponent, OpenEvent} from '@app/log-navigator/log-navigator.component';
import {LvUtils} from "@app/utils/utils";

@Component({
    selector: 'lv-main-page',
    templateUrl: './main-page.template.html',
    styleUrls: ['./main-page.style.scss'],
})
export class MainPageComponent implements OnInit {

    @ViewChild('navigator', {read: LogNavigatorComponent, static: true})
    private navigatorComponent: LogNavigatorComponent;

    initialPath: string;

    constructor(private router: Router, private route: ActivatedRoute) {

    }

    open(event: OpenEvent) {
        if (event.isCtrlClick) {
            window.open('log?path=' + encodeURI(event.path));
        } else {
            this.router.navigate(['/log'], {
                queryParams: {path: event.path}
            });
        }
    }

    ngOnInit(): void {
        this.initialPath = LvUtils.lastParam(this.route.snapshot.queryParams.dir);
    }

    directoryChanged(dir: string) {
        let queryParams: any = {};

        if (dir != this.navigatorComponent.defaultDir) {
            queryParams.dir = dir;
        }
        
        this.router.navigate([], {queryParams});
    }
}
