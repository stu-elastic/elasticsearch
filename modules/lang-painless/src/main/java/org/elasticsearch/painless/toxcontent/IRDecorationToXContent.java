/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.painless.toxcontent;

import org.elasticsearch.painless.FunctionRef;
import org.elasticsearch.painless.ir.IRNode.IRCondition;
import org.elasticsearch.painless.ir.IRNode.IRDecoration;
import org.elasticsearch.painless.lookup.PainlessCast;
import org.elasticsearch.painless.lookup.PainlessClassBinding;
import org.elasticsearch.painless.lookup.PainlessConstructor;
import org.elasticsearch.painless.lookup.PainlessField;
import org.elasticsearch.painless.lookup.PainlessInstanceBinding;
import org.elasticsearch.painless.lookup.PainlessMethod;
import org.elasticsearch.painless.symbol.FunctionTable;
import org.elasticsearch.painless.symbol.IRDecorations.IRCAllEscape;
import org.elasticsearch.painless.symbol.IRDecorations.IRCContinuous;
import org.elasticsearch.painless.symbol.IRDecorations.IRCInitialize;
import org.elasticsearch.painless.symbol.IRDecorations.IRCRead;
import org.elasticsearch.painless.symbol.IRDecorations.IRCStatic;
import org.elasticsearch.painless.symbol.IRDecorations.IRCSynthetic;
import org.elasticsearch.painless.symbol.IRDecorations.IRCVarArgs;
import org.elasticsearch.painless.symbol.IRDecorations.IRDArrayName;
import org.elasticsearch.painless.symbol.IRDecorations.IRDArrayType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDBinaryType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDCaptureNames;
import org.elasticsearch.painless.symbol.IRDecorations.IRDCast;
import org.elasticsearch.painless.symbol.IRDecorations.IRDClassBinding;
import org.elasticsearch.painless.symbol.IRDecorations.IRDComparisonType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDConstant;
import org.elasticsearch.painless.symbol.IRDecorations.IRDConstantFieldName;
import org.elasticsearch.painless.symbol.IRDecorations.IRDConstructor;
import org.elasticsearch.painless.symbol.IRDecorations.IRDDeclarationType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDDefReferenceEncoding;
import org.elasticsearch.painless.symbol.IRDecorations.IRDDepth;
import org.elasticsearch.painless.symbol.IRDecorations.IRDExceptionType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDExpressionType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDField;
import org.elasticsearch.painless.symbol.IRDecorations.IRDFieldType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDFlags;
import org.elasticsearch.painless.symbol.IRDecorations.IRDFunction;
import org.elasticsearch.painless.symbol.IRDecorations.IRDIndexName;
import org.elasticsearch.painless.symbol.IRDecorations.IRDIndexType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDIndexedType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDInstanceBinding;
import org.elasticsearch.painless.symbol.IRDecorations.IRDInstanceType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDIterableName;
import org.elasticsearch.painless.symbol.IRDecorations.IRDIterableType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDMaxLoopCounter;
import org.elasticsearch.painless.symbol.IRDecorations.IRDMethod;
import org.elasticsearch.painless.symbol.IRDecorations.IRDModifiers;
import org.elasticsearch.painless.symbol.IRDecorations.IRDName;
import org.elasticsearch.painless.symbol.IRDecorations.IRDOperation;
import org.elasticsearch.painless.symbol.IRDecorations.IRDParameterNames;
import org.elasticsearch.painless.symbol.IRDecorations.IRDReference;
import org.elasticsearch.painless.symbol.IRDecorations.IRDRegexLimit;
import org.elasticsearch.painless.symbol.IRDecorations.IRDReturnType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDShiftType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDSize;
import org.elasticsearch.painless.symbol.IRDecorations.IRDStoreType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDSymbol;
import org.elasticsearch.painless.symbol.IRDecorations.IRDType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDTypeParameters;
import org.elasticsearch.painless.symbol.IRDecorations.IRDUnaryType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDValue;
import org.elasticsearch.painless.symbol.IRDecorations.IRDVariableName;
import org.elasticsearch.painless.symbol.IRDecorations.IRDVariableType;

import java.util.List;

public class IRDecorationToXContent {

