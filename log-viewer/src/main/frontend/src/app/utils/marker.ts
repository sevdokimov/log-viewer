export class Marker {
    start: number;
    end: number;

    className: string;

    static equals(m1: Marker, m2: Marker) {
        return (
            m1.start === m2.start &&
            m1.end === m2.end &&
            m1.className === m2.className
        );
    }

    static equalsArray(m1: Marker[], m2: Marker[]): boolean {
        if (!m1 || m1.length === 0) {
            return !m2 || m2.length === 0;
        }

        if (!m2 || m2.length === 0) { return false; }

        if (m1.length !== m2.length) { return false; }

        for (let i = 0; i < m1.length; i++) {
            if (!Marker.equals(m1[i], m2[i])) { return false; }
        }

        return true;
    }
}
