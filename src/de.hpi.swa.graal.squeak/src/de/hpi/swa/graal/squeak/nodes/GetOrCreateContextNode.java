package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameArgumentNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

@ImportStatic(FrameAccess.class)
public abstract class GetOrCreateContextNode extends AbstractNodeWithCode {

    public static GetOrCreateContextNode create(final CompiledCodeObject code) {
        return GetOrCreateContextNodeGen.create(code);
    }

    protected GetOrCreateContextNode(final CompiledCodeObject code) {
        super(code);
    }

    public abstract ContextObject executeGet(Frame frame);

    @Specialization(guards = {"isFullyVirtualized(frame)"})
    protected final ContextObject doCreateLight(final VirtualFrame frame,
                    @Cached("create(METHOD)") final FrameArgumentNode methodNode) {
        final CompiledCodeObject method = (CompiledCodeObject) methodNode.executeRead(frame);
        final ContextObject context = ContextObject.create(method.image, method.sqContextSize(), frame.materialize(), getFrameMarker(frame));
        frame.setObject(code.thisContextOrMarkerSlot, context);
        return context;
    }

    @Fallback
    protected final ContextObject doGet(final VirtualFrame frame) {
        return getContext(frame);
    }

    public static final ContextObject getOrCreateFull(final MaterializedFrame frame) {
        final Object contextOrMarker = FrameAccess.getContextOrMarker(frame);
        final ContextObject context;
        final CompiledCodeObject method;
        if (contextOrMarker instanceof ContextObject) {
            context = (ContextObject) contextOrMarker;
            method = context.getMethod();
        } else {
            method = (CompiledCodeObject) frame.getArguments()[FrameAccess.METHOD];
            context = ContextObject.create(method.image, method.sqContextSize(), frame, (FrameMarker) contextOrMarker);
            frame.setObject(method.thisContextOrMarkerSlot, context);
        }
        return context;
    }
}
