package vntokenizer;

import gate.*;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ExecutionInterruptedException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.util.InvalidOffsetException;
import org.apache.log4j.Logger;
import vn.pipeline.Annotation;
import vn.pipeline.VnCoreNLP;
import vn.pipeline.Word;

import java.io.IOException;

@CreoleResource(
    name = "vntokenizer",
    comment = "(A short one-line description of this PR, suitable for a tooltip in the GUI)")
public class SentenceSplitter extends AbstractLanguageAnalyser {

  private static final Logger log = Logger.getLogger(SentenceSplitter.class);

  /**
   * Annotation set name from which this PR will take its input annotations.
   */
  private String inputASName;

  public String getInputASName() {
    return inputASName;
  }

  @Optional
  @RunTime
  @CreoleParameter(comment = "The annotation set used for input annotations")
  public void setInputASName(String inputASName) {
    this.inputASName = inputASName;
  }

  //tất cả ký tự viết hoa tiếng Việt
  private final char[] UPPER_UNICODE = {'\u00C1', '\u00C0', '\u00C2', '\u0102', '\u00C3', '\u1EA4', '\u1EA6', '\u1EAE', '\u1EB0', '\u1EAA', '\u1EB4', '\u1EA2', '\u1EA8', '\u1EB2', '\u1EA0', '\u1EAC', '\u1EB6', '\u0110', '\u00C9', '\u00C8', '\u00CA', '\u1EBC', '\u1EBE', '\u1EC0', '\u1EC4', '\u1EBA', '\u1EC2', '\u1EB8', '\u1EC6', '\u00CD', '\u00CC', '\u0128', '\u1EC8', '\u1ECA', '\u00D3', '\u00D2', '\u00D4', '\u00D5', '\u1ED0', '\u1ED2', '\u1ED6', '\u1ECE', '\u01A0', '\u1ED4', '\u1ECC', '\u1EDA', '\u1EDC', '\u1EE0', '\u1ED8', '\u1EDE', '\u1EE2', '\u00DA', '\u00D9', '\u0168', '\u1EE6', '\u01AF', '\u1EE4', '\u1EE8', '\u1EEA', '\u1EEE', '\u1EEC', '\u1EF0', '\u00DD', '\u1EF2', '\u1EF8', '\u1EF6', '\u1EF4'};
  //tất cả ký tự viết thường tiếng Việt
  private final char[] LOWER_UNICODE = {'\u00E1', '\u00E0', '\u00E2', '\u0103', '\u00E3', '\u1EA5', '\u1EA7', '\u1EAF', '\u1EB1', '\u1EAB', '\u1EB5', '\u1EA3', '\u1EA9', '\u1EB3', '\u1EA1', '\u1EAD', '\u1EB7', '\u0111', '\u00E9', '\u00E8', '\u00EA', '\u1EBD', '\u1EBF', '\u1EC1', '\u1EC5', '\u1EBB', '\u1EC3', '\u1EB9', '\u1EC7', '\u00ED', '\u00EC', '\u0129', '\u1EC9', '\u1ECB', '\u00F3', '\u00F2', '\u00F4', '\u00F5', '\u1ED1', '\u1ED3', '\u1ED7', '\u1ECF', '\u01A1', '\u1ED5', '\u1ECD', '\u1EDB', '\u1EDD', '\u1EE1', '\u1ED9', '\u1EDF', '\u1EE3', '\u00FA', '\u00F9', '\u0169', '\u1EE7', '\u01B0', '\u1EE5', '\u1EE9', '\u1EEB', '\u1EEF', '\u1EED', '\u1EF1', '\u00FD', '\u1EF3', '\u1EF9', '\u1EF7', '\u1EF5'};
  //ký tự được xem là kết thúc câu
  private final char[] BOUNDARY_SYMBOL = {'.', '!', '?', '\u2026', '…', '-', '\n'};
  //ký tự trắng
  private final char WHITE_SPACE = ' ';
  //ký tự dấu ba chấm
  private final char ELLIPSIS = '\u2026';
  private final char NEWLINE_CHAR = '\n';

  //kiểm tra kí tự kết thúc câu
  Boolean isBoundarySymbol(char c) {
    for (char symbol : BOUNDARY_SYMBOL) {
      if (symbol == c)
        return true;
    }
    return false;
  }

  Boolean isUpper(char c) {
    if ('A' <= c && c <= 'Z')
      return true;

    for (char chr : UPPER_UNICODE) {
      if (chr == c)
        return true;
    }
    return false;
  }

