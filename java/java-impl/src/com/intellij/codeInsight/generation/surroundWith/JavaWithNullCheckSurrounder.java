
/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.codeInsight.generation.surroundWith;

import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;

class JavaWithNullCheckSurrounder extends JavaExpressionSurrounder{
  public boolean isApplicable(PsiExpression expr) {
    PsiType type = expr.getType();
    if (type instanceof PsiPrimitiveType) return false;
    if (!expr.isPhysical()) return false;
    if (expr.getParent() instanceof PsiExpressionStatement) return false;
    PsiElement parent = PsiTreeUtil.getParentOfType(expr, PsiExpressionStatement.class);
    if (parent == null) return false;
    final PsiElement element = parent.getParent();
    if (!(element instanceof PsiCodeBlock) && !(JspPsiUtil.isInJspFile(element)  && element instanceof PsiFile)) return false;
    return true;
  }

  public TextRange surroundExpression(Project project, Editor editor, PsiExpression expr) throws IncorrectOperationException {
    PsiManager manager = expr.getManager();
    PsiElementFactory factory = JavaPsiFacade.getInstance(manager.getProject()).getElementFactory();
    CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);

    @NonNls String text = "if(a != null){\nst;\n}";
    PsiIfStatement ifStatement = (PsiIfStatement)factory.createStatementFromText(text, null);
    ifStatement = (PsiIfStatement)codeStyleManager.reformat(ifStatement);

    ((PsiBinaryExpression)ifStatement.getCondition()).getLOperand().replace(expr);

    PsiExpressionStatement statement = PsiTreeUtil.getParentOfType(expr, PsiExpressionStatement.class);
    String oldText = statement.getText();
    ifStatement = (PsiIfStatement)statement.replace(ifStatement);
    PsiCodeBlock block = ((PsiBlockStatement)ifStatement.getThenBranch()).getCodeBlock();
    block = CodeInsightUtilBase.forcePsiPostprocessAndRestoreElement(block);
    PsiElement replace = block.getStatements()[0].replace(factory.createStatementFromText(oldText, block));
    int offset = replace.getTextRange().getEndOffset();
    return new TextRange(offset, offset);
  }

  public String getTemplateDescription() {
    return "if (expr != null) {...}";
  }
}
