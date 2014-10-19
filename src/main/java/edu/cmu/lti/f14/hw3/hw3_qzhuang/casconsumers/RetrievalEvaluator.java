package edu.cmu.lti.f14.hw3.hw3_qzhuang.casconsumers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f14.hw3.hw3_qzhuang.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_qzhuang.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_qzhuang.utils.Utils;

/**
 * Use different similarity measure to find the document that is the most relevant to the query.
 * Compute the MRR across all queries and documents.
 **/
public class RetrievalEvaluator extends CasConsumer_ImplBase {

  /** query id number **/
  public ArrayList<Integer> qIdList;

  /** query and text relevant values **/
  public ArrayList<Integer> relList;

  /** text content **/
  public ArrayList<String> sentenceList;

  public void initialize() throws ResourceInitializationException {

    qIdList = new ArrayList<Integer>();

    relList = new ArrayList<Integer>();

    sentenceList = new ArrayList<String>();
    query = new HashMap<Integer, HashMap<String, Integer>>();
    docs = new HashMap<Integer, LinkedList<HashMap<String, Integer>>>();
    statistic = new HashMap<Integer, HashMap<String, Integer>>();
  }

  HashMap<Integer, HashMap<String, Integer>> query;

  HashMap<Integer, LinkedList<HashMap<String, Integer>>> docs;

  HashMap<Integer, HashMap<String, Integer>> statistic;

  /**
   * TODO :: 1. construct the global word dictionary 2. keep the word frequency for each sentence
   */
  @Override
  public void processCas(CAS aCas) throws ResourceProcessException {

    JCas jcas;
    try {
      jcas = aCas.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }

    FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();
    if (it.hasNext()) {
      Document doc = (Document) it.next();

      // Make sure that your previous annotators have populated this in CAS
      FSList fsTokenList = doc.getTokenList();
      ArrayList<Token> tokenList = Utils.fromFSListToCollection(fsTokenList, Token.class);

      qIdList.add(doc.getQueryID());
      relList.add(doc.getRelevanceValue());
      sentenceList.add(doc.getText());

      HashMap<String, Integer> freq = new HashMap<String, Integer>();
      int qID = doc.getQueryID();

      for (Token token : tokenList) {
        freq.put(token.getText(), token.getFrequency());
      }
      if (doc.getRelevanceValue() == 99) {
        query.put(doc.getQueryID(), new HashMap<String, Integer>(freq));
      } else {
        if (!docs.containsKey(doc.getQueryID())) {
          docs.put(doc.getQueryID(), new LinkedList<HashMap<String, Integer>>());
        }
        docs.get(doc.getQueryID()).add(new HashMap<String, Integer>(freq));
        if (!statistic.containsKey(qID)) {
          statistic.put(qID, new HashMap<String, Integer>());
        }
        for (Token token : tokenList) {
          HashMap<String, Integer> stringMap = statistic.get(qID);
          if (!stringMap.containsKey(token.getText())) {
            stringMap.put(token.getText(), token.getFrequency());
          } else {
            stringMap.put(token.getText(), stringMap.get(token.getText()) + token.getFrequency());
          }
        }
      }
    }

  }

  /**
   * For the convenience of ranking documents according to their similarity, this inner class stores
   * the document ID with respect to a particular query, the human-labled relavence, and the
   * similarity computed by different measures.
   * 
   * It implements the interface Comparable, so that we can use Collections.sort() to sort the
   * triples.
   **/
  public class Triple implements Comparable<Triple> {
    int Id;

    int rel;

    double Similarity;

    Triple(int Id, int rel, double Similarity) {
      this.Id = Id;
      this.rel = rel;
      this.Similarity = Similarity;
    }

    public double getCos() {
      return Similarity;
    }

    public int getId() {
      return Id;
    }

    @Override
    public int compareTo(Triple o) {
      if (this.Similarity - o.Similarity < 0)
        return 1;
      else if (this.Similarity - o.Similarity > 0)
        return -1;
      else
        return this.rel == 1 ? 0 : 1;
    }
  }

  LinkedList<Integer> ranks = new LinkedList<Integer>();

  private static final String reportPath = "report.txt";

  /**
   * Compute Cosine Similarity and rank the retrieved sentences Compute the MRR metric
   */
  @Override
  public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException,
          IOException {
    File report = new File(reportPath);
    report.createNewFile();
    FileWriter writer = new FileWriter(report);
    super.collectionProcessComplete(arg0);
    int qIndex = 0;
    while (qIndex < qIdList.size()) {
      int qId = qIdList.get(qIndex);
      int dIndex = 1;
      Vector<Triple> aDoc = new Vector<Triple>();
      while (qIndex + dIndex < qIdList.size() && qIdList.get(qIndex + dIndex) == qId) {
        double similarity = computeCosineSimilarity(query.get(qId), docs.get(qId).get(dIndex - 1));
        //double similarity = computeTfIdf(query.get(qId), docs.get(qId).get(dIndex - 1), qId);
        Triple p = new Triple(dIndex, relList.get(qIndex + dIndex), similarity);
        aDoc.add(p);
        dIndex++;
      }
      System.out.println("Query " + qId + ":" + sentenceList.get(qIndex) + '\n');
      Collections.sort(aDoc);
      for (Triple t : aDoc) {
        System.out.println("docId:" + t.Id + "\tcosine:" + t.Similarity + "\trel:" + t.rel);
        System.out.println(sentenceList.get(qIndex + t.Id) + '\n');
      }
      System.out.println("-------------------------------------");
      int rank = 1;
      for (Triple t : aDoc) {
        if (t.rel == 1)
          break;
        rank++;
      }
      Triple temp = aDoc.get(rank - 1);
      String line = "cosine=" + String.format("%.4f", temp.getCos()) + "\trank=" + rank + "\tqid="
              + qId + "\trel=1\t" + sentenceList.get(qIndex + temp.getId()) + '\n';
      // System.out.print(line);
      writer.write(line);
      ranks.add(rank);
      qIndex += dIndex;
    }
    double metric_mrr = compute_mrr();
    String mrrLine = "MRR=" + String.format("%.4f", metric_mrr);
    System.out.println(mrrLine);
    writer.write(mrrLine);
    writer.flush();
    writer.close();
  }

