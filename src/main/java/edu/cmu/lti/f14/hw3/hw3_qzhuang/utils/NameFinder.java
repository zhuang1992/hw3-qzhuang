package edu.cmu.lti.f14.hw3.hw3_qzhuang.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.Span;

public class NameFinder {

  String locationModel = "src/main/resources/en-ner-location.bin";
  String personModel = "src/main/resources/en-ner-person.bin";
  private static NameFinder instance = null;
  NameFinderME nameFinder;
  TokenNameFinderModel locModel;
  TokenNameFinderModel perModel;
  public static NameFinder getInstance(){
    if(instance == null)
      instance = new NameFinder();
    return instance;
  }
  public boolean findLocation(List<String> s){
    nameFinder = new NameFinderME(locModel);
    String[] sentence = new String[s.size()];
    for(int i = 0; i < s.size(); i++){
      sentence[i] = new String(s.get(i));
    }
    Span nameSpans[] = nameFinder.find(sentence);
    System.out.println(nameSpans.length);
    for(int i = 0; i < nameSpans.length; i++){
      System.out.println("Span: "+nameSpans[i].toString());
      for(int j = nameSpans[i].getStart() ; j < nameSpans[i].getEnd(); j++){
        System.out.print(sentence[j]+" ");
      }        
      System.out.println();
    }
    nameFinder.clearAdaptiveData();
    return nameSpans.length!=0;
  }
  private NameFinder() {
    InputStream in = null;
    try {
      in = new FileInputStream(locationModel);
      locModel = new TokenNameFinderModel(in);
      in = new FileInputStream(personModel);
      perModel = new TokenNameFinderModel(in);
    } catch (FileNotFoundException e1) {
      e1.printStackTrace();
    } catch (InvalidFormatException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } 
  }
  public static void main(String[] args){
    NameFinder test = NameFinder.getInstance();
    OpenNLPTokenization tokenize = OpenNLPTokenization.getInstance();
    test.findLocation(tokenize.tokenize("From a single hamburger stand in San Bernardino, Calif., in 1948, the systematized approach that the McDonald brothers"));
  }
}
