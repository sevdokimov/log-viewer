package com.logviewer.web.session;

import com.logviewer.data2.Snapshot;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class Status implements Externalizable {

    private Throwable error;

    private String hash;

    private long size;

    private long lastModification;

    /**
     * Used by deserializer only.
     */
    public Status() {

    }

    public Status(Snapshot snapshot) {
        this.hash = snapshot.getHash();
        this.size = snapshot.getSize();
        this.lastModification = snapshot.getLastModification();
    }

    public Status(Throwable error) {
        this.error = error;
    }

    public Throwable getError() {
        return error;
    }

    public String getHash() {
        return hash;
    }

    public long getSize() {
        return size;
    }

    public long getLastModification() {
        return lastModification;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(error);
        if (error == null) {
            out.writeUTF(hash);
            out.writeLong(size);
            out.writeLong(lastModification);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        error = (Throwable) in.readObject();
        if (error == null) {
            hash = in.readUTF();
            size = in.readLong();
            lastModification = in.readLong();
        }
    }
}
