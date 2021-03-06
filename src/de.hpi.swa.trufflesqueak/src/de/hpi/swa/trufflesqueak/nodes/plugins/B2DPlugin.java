/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.BinaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.OctonaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.QuinaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.SenaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.SeptenaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.TernaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.UnaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public final class B2DPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return B2DPluginFactory.getFactories();
    }

    // primitiveAbortProcessing omitted because it does not seem to be used.

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddActiveEdgeEntry")
    protected abstract static class PrimAddActiveEdgeEntryNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization
        protected static final PointersObject doAdd(final PointersObject receiver, final PointersObject edgeEntry,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.b2d.primitiveAddActiveEdgeEntry(receiver, edgeEntry);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddBezier")
    protected abstract static class PrimAddBezierNode extends AbstractPrimitiveNode implements SenaryPrimitiveFallback {

        @Specialization(guards = {"start.isPoint()", "stop.isPoint()", "via.isPoint()"})
        protected static final PointersObject doAdd(final PointersObject receiver, final PointersObject start, final PointersObject stop, final PointersObject via, final long leftFillIndex,
                        final long rightFillIndex,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.b2d.primitiveAddBezier(receiver, start, stop, via, leftFillIndex, rightFillIndex);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddBezierShape")
    protected abstract static class PrimAddBezierShapeNode extends AbstractPrimitiveNode implements SenaryPrimitiveFallback {

        @Specialization
        protected static final PointersObject doAdd(final PointersObject receiver, final AbstractSqueakObject points, final long nSegments, final long fillStyle, final long lineWidth,
                        final long lineFill,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.b2d.primitiveAddBezierShape(receiver, points, nSegments, fillStyle, lineWidth, lineFill);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddBitmapFill")
    protected abstract static class PrimAddBitmapFillNode extends AbstractPrimitiveNode implements OctonaryPrimitiveFallback {

        @Specialization(guards = {"xIndex > 0", "origin.isPoint()", "direction.isPoint()", "normal.isPoint()"})
        protected static final long doAdd(final PointersObject receiver, final PointersObject form, final AbstractSqueakObject cmap, final boolean tileFlag, final PointersObject origin,
                        final PointersObject direction, final PointersObject normal, final long xIndex,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.b2d.primitiveAddBitmapFill(receiver, form, cmap, tileFlag, origin, direction, normal, xIndex);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddCompressedShape")
    protected abstract static class PrimAddCompressedShapeNode extends AbstractPrimitiveNode implements OctonaryPrimitiveFallback {

        @Specialization
        protected static final PointersObject doAdd(final PointersObject receiver, final NativeObject points, final long nSegments, final NativeObject leftFills, final NativeObject rightFills,
                        final NativeObject lineWidths, final NativeObject lineFills, final NativeObject fillIndexList,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.b2d.primitiveAddCompressedShape(receiver, points, nSegments, leftFills, rightFills, lineWidths, lineFills, fillIndexList);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddGradientFill")
    protected abstract static class PrimAddGradientFillNode extends AbstractPrimitiveNode implements SenaryPrimitiveFallback {

        @Specialization(guards = {"colorRamp.getSqueakClass().isBitmapClass()", "origin.isPoint()", "direction.isPoint()", "normal.isPoint()"})
        protected static final long doAdd(final PointersObject receiver, final NativeObject colorRamp, final PointersObject origin, final PointersObject direction,
                        final PointersObject normal,
                        final boolean isRadial,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.b2d.primitiveAddGradientFill(receiver, colorRamp, origin, direction, normal, isRadial);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddLine")
    protected abstract static class PrimAddLineNode extends AbstractPrimitiveNode implements QuinaryPrimitiveFallback {

        @Specialization(guards = {"start.isPoint()", "end.isPoint()"})
        protected static final PointersObject doAdd(final PointersObject receiver, final PointersObject start, final PointersObject end, final long leftFill, final long rightFill,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.b2d.primitiveAddLine(receiver, start, end, leftFill, rightFill);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddOval")
    protected abstract static class PrimAddOvalNode extends AbstractPrimitiveNode implements SenaryPrimitiveFallback {

        @Specialization(guards = {"start.isPoint()", "end.isPoint()"})
        protected static final PointersObject doAdd(final PointersObject receiver, final PointersObject start, final PointersObject end, final long fillIndex, final long width,
                        final long pixelValue32,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.b2d.primitiveAddOval(receiver, start, end, fillIndex, width, pixelValue32);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddPolygon")
    protected abstract static class PrimAddPolygonNode extends AbstractPrimitiveNode implements SenaryPrimitiveFallback {

        @Specialization
        protected static final PointersObject doAdd(final PointersObject receiver, final AbstractSqueakObject points, final long nSegments, final long fillStyle, final long lineWidth,
                        final long lineFill,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.b2d.primitiveAddPolygon(receiver, points, nSegments, fillStyle, lineWidth, lineFill);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddRect")
    protected abstract static class PrimAddRectNode extends AbstractPrimitiveNode implements SenaryPrimitiveFallback {

        @Specialization(guards = {"start.isPoint()", "end.isPoint()"})
        protected static final PointersObject doAdd(final PointersObject receiver, final PointersObject start, final PointersObject end, final long fillIndex, final long width,
                        final long pixelValue32,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.b2d.primitiveAddRect(receiver, start, end, fillIndex, width, pixelValue32);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveChangedActiveEdgeEntry")
    protected abstract static class PrimChangedActiveEdgeEntryNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization
        protected static final PointersObject doChange(final PointersObject receiver, final PointersObject edgeEntry,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.b2d.primitiveChangedActiveEdgeEntry(receiver, edgeEntry);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCopyBuffer")
    protected abstract static class PrimCopyBufferNode extends AbstractPrimitiveNode implements TernaryPrimitiveFallback {

        @Specialization(guards = {"oldBuffer.isIntType()", "newBuffer.isIntType()"})
        protected static final PointersObject doCopy(final PointersObject receiver, final NativeObject oldBuffer, final NativeObject newBuffer,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.b2d.primitiveCopyBuffer(oldBuffer, newBuffer);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDisplaySpanBuffer")
    protected abstract static class PrimDisplaySpanBufferNode extends AbstractPrimitiveNode implements UnaryPrimitiveFallback {

        @Specialization
        protected static final PointersObject doDisplay(final PointersObject receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.b2d.primitiveDisplaySpanBuffer(receiver);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDoProfileStats")
    protected abstract static class PrimDoProfileStatsNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization
        protected static final boolean doProfile(@SuppressWarnings("unused") final Object receiver, final boolean aBoolean,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.b2d.primitiveDoProfileStats(aBoolean);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFinishedProcessing")
    protected abstract static class PrimFinishedProcessingNode extends AbstractPrimitiveNode implements UnaryPrimitiveFallback {

        @Specialization
        protected static final boolean doCopy(final PointersObject receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.b2d.primitiveFinishedProcessing(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetAALevel")
    protected abstract static class PrimGetAALevelNode extends AbstractPrimitiveNode implements UnaryPrimitiveFallback {

        @Specialization
        protected static final long doGet(final PointersObject receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.b2d.primitiveGetAALevel(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetBezierStats")
    protected abstract static class PrimGetBezierStatsNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization(guards = {"statsArray.isIntType()", "statsArray.getIntLength() >= 4"})
        protected static final PointersObject doGet(final PointersObject receiver, final NativeObject statsArray,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.b2d.primitiveGetBezierStats(receiver, statsArray);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetClipRect")
    protected abstract static class PrimGetClipRectNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization(guards = {"rect.size() >= 2"})
        protected static final PointersObject doGet(final PointersObject receiver, final PointersObject rect,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            image.b2d.primitiveGetClipRect(writeNode, receiver, rect);
            return rect;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetCounts")
    protected abstract static class PrimGetCountsNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization(guards = {"statsArray.isIntType()", "statsArray.getIntLength() >= 9"})
        protected static final PointersObject doGet(final PointersObject receiver, final NativeObject statsArray,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.b2d.primitiveGetCounts(receiver, statsArray);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetDepth")
    protected abstract static class PrimGetDepthNode extends AbstractPrimitiveNode implements UnaryPrimitiveFallback {

        @Specialization
        protected static final long doGet(final PointersObject receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.b2d.primitiveGetDepth(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetFailureReason")
    protected abstract static class PrimGetFailureReasonNode extends AbstractPrimitiveNode implements UnaryPrimitiveFallback {

        @Specialization
        protected static final long doGet(final PointersObject receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.b2d.primitiveGetFailureReason(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetOffset")
    protected abstract static class PrimGetOffsetNode extends AbstractPrimitiveNode implements UnaryPrimitiveFallback {

        @Specialization
        protected static final PointersObject doGet(final PointersObject receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            return image.b2d.primitiveGetOffset(writeNode, receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetTimes")
    protected abstract static class PrimGetTimesNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization(guards = {"statsArray.isIntType()", "statsArray.getIntLength() >= 9"})
        protected static final PointersObject doGet(final PointersObject receiver, final NativeObject statsArray,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.b2d.primitiveGetTimes(receiver, statsArray);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveInitializeBuffer")
    protected abstract static class PrimInitializeBufferNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization(guards = {"buffer.isIntType()", "hasMinimalSize(buffer)"})
        protected static final Object doInit(final Object receiver, final NativeObject buffer,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.b2d.primitiveInitializeBuffer(buffer);
            return receiver;
        }

        protected static final boolean hasMinimalSize(final NativeObject buffer) {
            return buffer.getIntLength() >= B2D.GW_MINIMAL_SIZE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveInitializeProcessing")
    protected abstract static class PrimInitializeProcessingNode extends AbstractPrimitiveNode implements UnaryPrimitiveFallback {

        @Specialization
        protected static final PointersObject doCopy(final PointersObject receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.b2d.primitiveInitializeProcessing(receiver);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveMergeFillFrom")
    protected abstract static class PrimMergeFillFromNode extends AbstractPrimitiveNode implements TernaryPrimitiveFallback {

        @Specialization(guards = {"fillBitmap.getSqueakClass().isBitmapClass()"})
        protected static final PointersObject doCopy(final PointersObject receiver, final NativeObject fillBitmap, final PointersObject fill,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.b2d.primitiveMergeFillFrom(receiver, fillBitmap, fill);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveNeedsFlush")
    protected abstract static class PrimNeedsFlushNode extends AbstractPrimitiveNode implements UnaryPrimitiveFallback {

        @Specialization
        protected static final boolean doNeed(final PointersObject receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.b2d.primitiveNeedsFlush(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveNeedsFlushPut")
    protected abstract static class PrimNeedsFlushPutNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization
        protected static final PointersObject doNeed(final PointersObject receiver, final boolean aBoolean,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.b2d.primitiveNeedsFlushPut(receiver, aBoolean);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveNextActiveEdgeEntry")
    protected abstract static class PrimNextActiveEdgeEntryNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization
        protected static final boolean doNext(final PointersObject receiver, final PointersObject edgeEntry,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.b2d.primitiveNextActiveEdgeEntry(receiver, edgeEntry);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveNextFillEntry")
    protected abstract static class PrimNextFillEntryNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization
        protected static final boolean doNext(final PointersObject receiver, final PointersObject fillEntry,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.b2d.primitiveNextFillEntry(receiver, fillEntry);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveNextGlobalEdgeEntry")
    protected abstract static class PrimNextGlobalEdgeEntryNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization
        protected static final boolean doNext(final PointersObject receiver, final PointersObject edgeEntry,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.b2d.primitiveNextGlobalEdgeEntry(receiver, edgeEntry);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveRegisterExternalEdge")
    protected abstract static class PrimRegisterExternalEdgeNode extends AbstractPrimitiveNode implements SeptenaryPrimitiveFallback {

        @Specialization
        protected static final PointersObject doRegister(final PointersObject receiver, final long index, final long initialX, final long initialY, final long initialZ, final long leftFillIndex,
                        final long rightFillIndex,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.b2d.primitiveRegisterExternalEdge(receiver, index, initialX, initialY, initialZ, leftFillIndex, rightFillIndex);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveRegisterExternalFill")
    protected abstract static class PrimRegisterExternalFillNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization
        protected static final long doRegister(final PointersObject receiver, final long index,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.b2d.primitiveRegisterExternalFill(receiver, index);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveRenderImage")
    protected abstract static class PrimRenderImageNode extends AbstractPrimitiveNode implements TernaryPrimitiveFallback {

        @Specialization
        protected static final long doRender(final PointersObject receiver, final PointersObject edge, final PointersObject fill,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.b2d.primitiveRenderImage(receiver, edge, fill);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveRenderScanline")
    protected abstract static class PrimRenderScanlineNode extends AbstractPrimitiveNode implements TernaryPrimitiveFallback {

        @Specialization
        protected static final long doRender(final PointersObject receiver, final PointersObject edge, final PointersObject fill,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.b2d.primitiveRenderScanline(receiver, edge, fill);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetAALevel")
    protected abstract static class PrimSetAALevelNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization
        protected static final PointersObject doSet(final PointersObject receiver, final long level,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.b2d.primitiveSetAALevel(receiver, level);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetBitBltPlugin")
    protected abstract static class PrimSetBitBltPluginNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization(guards = {"pluginName.isByteType()"})
        protected static final ClassObject doSet(final ClassObject receiver, final NativeObject pluginName) {
            return B2D.primitiveSetBitBltPlugin(receiver, pluginName);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetClipRect")
    protected abstract static class PrimSetClipRectNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization(guards = {"rect.size() >= 2"})
        protected static final PointersObject doSet(final PointersObject receiver, final PointersObject rect,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.b2d.primitiveSetClipRect(receiver, rect);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetColorTransform")
    protected abstract static class PrimSetColorTransformNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization
        protected static final PointersObject doSet(final PointersObject receiver, final AbstractSqueakObject transform,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.b2d.primitiveSetColorTransform(receiver, transform);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetDepth")
    protected abstract static class PrimSetDepthNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization
        protected static final PointersObject doSet(final PointersObject receiver, final long depth,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.b2d.primitiveSetDepth(receiver, depth);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetEdgeTransform")
    protected abstract static class PrimSetEdgeTransformNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization
        protected static final PointersObject doSet(final PointersObject receiver, final AbstractSqueakObject transform,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.b2d.primitiveSetEdgeTransform(receiver, transform);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetOffset")
    protected abstract static class PrimSetOffsetNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization(guards = {"point.isPoint()"})
        protected static final PointersObject doSet(final PointersObject receiver, final PointersObject point,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.b2d.primitiveSetOffset(receiver, point);
            return receiver;
        }
    }
}
