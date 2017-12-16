package de.hpi.swa.trufflesqueak.nodes.bytecodes.store;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.stack.TopStackNode;

public class StoreIntoReceiverVariableNode extends AbstractStoreIntoReceiverVariableNode {
    @Child private TopStackNode topNode;

    public StoreIntoReceiverVariableNode(CompiledCodeObject code, int index, int numBytecodes, int receiverIndex) {
        super(code, index, numBytecodes, receiverIndex);
        topNode = new TopStackNode(code);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        storeNode.executeWrite(frame, topNode.execute(frame));
    }

    @Override
    public String toString() {
        return "storeIntoRcvr: " + receiverIndex;
    }
}