  Boolean isLower(char c) {
    if ('a' <= c && c <= 'z')
      return true;

    for (char chr : LOWER_UNICODE) {
      if (chr == c)
        return true;
    }
    return false;
  }

  Boolean isNumber(char c) {
    return '0' <= c && c <= '9';
  }

  Boolean isBoundary(int i, String text) {
    if (i == 0) return false;

    if (i == text.length() - 1) return true;

    if (text.charAt(i+1) == WHITE_SPACE) return true;

    if (text.charAt(i) == NEWLINE_CHAR && text.charAt(i-1) != NEWLINE_CHAR) return true;

    if (text.charAt(i) == '-' && text.charAt(i-1) == WHITE_SPACE && text.charAt(i+1) == WHITE_SPACE) return true;

    if (isNumber(text.charAt(i-1)) && isNumber(text.charAt(i+1))) return false;

    if (isNumber(text.charAt(i-1)) && !isNumber(text.charAt(i+1))) return true;

    if (isLower(text.charAt(i-1)) && isUpper(text.charAt(i+1))) return true;

    if (isUpper(text.charAt(i-1)) && isUpper(text.charAt(i+1))) return true;

    return false;
  }
  /**
   * Initialize this vntokenizer.
   * @return this resource.
   * @throws ResourceInstantiationException if an error occurs during init.
   */
  public Resource init() throws ResourceInstantiationException {
    log.debug("SentenceSplitter is initializing");

    return this;
  }

  int addNewSentenceAnnotation(int originalContentCursor, int begin, int end, String content, String pContent, AnnotationSet annotationSet) {
    int len = 0;
    int pos = originalContentCursor;
    int cur = originalContentCursor;

    while (cur < content.length() && content.charAt(cur) != pContent.charAt(begin))
      cur += 1;

    while (begin < end) {
      while (pos < content.length() && content.charAt(pos) != pContent.charAt(begin))
        pos += 1;

      begin += 1;
    }

    FeatureMap featureMap = Factory.newFeatureMap();

    String sentence = content.substring(cur, pos);
    featureMap.put(TOKEN_STRING_FEATURE_NAME, sentence);
    featureMap.put(TOKEN_KIND_FEATURE_NAME, "sentence");
    featureMap.put(TOKEN_LENGTH_FEATURE_NAME, sentence.length());

    try {
      annotationSet.add((long) cur, (long) pos, "Sentence", featureMap);
    } catch (InvalidOffsetException e) {
      e.printStackTrace();
    }

    return pos - originalContentCursor;
  }

  /**
   * Execute this SentenceSplitter over the current document.
   * @throws ExecutionException if an error occurs during processing.
   */
  public void execute() throws ExecutionException {
    // check the interrupt flag before we start - in a long-running PR it is
    // good practice to check this flag at appropriate key points in the
    // execution, to allow the user to interrupt processing if it takes too
    // long.

    if(isInterrupted()) {
      throw new ExecutionInterruptedException("Execution of SentenceSplitter has been interrupted!");
    }
    interrupted = false;

    Document doc = getDocument();
    if(doc == null) {
      throw new ExecutionException("No document found");
    }

    AnnotationSet annotationSet;

    if (inputASName == null || inputASName.equals("")) {
      annotationSet = doc.getAnnotations();
    } else {
      annotationSet = doc.getAnnotations(inputASName);
    }

    fireStatusChanged("Splitting sentences " + doc.getName() + "...");

    String content = document.getContent().toString();
    String preprocessedContent = content.trim()
            .strip()
            .replaceAll("[ ]+", " ")
            .replaceAll("[.]+", ".")
            .replaceAll("[\n]+", "\n");


    int i = 0;
    int begin = 0;
    int originalContentCursor = 0;

    while (i < preprocessedContent.length()) {
      for (char sym : BOUNDARY_SYMBOL) {
        if (sym == preprocessedContent.charAt(i)) {
          if (isBoundary(i, preprocessedContent)) {
            while (begin < i && preprocessedContent.charAt(begin) == WHITE_SPACE)
              begin += 1;
            originalContentCursor = originalContentCursor + addNewSentenceAnnotation(originalContentCursor, begin, i+1, content, preprocessedContent, annotationSet) + 1;
            begin = i + 1;
          }
        }
      }
      i += 1;
    }
    fireProcessFinished();
    fireStatusChanged("Sentences split successfully");
  }

}

