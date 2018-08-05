package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;

public abstract class StackTopNode extends AbstractStackNode {

    public static StackTopNode create(final CompiledCodeObject code) {
        return StackTopNodeGen.create(code);
    }

    protected StackTopNode(final CompiledCodeObject code) {
        super(code);
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected final Object doTopVirtualized(final VirtualFrame frame) {
        return getReadNode().execute(frame, frameStackPointer(frame));
    }

    @Fallback
    protected final Object doTop(final VirtualFrame frame) {
        return getContext(frame).top();
    }
}
