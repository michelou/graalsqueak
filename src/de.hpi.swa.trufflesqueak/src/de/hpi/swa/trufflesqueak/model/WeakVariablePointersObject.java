/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.lang.ref.ReferenceQueue;
import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.utilities.TruffleWeakReference;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayout;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectIdentityNode;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public final class WeakVariablePointersObject extends AbstractPointersObject {
    private Object[] variablePart;
    private final ReferenceQueue<Object> weakPointersQueue;

    public WeakVariablePointersObject(final SqueakImageContext image, final long hash, final ClassObject classObject) {
        super(image, hash, classObject);
        weakPointersQueue = image.weakPointersQueue;
    }

    public WeakVariablePointersObject(final SqueakImageContext image, final ClassObject classObject, final ObjectLayout layout, final int variableSize) {
        super(image, classObject, layout);
        variablePart = new Object[variableSize];
        Arrays.fill(variablePart, NilObject.SINGLETON);
        weakPointersQueue = image.weakPointersQueue;
    }

    public WeakVariablePointersObject(final SqueakImageContext image, final ClassObject classObject, final int variableSize) {
        super(image, classObject);
        variablePart = new Object[variableSize];
        Arrays.fill(variablePart, NilObject.SINGLETON);
        weakPointersQueue = image.weakPointersQueue;
    }

    private WeakVariablePointersObject(final WeakVariablePointersObject original) {
        super(original);
        variablePart = original.variablePart.clone();
        weakPointersQueue = original.weakPointersQueue;
    }

    @Override
    protected void fillInVariablePart(final Object[] pointers, final int instSize) {
        variablePart = Arrays.copyOfRange(pointers, instSize, pointers.length);
        for (int i = 0; i < variablePart.length; i++) {
            final Object value = variablePart[i];
            if (value instanceof AbstractSqueakObject) {
                variablePart[i] = new TruffleWeakReference<>(value, weakPointersQueue);
            }
        }
    }

    public void become(final WeakVariablePointersObject other) {
        becomeLayout(other);
        final Object[] otherVariablePart = other.variablePart;
        other.variablePart = variablePart;
        variablePart = otherVariablePart;
    }

    @Override
    public int size() {
        return instsize() + variablePart.length;
    }

    public Object[] getVariablePart() {
        return variablePart;
    }

    public int getVariablePartSize() {
        return variablePart.length;
    }

    private Object getFromVariablePart(final int index) {
        final Object value = UnsafeUtils.getObject(variablePart, index);
        if (value instanceof TruffleWeakReference) {
            return NilObject.nullToNil(((TruffleWeakReference<?>) value).get());
        } else {
            assert value != null;
            return value;
        }
    }

    public Object getFromVariablePart(final int index, final ConditionProfile weakRefProfile) {
        final Object value = UnsafeUtils.getObject(variablePart, index);
        if (weakRefProfile.profile(value instanceof TruffleWeakReference)) {
            return NilObject.nullToNil(((TruffleWeakReference<?>) value).get());
        } else {
            assert value != null;
            return value;
        }
    }

    private void putIntoVariablePartSlow(final int index, final Object value) {
        putIntoVariablePart(index, value, ConditionProfile.getUncached());
    }

    public void putIntoVariablePart(final int index, final Object value, final ConditionProfile profile) {
        UnsafeUtils.putObject(variablePart, index, profile.profile(value instanceof AbstractSqueakObject) ? new TruffleWeakReference<>(value, weakPointersQueue) : value);
    }

    public boolean pointsTo(final SqueakObjectIdentityNode identityNode, final Object thang) {
        return layoutValuesPointTo(identityNode, thang) || variablePartPointsTo(thang);
    }

    private boolean variablePartPointsTo(final Object thang) {
        for (final Object value : variablePart) {
            if (value == thang || value instanceof TruffleWeakReference && ((TruffleWeakReference<?>) value).get() == thang) {
                return true;
            }
        }
        return false;
    }

    public WeakVariablePointersObject shallowCopy() {
        return new WeakVariablePointersObject(this);
    }

    @Override
    public void pointersBecomeOneWay(final Object[] from, final Object[] to) {
        layoutValuesBecomeOneWay(from, to);
        final int variableSize = variablePart.length;
        if (variableSize > 0) {
            for (int i = 0; i < from.length; i++) {
                final Object fromPointer = from[i];
                for (int j = 0; j < variableSize; j++) {
                    final Object object = getFromVariablePart(j);
                    if (object == fromPointer) {
                        putIntoVariablePartSlow(j, to[i]);
                    }
                }
            }
        }
    }

    @Override
    protected void traceVariablePart(final ObjectTracer tracer) {
        /* Weak pointers excluded from tracing. */
    }

    @Override
    protected void traceVariablePart(final SqueakImageWriter tracer) {
        /* Weak pointers excluded from tracing. */
    }

    @Override
    protected void writeVariablePart(final SqueakImageWriter writer) {
        for (int i = 0; i < variablePart.length; i++) {
            /*
             * Since weak pointers are excluded from tracing, ignore (replace with nil) all objects
             * that have not been traced somewhere else.
             */
            writer.writeObjectIfTracedElseNil(getFromVariablePart(i));
        }
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "a " + getSqueakClassName() + " @" + Integer.toHexString(hashCode()) + " of size " + variablePart.length;
    }
}
