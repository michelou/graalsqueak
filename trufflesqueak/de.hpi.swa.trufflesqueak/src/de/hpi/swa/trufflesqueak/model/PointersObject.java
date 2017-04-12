package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.trufflesqueak.Chunk;
import de.hpi.swa.trufflesqueak.exceptions.InvalidIndex;

public class PointersObject extends SqueakObject implements TruffleObject {
    private BaseSqueakObject[] pointers;

    public ForeignAccess getForeignAccess() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void fillin(Chunk chunk) {
        super.fillin(chunk);
        pointers = chunk.getPointers();
    }

    public BaseSqueakObject at0(int i) throws InvalidIndex {
        if (i < pointers.length) {
            return pointers[i];
        }
        throw new InvalidIndex();
    }

    public void atput0(int i, BaseSqueakObject obj) throws InvalidIndex {
        if (i < pointers.length) {
            pointers[i] = obj;
            return;
        }
        throw new InvalidIndex();
    }
}
