/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.expression.spel.ast;

import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.standard.CodeFlow;
import org.springframework.expression.spel.support.BooleanTypedValue;

/**
 * Represents the boolean AND operation.
 *
 * @author Andy Clement
 * @author Mark Fisher
 * @author Oliver Becker
 * @since 3.0
 */
public class OpAnd extends Operator {

	public OpAnd(int pos, SpelNodeImpl... operands) {
		super("and", pos, operands);
		this.exitTypeDescriptor = "Z";
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		if (getBooleanValue(state, getLeftOperand()) == false) {
			// no need to evaluate right operand
			return BooleanTypedValue.FALSE;
		}
		return BooleanTypedValue.forValue(getBooleanValue(state, getRightOperand()));
	}

	private boolean getBooleanValue(ExpressionState state, SpelNodeImpl operand) {
		try {
			Boolean value = operand.getValue(state, Boolean.class);
			assertValueNotNull(value);
			return value;
		}
		catch (SpelEvaluationException ex) {
			ex.setPosition(operand.getStartPosition());
			throw ex;
		}
	}

	private void assertValueNotNull(Boolean value) {
		if (value == null) {
			throw new SpelEvaluationException(SpelMessage.TYPE_CONVERSION_ERROR, "null", "boolean");
		}
	}

	@Override
	public boolean isCompilable() {
		SpelNodeImpl left = getLeftOperand();
		SpelNodeImpl right= getRightOperand();
		if (!left.isCompilable() || !right.isCompilable()) {
			return false;
		}
		return
			CodeFlow.isBooleanCompatible(left.getExitDescriptor()) &&
			CodeFlow.isBooleanCompatible(right.getExitDescriptor());		
	}
	
	@Override
	public void generateCode(MethodVisitor mv, CodeFlow codeflow) {
		// pseudo: if (!leftOperandValue) { result=false; } else { result=rightOperandValue; }
		Label elseTarget = new Label();
		Label endOfIf = new Label();
		codeflow.enterCompilationScope();
		getLeftOperand().generateCode(mv, codeflow);
		codeflow.unboxBooleanIfNecessary(mv);
		codeflow.exitCompilationScope();
		mv.visitJumpInsn(IFNE, elseTarget);
		mv.visitLdcInsn(0); // FALSE
		mv.visitJumpInsn(GOTO,endOfIf);
		mv.visitLabel(elseTarget);
		codeflow.enterCompilationScope();
		getRightOperand().generateCode(mv, codeflow);
		codeflow.unboxBooleanIfNecessary(mv);
		codeflow.exitCompilationScope();
		mv.visitLabel(endOfIf);
		codeflow.pushDescriptor(this.exitTypeDescriptor);
	}

}
