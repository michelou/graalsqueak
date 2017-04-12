package de.hpi.swa.trufflesqueak;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Vector;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.EmptyObject;
import de.hpi.swa.trufflesqueak.model.ImmediateCharacter;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.SmallInteger;
import de.hpi.swa.trufflesqueak.model.SqueakObject;

public class Chunk {
    SqueakObject object;

    private BaseSqueakObject sqClass;
    private BaseSqueakObject[] pointers;

    final int classid;
    final int pos;

    private final long size;
    private final ImageReader reader;
    private final int format;
    private final int hash;
    private final Vector<Integer> data;

    Chunk(ImageReader reader, long size, int format, int classid, int hash, int pos) {
        this.reader = reader;
        this.size = size;
        this.format = format;
        this.classid = classid;
        this.hash = hash;
        this.pos = pos;
        this.data = new Vector<>();
    }

    public void append(int nextInt) {
        data.add(nextInt);
    }

    public long size() {
        return data.size();
    }

    public void removeLast() {
        data.remove(data.size() - 1);
    }

    public Vector<Integer> data() {
        return data;
    }

    public SqueakObject asObject() {
        if (object == null) {
            if (format == 0) {
                // no fields
                object = new EmptyObject();
            } else if (format == 1) {
                // fixed pointers
                object = new PointersObject();
            } else if (format == 2) {
                // indexable fields
                object = new ListObject();
            } else if (format == 3) {
                // fixed and indexable fields
                object = new ListObject();
            } else if (format == 4) {
                // indexable weak fields // TODO: Weak
                object = new ListObject();
            } else if (format == 5) {
                // fixed weak fields // TODO: Weak
                object = new PointersObject();
            } else if (format <= 8) {
                assert false; // unused
            } else if (format == 9) {
                // 64-bit integers
                object = new NativeObject();
            } else if (format <= 11) {
                // 32-bit integers
                object = new NativeObject();
            } else if (format <= 15) {
                // 16-bit integers
                object = new NativeObject();
            } else if (format <= 23) {
                // bytes
                object = new NativeObject();
            } else if (format <= 31) {
                // compiled methods
                object = new CompiledMethodObject();
            }
        }
        return object;
    }

    public long getSize() {
        return size;
    }

    public int getHash() {
        return hash;
    }

    public BaseSqueakObject getSqClass() {
        return sqClass;
    }

    public void setSqClass(BaseSqueakObject baseSqueakObject) {
        this.sqClass = baseSqueakObject;
    }

    public BaseSqueakObject[] getPointers() {
        return getPointers(data.size());
    }

    public BaseSqueakObject[] getPointers(int end) {
        if (pointers == null) {
            pointers = new BaseSqueakObject[end];
            for (int i = 0; i < end; i++) {
                pointers[i] = decodePointer(data.get(i));
            }
        }
        return pointers;
    }

    private BaseSqueakObject decodePointer(int ptr) {
        if ((ptr & 3) == 0) {
            SqueakObject obj = reader.chunktable.get(ptr).asObject();
            if (obj == null) {
                System.err.println("Bogus pointer: " + ptr + ". Treating as smallint.");
                return new SmallInteger(ptr >> 1);
            } else {
                return obj;
            }
        } else if ((ptr & 1) == 1) {
            return new SmallInteger(ptr >> 1);
        } else {
            assert ((ptr & 3) == 2);
            return new ImmediateCharacter(ptr >> 2);
        }
    }

    public byte[] getBytes() {
        return getBytes(0);
    }

    public byte[] getBytes(int start) {
        List<Integer> subList = data.subList(start, data.size());
        ByteBuffer buf = ByteBuffer.allocate(subList.size() * 4);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        IntBuffer intBuf = buf.asIntBuffer();
        for (int i : subList) {
            intBuf.put(i);
        }
        return buf.array();
    }

    public int getPadding() {
        // TODO Auto-generated method stub
        if ((16 <= format) && (format <= 23)) {
            return format & 3;
        } else {
            return 0;
        }
    }
}