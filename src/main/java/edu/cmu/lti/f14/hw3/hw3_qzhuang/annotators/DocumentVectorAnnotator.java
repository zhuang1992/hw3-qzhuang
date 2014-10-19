package edu.cmu.lti.f14.hw3.hw3_qzhuang.annotators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.lti.f14.hw3.hw3_qzhuang.typesystems.*;
import edu.cmu.lti.f14.hw3.hw3_qzhuang.utils.MyLemmatizer;
import edu.cmu.lti.f14.hw3.hw3_qzhuang.utils.OpenNLPTokenization;
import edu.cmu.lti.f14.hw3.hw3_qzhuang.utils.RemoveStopWords;
import edu.cmu.lti.f14.hw3.hw3_qzhuang.utils.Utils;
/**
 * Identify the word and word frequency in each sentence. 
 * Store the word vector in the Document Type.
 **/
public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {

    FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
    if (iter.isValid()) {
      iter.moveToNext();
      Document doc = (Document) iter.get();
      createTermFreqVector(jcas, doc);
    }
  }

  /**
   * A basic white-space tokenizer, it deliberately does not split on punctuation!
   *
   * @param doc
   *          input text
   * @return a list of tokens.
   */

  List<String> tokenize0(String doc) {
    List<String> res = new ArrayList<String>();
    for (String s : doc.split("\\s+"))
      res.add(s);
    return res;
  }
  
  /**
   * 
   * @param jcas
   * @param doc
   */

  private void createTermFreqVector(JCas jcas, Document doc) {

    String docText = doc.getText();
    // TO DO: construct a vector of tokens and update the tokenList in CAS
    MyLemmatizer lemma = MyLemmatizer.getInstance();
    String newStr = lemma.lemmatize(docText);
    //System.out.println("newStr:"+newStr);
    OpenNLPTokenization tokenize = OpenNLPTokenization.getInstance();
    List<String> words = tokenize.tokenize(newStr);
    //List<String>words = lemma.lemmatize(docText);
    Map<String, Integer> count = new HashMap<String, Integer>();
    for (String s : words) {
      RemoveStopWords checker = RemoveStopWords.getInstance();
      if(checker.checkExistance(s))
        continue;
      if (count.containsKey(s)) {
        count.put(s, count.get(s) + 1);
      } else {
        count.put(s, 1);
      }
    }
    Iterator<String> iter = count.keySet().iterator();
    Collection<Token> tokenSet = new HashSet<Token>();
    while (iter.hasNext()) {
      String tokenText = iter.next();
      System.out.println("token:"+tokenText);
      Token newToken = new Token(jcas);
      newToken.setText(tokenText);
      newToken.setFrequency(count.get(tokenText));
      tokenSet.add(newToken);
    }
    System.out.println("\n");
    FSList docTokens = Utils.fromCollectionToFSList(jcas, tokenSet);
    doc.removeFromIndexes(jcas);
    doc.setTokenList(docTokens);
    doc.addToIndexes(jcas);
  }
}