  /**
   * @return TfIdf similarity
   **/
  private double computeTfIdf(Map<String, Integer> queryVector, Map<String, Integer> docVector,
          int qID) {
    Iterator<String> wordInDoc = docVector.keySet().iterator();
    int maxFreq = -1;
    while (wordInDoc.hasNext()) {
      String word = (String) wordInDoc.next();
      maxFreq = Math.max(maxFreq, docVector.get(word));
    }
    Iterator<String> wordInQuery = queryVector.keySet().iterator();
    double tfidf = 0.0;
    while (wordInQuery.hasNext()) {
      String word = (String) wordInQuery.next();
      LinkedList<HashMap<String, Integer>> docList = docs.get(qID);
      int docContainingWord = 0;
      for (HashMap<String, Integer> r : docList) {
        if (r.containsKey(word))
          docContainingWord++;
      }
      double temp = (double) docList.size() / (double) (docContainingWord + 1.0);
      tfidf += (0.5 + 0.5 * (double) (docVector.containsKey(word) ? docVector.get(word) : 0)
              / (double) maxFreq)
              * Math.log(temp);
    }
    return tfidf;
  }

  /**
   * @return Jaccard coefficient similarity
   * 
   **/
  private double computeJaccardCoefficient(Map<String, Integer> queryVector,
          Map<String, Integer> docVector) {
    Iterator<String> wordInQuery = queryVector.keySet().iterator();
    int querySum = 0;
    while (wordInQuery.hasNext()) {
      String word = (String) wordInQuery.next();
      int freq = queryVector.get(word);
      querySum += freq;
    }
    Iterator<String> wordInDoc = docVector.keySet().iterator();
    int docSum = 0;
    while (wordInDoc.hasNext()) {
      String word = (String) wordInDoc.next();
      int freq = docVector.get(word);
      docSum += freq;
    }
    wordInQuery = queryVector.keySet().iterator();

    int intersect = 0;
    while (wordInQuery.hasNext()) {
      String word = (String) wordInQuery.next();
      if (docVector.containsKey(word)) {
        intersect += Math.min(docVector.get(word), queryVector.get(word));
      }
    }
    return (double) intersect / (double) (docSum + querySum - intersect);
  }

  /**
   * @return dice coefficient similarity
   **/
  private double computeDiceCoefficient(Map<String, Integer> queryVector,
          Map<String, Integer> docVector) {
    Iterator<String> wordInQuery = queryVector.keySet().iterator();
    int querySum = 0;
    while (wordInQuery.hasNext()) {
      String word = (String) wordInQuery.next();
      int freq = queryVector.get(word);
      querySum += freq;
    }
    Iterator<String> wordInDoc = docVector.keySet().iterator();
    int docSum = 0;
    while (wordInDoc.hasNext()) {
      String word = (String) wordInDoc.next();
      int freq = docVector.get(word);
      docSum += freq;
    }
    wordInQuery = queryVector.keySet().iterator();

    int intersect = 0;
    while (wordInQuery.hasNext()) {
      String word = (String) wordInQuery.next();
      if (docVector.containsKey(word)) {
        intersect += Math.min(docVector.get(word), queryVector.get(word));
      }
    }
    return (double) intersect / ((double) docSum + (double) querySum);
  }

  /**
   * 
   * @return cosine_similarity
   */
  private double computeCosineSimilarity(Map<String, Integer> queryVector,
          Map<String, Integer> docVector) {
    double cosine_similarity = 0.0;
    Iterator<String> wordInQuery = queryVector.keySet().iterator();
    int querySum = 0;
    while (wordInQuery.hasNext()) {
      String word = (String) wordInQuery.next();
      int freq = queryVector.get(word);
      querySum += freq * freq;
    }
    Iterator<String> wordInDoc = docVector.keySet().iterator();
    int docSum = 0;
    while (wordInDoc.hasNext()) {
      String word = (String) wordInDoc.next();
      int freq = docVector.get(word);
      docSum += freq * freq;
    }
    wordInQuery = queryVector.keySet().iterator();
    int up = 0;
    while (wordInQuery.hasNext()) {
      String word = (String) wordInQuery.next();
      if (docVector.containsKey(word)) {
        up += docVector.get(word) * queryVector.get(word);
      }
    }
    cosine_similarity = (double) up / (Math.sqrt((double) querySum) * Math.sqrt((double) docSum));
    return cosine_similarity;
  }

  /**
   * 
   * @return mrr
   */
  private double compute_mrr() {
    double metric_mrr = 0.0;
    int totalQ = ranks.size();
    for (Integer r : ranks) {
      metric_mrr += 1.0 / r;
    }
    metric_mrr /= (double) totalQ;
    return metric_mrr;
  }
}
