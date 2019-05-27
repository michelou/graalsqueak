package de.hpi.swa.graal.squeak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.EnterCodeNode;
import de.hpi.swa.graal.squeak.nodes.GetOrCreateContextNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectLibrary;
import de.hpi.swa.graal.squeak.nodes.bytecodes.PushBytecodesFactory.PushLiteralVariableNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.PushBytecodesFactory.PushNewArrayNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.PushBytecodesFactory.PushReceiverNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.PushBytecodesFactory.PushReceiverVariableNodeGen;
import de.hpi.swa.graal.squeak.nodes.bytecodes.PushBytecodesFactory.PushRemoteTempNodeGen;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackReadAndClearNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackWriteNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class PushBytecodes {

    private abstract static class AbstractPushNode extends AbstractBytecodeNode {
        @Child protected FrameStackWriteNode pushNode;

        protected AbstractPushNode(final CompiledCodeObject code, final int index) {
            this(code, index, 1);
        }

        protected AbstractPushNode(final CompiledCodeObject code, final int index, final int numBytecodes) {
            super(code, index, numBytecodes);
            pushNode = FrameStackWriteNode.create(code);
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static final class PushActiveContextNode extends AbstractPushNode {
        @Child private GetOrCreateContextNode getContextNode;

        public PushActiveContextNode(final CompiledCodeObject code, final int index) {
            super(code, index);
            getContextNode = GetOrCreateContextNode.create(code);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executePush(frame, getContextNode.executeGet(frame));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushThisContext:";
        }
    }

    public static final class PushClosureNode extends AbstractPushNode {
        private final int blockSize;
        private final int numArgs;
        private final int numCopied;

        @Child private FrameStackReadAndClearNode popNNode;
        @Child private GetOrCreateContextNode getOrCreateContextNode;

        @CompilationFinal private CompiledBlockObject block;
        @CompilationFinal private RootCallTarget blockCallTarget;

        private PushClosureNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int i, final int j, final int k) {
            super(code, index, numBytecodes);
            numArgs = i & 0xF;
            numCopied = i >> 4 & 0xF;
            blockSize = j << 8 | k;
            popNNode = FrameStackReadAndClearNode.create(code);
            getOrCreateContextNode = GetOrCreateContextNode.create(code);
        }

        public static PushClosureNode create(final CompiledCodeObject code, final int index, final int numBytecodes, final int i, final int j, final int k) {
            return new PushClosureNode(code, index, numBytecodes, i, j, k);
        }

        private CompiledBlockObject getBlock(final VirtualFrame frame) {
            if (block == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                block = CompiledBlockObject.create(code, FrameAccess.getMethod(frame), numArgs, numCopied, index + numBytecodes, blockSize);
                blockCallTarget = Truffle.getRuntime().createCallTarget(EnterCodeNode.create(block.image.getLanguage(), block));
            }
            return block;
        }

        public int getBockSize() {
            return blockSize;
        }

        public int getClosureSuccessorIndex() {
            return getSuccessorIndex() + getBockSize();
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executePush(frame, createClosure(frame));
        }

        private BlockClosureObject createClosure(final VirtualFrame frame) {
            final Object receiver = FrameAccess.getReceiver(frame);
            final Object[] copiedValues = popNNode.executePopN(frame, numCopied);
            final ContextObject outerContext = getOrCreateContextNode.executeGet(frame);
            return new BlockClosureObject(getBlock(frame), blockCallTarget, receiver, copiedValues, outerContext);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            final int start = index + numBytecodes;
            final int end = start + blockSize;
            return "closureNumCopied: " + numCopied + " numArgs: " + numArgs + " bytes " + start + " to " + end;
        }
    }

    public static final class PushConstantNode extends AbstractPushNode {
        private final Object constant;

        public PushConstantNode(final CompiledCodeObject code, final int index, final Object obj) {
            super(code, index);
            constant = obj;
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executePush(frame, constant);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushConstant: " + constant.toString();
        }
    }

    public static final class PushLiteralConstantNode extends AbstractPushNode {
        private final int literalIndex;

        public PushLiteralConstantNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex) {
            super(code, index, numBytecodes);
            this.literalIndex = literalIndex;
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executePush(frame, code.getLiteral(literalIndex));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushConstant: " + code.getLiteral(literalIndex).toString();
        }
    }

    public abstract static class PushLiteralVariableNode extends AbstractPushNode {
        private final int literalIndex;

        protected PushLiteralVariableNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex) {
            super(code, index, numBytecodes);
            this.literalIndex = literalIndex;
        }

        public static PushLiteralVariableNode create(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex) {
            return PushLiteralVariableNodeGen.create(code, index, numBytecodes, literalIndex);
        }

        @Specialization
        protected final void doPush(final VirtualFrame frame,
                        @CachedLibrary(limit = "3") final SqueakObjectLibrary objectLibary) {
            pushNode.executePush(frame, objectLibary.at0(code.getLiteral(literalIndex), 1));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushLit: " + literalIndex;
        }
    }

    public abstract static class PushNewArrayNode extends AbstractPushNode {
        @Child protected FrameStackReadAndClearNode popNNode;
        private final int arraySize;

        protected PushNewArrayNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int param) {
            super(code, index, numBytecodes);
            arraySize = param & 127;
            popNNode = param > 127 ? FrameStackReadAndClearNode.create(code) : null;
        }

        public static PushNewArrayNode create(final CompiledCodeObject code, final int index, final int numBytecodes, final int param) {
            return PushNewArrayNodeGen.create(code, index, numBytecodes, param);
        }

        @Specialization(guards = {"popNNode != null"})
        protected final void doPushArray(final VirtualFrame frame) {
            pushNode.executePush(frame, code.image.asArrayOfObjects(popNNode.executePopN(frame, arraySize)));
        }

        @Specialization(guards = {"popNNode == null"})
        protected final void doPushNewArray(final VirtualFrame frame) {
            /**
             * Pushing an ArrayObject with object strategy. Contents likely to be mixed values and
             * therefore unlikely to benefit from storage strategy.
             */
            pushNode.executePush(frame, ArrayObject.createObjectStrategy(code.image, code.image.arrayClass, arraySize));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "push: (Array new: " + arraySize + ")";
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public abstract static class PushReceiverNode extends AbstractPushNode {

        protected PushReceiverNode(final CompiledCodeObject code, final int index) {
            super(code, index);
        }

        public static PushReceiverNode create(final CompiledCodeObject code, final int index) {
            return PushReceiverNodeGen.create(code, index);
        }

        @Specialization
        protected final void doReceiverVirtualized(final VirtualFrame frame) {
            pushNode.executePush(frame, FrameAccess.getReceiver(frame));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();

            return "self";
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public abstract static class PushReceiverVariableNode extends AbstractPushNode {
        private final int variableIndex;

        protected PushReceiverVariableNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int varIndex) {
            super(code, index, numBytecodes);
            variableIndex = varIndex;
        }

        public static PushReceiverVariableNode create(final CompiledCodeObject code, final int index, final int numBytecodes, final int varIndex) {
            return PushReceiverVariableNodeGen.create(code, index, numBytecodes, varIndex);
        }

        @Specialization
        protected final void doReceiverVirtualized(final VirtualFrame frame,
                        @CachedLibrary(limit = "3") final SqueakObjectLibrary objectLibary) {
            pushNode.executePush(frame, objectLibary.at0(FrameAccess.getReceiver(frame), variableIndex));
        }

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushRcvr: " + variableIndex;
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public abstract static class PushRemoteTempNode extends AbstractPushNode {
        @Child private FrameSlotReadNode readTempNode;
        private final int indexInArray;
        private final int indexOfArray;

        protected PushRemoteTempNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int indexInArray, final int indexOfArray) {
            super(code, index, numBytecodes);
            this.indexInArray = indexInArray;
            this.indexOfArray = indexOfArray;
            readTempNode = FrameSlotReadNode.create(code.getStackSlot(indexOfArray));
        }

        public static AbstractBytecodeNode create(final CompiledCodeObject code, final int index, final int numBytecodes, final int indexInArray, final int indexOfArray) {
            return PushRemoteTempNodeGen.create(code, index, numBytecodes, indexInArray, indexOfArray);
        }

        @Specialization
        protected final void executeVoid(final VirtualFrame frame,
                        @CachedLibrary(limit = "3") final SqueakObjectLibrary objectLibary) {
            pushNode.executePush(frame, objectLibary.at0(readTempNode.executeRead(frame), indexInArray));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushTemp: " + indexInArray + " inVectorAt: " + indexOfArray;
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static final class PushTemporaryLocationNode extends AbstractBytecodeNode {
        @Child private FrameStackWriteNode pushNode;
        @Child private FrameSlotReadNode tempNode;
        private final int tempIndex;

        public PushTemporaryLocationNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int tempIndex) {
            super(code, index, numBytecodes);
            this.tempIndex = tempIndex;
            pushNode = FrameStackWriteNode.create(code);
            tempNode = FrameSlotReadNode.create(code.getStackSlot(tempIndex));
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executePush(frame, tempNode.executeRead(frame));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "pushTemp: " + tempIndex;
        }
    }
}
