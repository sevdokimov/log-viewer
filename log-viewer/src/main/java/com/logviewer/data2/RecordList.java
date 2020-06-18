package com.logviewer.data2;

import com.logviewer.utils.Pair;

import javax.annotation.Nonnull;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;

/**
 * todo make externalizable and optimize
 */
public class RecordList extends ArrayList<Pair<Record, Throwable>> implements Externalizable {

    public RecordList() {

    }

    public RecordList(@Nonnull Pair<Record, Throwable> restRecord) {
        add(restRecord);
    }

    public RecordList(Collection<Pair<Record, Throwable>> queue) {
        super(queue);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(size());

        for (Pair<Record, Throwable> pair : this) {
            pair.getFirst().writeExternal(out);
            out.writeObject(pair.getSecond());
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int size = in.readInt();

        for (int i = 0; i < size; i++) {
            Record record = new Record();
            record.readExternal(in);
            Throwable t = (Throwable) in.readObject();

            add(Pair.of(record, t));
        }
    }
}
