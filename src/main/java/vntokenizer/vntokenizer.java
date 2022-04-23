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

import java.io.IOException;

@CreoleResource(
    name = "vntokenizer",
    comment = "(A short one-line description of this PR, suitable for a tooltip in the GUI)")
public class vntokenizer extends AbstractLanguageAnalyser {

  private static final Logger log = Logger.getLogger(vntokenizer.class);

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

    log.info(content);
    log.info(length);

    FeatureMap newTokenFm;
    newTokenFm = Factory.newFeatureMap();
    String tokenString = content.substring(0, 10);

    log.info("Token: " + tokenString);

    newTokenFm.put(TOKEN_STRING_FEATURE_NAME, tokenString);
    newTokenFm.put(TOKEN_LENGTH_FEATURE_NAME, tokenString.length());
    try {
      annotationSet.add(0L, 10L, "DEFAULT_TOKEN", newTokenFm);
    } catch (InvalidOffsetException e) {
      e.printStackTrace();
    }

    fireProcessFinished();
    fireStatusChanged("Tokenized successfully");
  }

}

