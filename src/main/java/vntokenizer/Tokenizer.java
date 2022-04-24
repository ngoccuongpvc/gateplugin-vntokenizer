package vntokenizer;

import gate.*;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ExecutionInterruptedException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.*;

import gate.util.InvalidOffsetException;
import org.apache.log4j.Logger;
import vn.pipeline.VnCoreNLP;
import vn.pipeline.Word;

import java.io.IOException;
import java.text.Normalizer;

@CreoleResource(
    name = "vntokenizer",
    comment = "(A short one-line description of this PR, suitable for a tooltip in the GUI)")
public class Tokenizer extends AbstractLanguageAnalyser {

  private static final Logger log = Logger.getLogger(Tokenizer.class);

  /**
   * Annotation set name from which this PR will take its input annotations.
   */
  private String inputASName;

  /**
   * Annotation set name into which this PR will create new annotations.
   */
  private String outputASName;


  public String getInputASName() {
    return inputASName;
  }

  @Optional
  @RunTime
  @CreoleParameter(comment = "The annotation set used for input annotations")
  public void setInputASName(String inputASName) {
    this.inputASName = inputASName;
  }

  public String getOutputASName() {
    return outputASName;
  }

  @Optional
  @RunTime
  @CreoleParameter(comment = "The annotation set used for output annotations")
  public void setOutputASName(String outputASName) {
    this.outputASName = outputASName;
  }

  /**
   * Initialize this vntokenizer.
   * @return this resource.
   * @throws ResourceInstantiationException if an error occurs during init.
   */
  public Resource init() throws ResourceInstantiationException {
    log.debug("vntokenizer is initializing");

    // your initialization code here

    return this;
  }

  /**
   * Execute this vntokenizer over the current document.
   * @throws ExecutionException if an error occurs during processing.
   */
  public void execute() throws ExecutionException {
    // check the interrupt flag before we start - in a long-running PR it is
    // good practice to check this flag at appropriate key points in the
    // execution, to allow the user to interrupt processing if it takes too
    // long.

    if(isInterrupted()) {
      throw new ExecutionInterruptedException("Execution of vntokenizer has been interrupted!");
    }
    interrupted = false;

    Document doc = getDocument();
    if(doc == null) {
      throw new ExecutionException("No document to tokenize");
    }

    AnnotationSet annotationSet;

    if (inputASName == null || inputASName.equals("")) {
      annotationSet = doc.getAnnotations();
    } else {
      annotationSet = doc.getAnnotations(inputASName);
    }

    fireStatusChanged("Tokenizing " + doc.getName() + "...");

    String content = document.getContent().toString();

    int length = content.length();
    String noTonedContent = Normalizer.normalize(content, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    String[] annotators = {"wseg"};
    try {
      VnCoreNLP pipeline = new VnCoreNLP(annotators);
      vn.pipeline.Annotation annotation = new vn.pipeline.Annotation(content);
      pipeline.annotate(annotation);

      int posDoc = 0;

      for (Word word : annotation.getWords()) {
        while (posDoc < length && noTonedContent.charAt(posDoc) == ' ') {
          FeatureMap newTokenFm = Factory.newFeatureMap();
          newTokenFm.put(TOKEN_STRING_FEATURE_NAME, " ");
          newTokenFm.put(TOKEN_LENGTH_FEATURE_NAME, 1);
          newTokenFm.put(TOKEN_KIND_FEATURE_NAME, "space");
          try {
            annotationSet.add((long) posDoc, (long) (posDoc + 1), "SpaceToken", newTokenFm);
          } catch (InvalidOffsetException e) {
            e.printStackTrace();
          }
          posDoc += 1;
        }
        String form = word.getForm();
        String noTonedForm = Normalizer.normalize(form, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        while (posDoc < length && noTonedContent.charAt(posDoc) != noTonedForm.charAt(0)) {
          posDoc += 1;
        }

        int i = 0;
        int j = 0;
        while (j < form.length()) {
          while (posDoc + i < length &&
                  noTonedContent.charAt(posDoc + i) == ' ' ||
                  noTonedContent.charAt(posDoc + i) == '\n'
          ) {
            FeatureMap newTokenFm = Factory.newFeatureMap();
            newTokenFm.put(TOKEN_STRING_FEATURE_NAME, "_");
            newTokenFm.put(TOKEN_LENGTH_FEATURE_NAME, 1);
            newTokenFm.put(TOKEN_KIND_FEATURE_NAME, "underscore");
            try {
              annotationSet.add((long) posDoc + i, (long) (posDoc + i + 1), "UnderscoreToken", newTokenFm);
            } catch (InvalidOffsetException e) {
              e.printStackTrace();
            }
            i += 1;
          }
          if (posDoc + i < length && noTonedContent.charAt(posDoc + i) == noTonedForm.charAt(j)) {
            i += 1;
          }
          j += 1;
        }

        FeatureMap newTokenFm = Factory.newFeatureMap();
        newTokenFm.put(TOKEN_STRING_FEATURE_NAME, form);
        newTokenFm.put(TOKEN_LENGTH_FEATURE_NAME, form.length());

        if (",;:.!?'`\"-/()[]*".contains(form)) {
          newTokenFm.put(TOKEN_KIND_FEATURE_NAME, "punctuation");
        } else {
          newTokenFm.put(TOKEN_KIND_FEATURE_NAME, "word");
        }

        try {
          annotationSet.add((long) posDoc, (long) (posDoc + i), "Token", newTokenFm);
        } catch (InvalidOffsetException e) {
          e.printStackTrace();
        }

        posDoc += i;
      }

    } catch (IOException e) {
      e.printStackTrace();
    }


    fireProcessFinished();
    fireStatusChanged("Tokenized successfully");
  }

}

