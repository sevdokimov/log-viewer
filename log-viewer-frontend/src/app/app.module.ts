import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {AppRootComponent} from './app-root.component';
import {RouterModule, Routes} from '@angular/router';
import {LogNavigatorComponent} from './log-navigator/log-navigator.component';
import {LogViewComponent} from './log-view/log-view.component';
import {FormsModule} from '@angular/forms';
import {SlDurationPipe} from './utils/sl-duration.pipe';
import {SlSizePipe} from './utils/sl-size.pipe';
import {AceEditorDirective} from './utils/ace-editor.directive';
import {FavoritesService} from './services/favorites.service';
import {LogListPanelComponent} from './log-view/log-list-panel.component';
import {ModalModule} from 'ngx-bootstrap';
import {BsDropdownModule} from 'ngx-bootstrap/dropdown';
import {ToastrModule} from 'ngx-toastr';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {HttpClientModule} from '@angular/common/http';
import {TopFilterListComponent} from './log-view/top-filters/top-filter-list/top-filter-list.component';
import {LevelListComponent} from './log-view/top-filters/level-list/level-list.component';
import {ExceptionOnlyComponent} from '@app/log-view/top-filters/exception-only/exception-only.component';
import {EventDetailsComponent} from '@app/log-view/event-details/event-details.component';
import {FileStatusComponent} from '@app/log-view/file-status/file-status.component';
import {ContextMenuModule} from 'ngx-contextmenu';
import {LvDateIntervalComponent} from '@app/log-view/top-filters/date-interval/date-interval.component';
import {NgxMatDatetimePickerModule, NgxMatTimepickerModule} from '@angular-material-components/datetime-picker';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatDatepickerModule} from '@angular/material/datepicker';
import {MatInputModule} from '@angular/material/input';
import {MatCommonModule} from '@angular/material/core';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {NgxMomentDateModule} from '@angular-material-components/moment-adapter';
import {MatMenuModule} from '@angular/material/menu';
import {LvThreadFilterComponent} from '@app/log-view/top-filters/thread-filter/thread-filter.component';
import {LvJsFilterComponent} from '@app/log-view/top-filters/js-filter/js-filter.component';
import {LvTextFilterComponent} from '@app/log-view/top-filters/text-filter/text-filter.component';
import {AutosizeModule} from 'ngx-autosize';
import {MainPageComponent} from '@app/main-page/main-page.component';
import {SwitcherComponent} from '@app/switcher/switcher.component';
import {DownloadDialogComponent} from "@app/log-view/download-dialog/download-dialog.component";

export const appRoutes: Routes = [
    {
        path: '',
        component: MainPageComponent,
        data: {title: 'File system'},
    },
    {path: 'log', component: LogViewComponent, data: {title: 'Log view'}},
];

@NgModule({
    imports: [
        BrowserModule,
        BrowserAnimationsModule,
        HttpClientModule,
        BsDropdownModule.forRoot(),
        ModalModule.forRoot(),
        FormsModule,
        ToastrModule.forRoot(),
        ContextMenuModule.forRoot({
            useBootstrap4: true,
        }),

        AutosizeModule,

        RouterModule.forRoot(appRoutes, { relativeLinkResolution: 'legacy' }),

        NgxMatDatetimePickerModule,
        NgxMatTimepickerModule,
        NgxMomentDateModule,

        MatFormFieldModule,
        MatDatepickerModule,
        MatButtonModule,
        MatInputModule,
        MatIconModule,
        MatCommonModule,
        MatMenuModule,
    ],
    declarations: [
        // root
        AppRootComponent,

        LogNavigatorComponent,
        LogViewComponent,
        LogListPanelComponent,
        EventDetailsComponent,
        DownloadDialogComponent,
        FileStatusComponent,
        SwitcherComponent,

        MainPageComponent,

        AceEditorDirective,
        SlDurationPipe,
        SlSizePipe,
        TopFilterListComponent,
        LevelListComponent,
        ExceptionOnlyComponent,
        LvDateIntervalComponent,
        LvJsFilterComponent,
        LvTextFilterComponent,
        LvThreadFilterComponent,
    ],
    providers: [FavoritesService],

    exports: [SlDurationPipe, SlSizePipe],

    bootstrap: [AppRootComponent],
})
export class AppModule {
}