    public static void visitType(IRDType ird, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("decoration", ird.getClass().getSimpleName());
        if (ird.getValue() != null) {
            builder.field("type", ird.getValue().getClass().getSimpleName());
            builder.field("value", ird.getValue().toString());
        }
        builder.endObject();
    }

    public static void visitAbstractDecoration(IRDecoration<?> ird, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("decoration", ird.getClass().getSimpleName());
        if (ird.getValue() != null) {
            builder.field("type", ird.getValue().getClass().getSimpleName());
            builder.field("value", ird.getValue().toString());
        }
        builder.endObject();
    }

    public static void visiDecorationListString(IRDecoration<List<String>> ird, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("decoration", ird.getClass().getSimpleName());
        if (ird.getValue() != null) {
            builder.field("type", ird.getValue().getClass().getSimpleName());
            if (ird.getValue().size() > 0) {
                builder.startArray("value");
                for (String value : ird.getValue()) {
                    builder.value(value);
                }
                builder.endArray();
            }
        }
        builder.endObject();
    }

    public static void visitCondition(IRCondition irc, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("condition", irc.getClass().getSimpleName());
        builder.endObject();
    }

    public static void visitExpressionType(IRDExpressionType ird, XContentBuilderWrapper builder) {
        visitType(ird, builder);
    }

    public static void visitBinaryType(IRDBinaryType ird, XContentBuilderWrapper builder) {
        visitType(ird, builder);
    }

    public static void visitShiftType(IRDShiftType ird, XContentBuilderWrapper builder) {
        visitType(ird, builder);
    }

    public static void visitOperation(IRDOperation ird, XContentBuilderWrapper builder) {
        visitAbstractDecoration(ird, builder);
    }

    public static void visitFlags(IRDFlags ird, XContentBuilderWrapper builder) {
        visitAbstractDecoration(ird, builder);
    }

    public static void visitAllEscape(IRCAllEscape irc, XContentBuilderWrapper builder) {
        visitCondition(irc, builder);
    }

    public static void visitCast(IRDCast ird, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("decoration", ird.getClass().getSimpleName());
        if (ird.getValue() != null) {
            builder.field("type", ird.getValue().getClass().getSimpleName());
            PainlessCast cast = ird.getValue();
            if (cast != null) {
                builder.field("cast");
                DecorationToXContent.toXContent(cast, builder);
            }
        }
        builder.endObject();
    }

    public static void visitExceptionType(IRDExceptionType ird, XContentBuilderWrapper builder) {
        visitType(ird, builder);
    }

    public static void visitSymbol(IRDSymbol ird, XContentBuilderWrapper builder) {
        visitAbstractDecoration(ird, builder);
    }

    public static void visitComparisonType(IRDComparisonType ird, XContentBuilderWrapper builder) {
        visitType(ird, builder);
    }

    public static void visitConstant(IRDConstant ird, XContentBuilderWrapper builder) {
        visitAbstractDecoration(ird, builder);
    }

    public static void visitConstantFieldName(IRDConstantFieldName ird, XContentBuilderWrapper builder) {
        visitAbstractDecoration(ird, builder);
    }

    public static void visitDeclarationType(IRDDeclarationType ird, XContentBuilderWrapper builder) {
        visitType(ird, builder);
    }

    public static void visitName(IRDName ird, XContentBuilderWrapper builder) {
        visitAbstractDecoration(ird, builder);
    }

    public static void visitDefReferenceEncoding(IRDDefReferenceEncoding ird, XContentBuilderWrapper builder) {
        visitAbstractDecoration(ird, builder);
    }

    public static void visitSize(IRDSize ird, XContentBuilderWrapper builder) {
        visitAbstractDecoration(ird, builder);
    }

    public static void visitDepth(IRDDepth ird, XContentBuilderWrapper builder) {
        visitAbstractDecoration(ird, builder);
    }

    public static void visitModifiers(IRDModifiers ird, XContentBuilderWrapper builder) {
        visitAbstractDecoration(ird, builder);
    }

    public static void visitFieldType(IRDFieldType ird, XContentBuilderWrapper builder) {
        visitType(ird, builder);
    }

    public static void visitVariableType(IRDVariableType ird, XContentBuilderWrapper builder) {
        visitType(ird, builder);
    }

