package de.hpi.swa.trufflesqueak.nodes.primitives;

import java.lang.reflect.InvocationTargetException;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

@NodeChildren({@NodeChild(value = "receiver", type = SqueakNode.class),
                @NodeChild(value = "argument", type = SqueakNode.class)})
public abstract class PrimitiveBinaryOperation extends PrimitiveNode {
    public PrimitiveBinaryOperation(CompiledMethodObject cm) {
        super(cm);
    }

    public static PrimitiveNode createInstance(Class<? extends PrimitiveNode> cls, CompiledMethodObject cm)
                    throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        return (PrimitiveNode) cls.getMethod("create", CompiledMethodObject.class, SqueakNode.class, SqueakNode.class).invoke(cls, cm, arg(0), arg(1));
    }
}