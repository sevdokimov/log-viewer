export class PathUtils {
    static extractName(path: string) {
        let idx = path.lastIndexOf('/');
        return path.substr(idx + 1);
    }
}