    public static void visitVariableName(IRDVariableName ird, XContentBuilderWrapper builder) {
        visitAbstractDecoration(ird, builder);
    }

    public static void visitArrayType(IRDArrayType ird, XContentBuilderWrapper builder) {
        visitType(ird, builder);
    }

    public static void visitArrayName(IRDArrayName ird, XContentBuilderWrapper builder) {
        visitAbstractDecoration(ird, builder);
    }

    public static void visitIndexType(IRDIndexType ird, XContentBuilderWrapper builder) {
        visitType(ird, builder);
    }

    public static void visitIndexName(IRDIndexName ird, XContentBuilderWrapper builder) {
        visitAbstractDecoration(ird, builder);
    }

    public static void visitIndexedType(IRDIndexedType ird, XContentBuilderWrapper builder) {
        visitType(ird, builder);
    }

    public static void visitIterableType(IRDIterableType ird, XContentBuilderWrapper builder) {
        visitType(ird, builder);
    }

    public static void visitIterableName(IRDIterableName ird, XContentBuilderWrapper builder) {
        visitAbstractDecoration(ird, builder);
    }

    public static void visitMethod(IRDMethod ird, XContentBuilderWrapper builder) {
        builder.startObject();
        PainlessMethod method = ird.getValue();
        if (method != null) {
            builder.field("value");
            DecorationToXContent.toXContent(method, builder);
        }
        builder.endObject();
        visitAbstractDecoration(ird, builder);
    }

    public static void visitReturnType(IRDReturnType ird, XContentBuilderWrapper builder) {
        visitType(ird, builder);
    }

    public static void visitTypeParameters(IRDTypeParameters ird, XContentBuilderWrapper builder) {
        // TODO(stu): handle as list of class
    }

    public static void visitParameterNames(IRDParameterNames ird, XContentBuilderWrapper builder) {
        visiDecorationListString(ird, builder);
    }

    public static void visitStatic(IRCStatic irc, XContentBuilderWrapper builder) {
        visitCondition(irc, builder);
    }

    public static void visitVarArgs(IRCVarArgs irc, XContentBuilderWrapper builder) {
        visitCondition(irc, builder);
    }

    public static void visitSynthetic(IRCSynthetic irc, XContentBuilderWrapper builder) {
        visitCondition(irc, builder);
    }

    public static void visitMaxLoopCounter(IRDMaxLoopCounter ird, XContentBuilderWrapper builder) {
        visitAbstractDecoration(ird, builder);
    }

    public static void visitInstanceType(IRDInstanceType ird, XContentBuilderWrapper builder) {
        visitType(ird, builder);
    }

    public static void visitFunction(IRDFunction ird, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("decoration", ird.getClass().getSimpleName());
        if (ird.getValue() != null) {
            builder.field("type", ird.getValue().getClass().getSimpleName());
            FunctionTable.LocalFunction func = ird.getValue();
            if (func != null) {
                builder.field("function");
                DecorationToXContent.toXContent(func, builder);
            }
        }
        builder.endObject();
    }

    public static void visitClassBinding(IRDClassBinding ird, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("decoration", ird.getClass().getSimpleName());
        if (ird.getValue() != null) {
            builder.field("type", ird.getValue().getClass().getSimpleName());
            PainlessClassBinding binding = ird.getValue();
            if (binding != null) {
                builder.field("binding");
                DecorationToXContent.toXContent(binding, builder);
            }
        }
        builder.endObject();
    }

    public static void visitInstanceBinding(IRDInstanceBinding ird, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("decoration", ird.getClass().getSimpleName());
        if (ird.getValue() != null) {
            builder.field("type", ird.getValue().getClass().getSimpleName());
            PainlessInstanceBinding binding = ird.getValue();
            if (binding != null) {
                builder.field("binding");
                DecorationToXContent.toXContent(binding, builder);
            }
        }
        builder.endObject();
    }

    public static void visitConstructor(IRDConstructor ird, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("decoration", ird.getClass().getSimpleName());
        if (ird.getValue() != null) {
            builder.field("type", ird.getValue().getClass().getSimpleName());
            PainlessConstructor constructor = ird.getValue();
            if (constructor != null) {
                builder.field("constructor");
                DecorationToXContent.toXContent(constructor, builder);
            }
        }
        builder.endObject();
    }

