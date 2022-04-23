package vntokenizer;

import gate.Factory;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ResourceInstantiationException;
import gate.test.GATEPluginTests;
import org.junit.Test;
import static org.junit.Assert.*;


/**
 * Using this class automatically prepares GATE and the plugin for testing.
 * 
 * This class automatically initializes GATE and loads the plugin. 
 * Any method in this class with the "@Test" annotation will then get
 * run with the plugin already properly loaded.
 * 
 */
public class Testvntokenizer extends GATEPluginTests {

  @Test
  public void testSomething() throws ResourceInstantiationException {
    AbstractLanguageAnalyser pr = (AbstractLanguageAnalyser) Factory.createResource("vntokenizer.vntokenizer");
    try {
      // testing code goes here
    } finally {
      Factory.deleteResource(pr);
    }
  }
}
