package de.hpi.swa.trufflesqueak.nodes.bytecodes.store;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.stack.TopStackNode;

public class StoreIntoAssociationNode extends AbstractStoreIntoAssociationNode {
    @Child private TopStackNode topNode;

    public StoreIntoAssociationNode(CompiledCodeObject code, int index, int numBytecodes, int variableIndex) {
        super(code, index, numBytecodes, variableIndex);
        topNode = new TopStackNode(code);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        storeNode.executeWrite(frame, topNode.execute(frame));
    }

    @Override
    public String toString() {
        return "storeIntoLit: " + variableIndex;
    }
}