    public static void visitValue(IRDValue ird, XContentBuilderWrapper builder) {
        visitAbstractDecoration(ird, builder);
    }

    public static void visitField(IRDField ird, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("decoration", ird.getClass().getSimpleName());
        if (ird.getValue() != null) {
            builder.field("type", ird.getValue().getClass().getSimpleName());
            PainlessField field = ird.getValue();
            if (field != null) {
                builder.field("field");
                DecorationToXContent.toXContent(field, builder);
            }
        }
        builder.endObject();
    }

    public static void visitContinuous(IRCContinuous irc, XContentBuilderWrapper builder) {
        visitCondition(irc, builder);
    }

    public static void visitInitialize(IRCInitialize irc, XContentBuilderWrapper builder) {
        visitCondition(irc, builder);
    }

    public static void visitRead(IRCRead irc, XContentBuilderWrapper builder) {
        visitCondition(irc, builder);
    }

    public static void visitCaptureNames(IRDCaptureNames ird, XContentBuilderWrapper builder) {
        visiDecorationListString(ird, builder);
    }

    public static void visitStoreType(IRDStoreType ird, XContentBuilderWrapper builder) {
        visitType(ird, builder);
    }

    public static void visitUnaryType(IRDUnaryType ird, XContentBuilderWrapper builder) {
        visitType(ird, builder);
    }

    public static void visitReference(IRDReference ird, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("decoration", ird.getClass().getSimpleName());
        if (ird.getValue() != null) {
            builder.field("type", ird.getValue().getClass().getSimpleName());
            FunctionRef functionRef = ird.getValue();
            if (functionRef != null) {
                // TODO(stu): regularize whether to start an object here or not
                builder.startObject("functionRef");
                DecorationToXContent.toXContent(functionRef, builder);
                builder.endObject();
            }
        }
        builder.endObject();

    }

    public static void visitRegexLimit(IRDRegexLimit ird, XContentBuilderWrapper builder) {
        visitAbstractDecoration(ird, builder);
    }

    public static void visitIRDecoration(IRCondition irc, XContentBuilderWrapper builder) {
        if (irc instanceof IRCAllEscape) {
            visitAllEscape((IRCAllEscape) irc, builder);
        } else if (irc instanceof IRCStatic) {
            visitStatic((IRCStatic) irc, builder);
        } else if (irc instanceof IRCVarArgs) {
            visitVarArgs((IRCVarArgs) irc, builder);
        } else if (irc instanceof IRCSynthetic) {
            visitSynthetic((IRCSynthetic) irc, builder);
        } else if (irc instanceof IRCContinuous) {
            visitContinuous((IRCContinuous) irc, builder);
        } else if (irc instanceof IRCInitialize) {
            visitInitialize((IRCInitialize) irc, builder);
        } else if (irc instanceof IRCRead) {
            visitRead((IRCRead) irc, builder);
        }
        throw new IllegalStateException("unhandled IRCCondition [" + irc.getClass().getSimpleName() + "]");
    }

