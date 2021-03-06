// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.todo;

import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.TextRange;
import com.intellij.testFramework.EditorTestUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class TodoItemsTestCase extends LightDaemonAnalyzerTestCase {
  protected abstract String getFileExtension();

  protected void testTodos(String text) {
    configureFromFileText("Foo." + getFileExtension(), text);
    EditorTestUtil.setEditorVisibleSize(myEditor, 1000, 1000); // set visible area for highlighting
    List<TextRange> expectedTodoRanges = extractExpectedTodoRanges(myEditor.getDocument());
    List<HighlightInfo> highlightInfos = doHighlighting();
    List<TextRange> actualTodoRanges = getActualTodoRanges(highlightInfos);
    assertTodoRanges(expectedTodoRanges, actualTodoRanges);
  }

  protected void checkTodos(String text) {
    DocumentImpl document = new DocumentImpl(text);
    List<TextRange> expectedTodoRanges = extractExpectedTodoRanges(document);
    List<HighlightInfo> highlightInfos = doHighlighting();
    checkResultByText(document.getText());
    List<TextRange> actualTodoRanges = getActualTodoRanges(highlightInfos);
    assertTodoRanges(expectedTodoRanges, actualTodoRanges);
  }

  private static List<TextRange> extractExpectedTodoRanges(Document document) {
    ArrayList<TextRange> result = new ArrayList<>();
    int offset = 0;
    int startPos;
    while ((startPos = document.getText().indexOf('[', offset)) != -1) {
      int finalStartPos = startPos;
      WriteCommandAction.runWriteCommandAction(ourProject, () -> document.deleteString(finalStartPos, finalStartPos + 1));
      int endPos = document.getText().indexOf(']', startPos);
      if (endPos == -1) break;
      WriteCommandAction.runWriteCommandAction(ourProject, () -> document.deleteString(endPos, endPos + 1));
      result.add(new TextRange(startPos, endPos));
    }
    return result;
  }

  private static List<TextRange> getActualTodoRanges(List<HighlightInfo> highlightInfos) {
    return highlightInfos.stream()
      .filter(info -> info.type == HighlightInfoType.TODO)
      .map(info -> TextRange.create(info.getHighlighter()))
      .sorted(Segment.BY_START_OFFSET_THEN_END_OFFSET)
      .collect(Collectors.toList());
  }

  private static void assertTodoRanges(List<TextRange> expectedTodoRanges, List<TextRange> actualTodoRanges) {
    assertEquals("Unexpected todos highlighting", generatePresentation(expectedTodoRanges), generatePresentation(actualTodoRanges));
  }

  private static String generatePresentation(List<TextRange> ranges) {
    StringBuilder b = new StringBuilder(myEditor.getDocument().getText());
    int prevStart = Integer.MAX_VALUE;
    for (int i = ranges.size() - 1; i >= 0; i--) {
      TextRange r = ranges.get(i);
      assertTrue(r.getEndOffset() <= prevStart);
      b.insert(r.getEndOffset(), ']');
      b.insert(prevStart = r.getStartOffset(), '[');
    }
    return b.toString();
  }

  @Override
  protected boolean doInspections() {
    return false;
  }
}
