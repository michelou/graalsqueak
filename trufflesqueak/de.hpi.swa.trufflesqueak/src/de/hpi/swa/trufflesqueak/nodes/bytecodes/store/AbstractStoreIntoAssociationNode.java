package de.hpi.swa.trufflesqueak.nodes.bytecodes.store;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.MethodLiteralNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.AbstractStackNode;
import de.hpi.swa.trufflesqueak.util.KnownClasses.ASSOCIATION;

public abstract class AbstractStoreIntoAssociationNode extends AbstractBytecodeNode {
    @Child ObjectAtPutNode storeNode;
    protected final int variableIndex;

    public AbstractStoreIntoAssociationNode(CompiledCodeObject code, int index, int numBytecodes, int variableIndex) {
        super(code, index, numBytecodes);
        this.variableIndex = variableIndex;
        storeNode = ObjectAtPutNode.create(ASSOCIATION.VALUE, new MethodLiteralNode(code, variableIndex), getValueNode());
    }

    protected abstract AbstractStackNode getValueNode();

}