    public static void visitIRDecoration(IRDecoration<?> ird, XContentBuilderWrapper builder) {
        if (ird instanceof IRDExpressionType) {
            visitExpressionType((IRDExpressionType) ird, builder);
        } else if (ird instanceof IRDBinaryType) {
            visitBinaryType((IRDBinaryType) ird, builder);
        } else if (ird instanceof IRDShiftType) {
            visitShiftType((IRDShiftType) ird, builder);
        } else if (ird instanceof IRDOperation) {
            visitOperation((IRDOperation) ird, builder);
        } else if (ird instanceof IRDFlags) {
            visitFlags((IRDFlags) ird, builder);
        } else if (ird instanceof IRDCast) {
            visitCast((IRDCast) ird, builder);
        } else if (ird instanceof IRDExceptionType) {
            visitExceptionType((IRDExceptionType) ird, builder);
        } else if (ird instanceof IRDSymbol) {
            visitSymbol((IRDSymbol) ird, builder);
        } else if (ird instanceof IRDComparisonType) {
            visitComparisonType((IRDComparisonType) ird, builder);
        } else if (ird instanceof IRDConstant) {
            visitConstant((IRDConstant) ird, builder);
        } else if (ird instanceof IRDConstantFieldName) {
            visitConstantFieldName((IRDConstantFieldName) ird, builder);
        } else if (ird instanceof IRDDeclarationType) {
            visitDeclarationType((IRDDeclarationType) ird, builder);
        } else if (ird instanceof IRDName) {
            visitName((IRDName) ird, builder);
        } else if (ird instanceof IRDDefReferenceEncoding) {
            visitDefReferenceEncoding((IRDDefReferenceEncoding) ird, builder);
        } else if (ird instanceof IRDSize) {
            visitSize((IRDSize) ird, builder);
        } else if (ird instanceof IRDDepth) {
            visitDepth((IRDDepth) ird, builder);
        } else if (ird instanceof IRDModifiers) {
            visitModifiers((IRDModifiers) ird, builder);
        } else if (ird instanceof IRDFieldType) {
            visitFieldType((IRDFieldType) ird, builder);
        } else if (ird instanceof IRDVariableType) {
            visitVariableType((IRDVariableType) ird, builder);
        } else if (ird instanceof IRDVariableName) {
            visitVariableName((IRDVariableName) ird, builder);
        } else if (ird instanceof IRDArrayType) {
            visitArrayType((IRDArrayType) ird, builder);
        } else if (ird instanceof IRDArrayName) {
            visitArrayName((IRDArrayName) ird, builder);
        } else if (ird instanceof IRDIndexType) {
            visitIndexType((IRDIndexType) ird, builder);
        } else if (ird instanceof IRDIndexName) {
            visitIndexName((IRDIndexName) ird, builder);
        } else if (ird instanceof IRDIndexedType) {
            visitIndexedType((IRDIndexedType) ird, builder);
        } else if (ird instanceof IRDIterableType) {
            visitIterableType((IRDIterableType) ird, builder);
        } else if (ird instanceof IRDIterableName) {
            visitIterableName((IRDIterableName) ird, builder);
        } else if (ird instanceof IRDMethod) {
            visitMethod((IRDMethod) ird, builder);
        } else if (ird instanceof IRDReturnType) {
            visitReturnType((IRDReturnType) ird, builder);
        } else if (ird instanceof IRDTypeParameters) {
            visitTypeParameters((IRDTypeParameters) ird, builder);
        } else if (ird instanceof IRDParameterNames) {
            visitParameterNames((IRDParameterNames) ird, builder);
        } else if (ird instanceof IRDMaxLoopCounter) {
            visitMaxLoopCounter((IRDMaxLoopCounter) ird, builder);
        } else if (ird instanceof IRDInstanceType) {
            visitInstanceType((IRDInstanceType) ird, builder);
        } else if (ird instanceof IRDFunction) {
            visitFunction((IRDFunction) ird, builder);
        } else if (ird instanceof IRDClassBinding) {
            visitClassBinding((IRDClassBinding) ird, builder);
        } else if (ird instanceof IRDInstanceBinding) {
            visitInstanceBinding((IRDInstanceBinding) ird, builder);
        } else if (ird instanceof IRDConstructor) {
            visitConstructor((IRDConstructor) ird, builder);
        } else if (ird instanceof IRDValue) {
            visitValue((IRDValue) ird, builder);
        } else if (ird instanceof IRDField) {
            visitField((IRDField) ird, builder);
        } else if (ird instanceof IRDCaptureNames) {
            visitCaptureNames((IRDCaptureNames) ird, builder);
        } else if (ird instanceof IRDStoreType) {
            visitStoreType((IRDStoreType) ird, builder);
        } else if (ird instanceof IRDUnaryType) {
            visitUnaryType((IRDUnaryType) ird, builder);
        } else if (ird instanceof IRDReference) {
            visitReference((IRDReference) ird, builder);
        } else if (ird instanceof IRDRegexLimit) {
            visitRegexLimit((IRDRegexLimit) ird, builder);
        } else if (ird instanceof IRDType) {
            visitType((IRDType) ird, builder);
        } else {
            throw new IllegalStateException("unhandled IRDecoration [" + ird.getClass().getSimpleName() + "]");
        }
    }
}
