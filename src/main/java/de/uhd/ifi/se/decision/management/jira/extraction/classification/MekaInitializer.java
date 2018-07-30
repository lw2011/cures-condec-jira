package de.uhd.ifi.se.decision.management.jira.extraction.classification;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import de.uhd.ifi.se.decision.management.jira.ComponentGetter;
import de.uhd.ifi.se.decision.management.jira.extraction.model.Comment;
import de.uhd.ifi.se.decision.management.jira.extraction.model.Rationale;
import de.uhd.ifi.se.decision.management.jira.extraction.model.Sentence;
import meka.classifiers.multilabel.LC;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.tokenizers.NGramTokenizer;
import weka.core.tokenizers.Tokenizer;
import weka.filters.unsupervised.attribute.StringToWordVector;

public class MekaInitializer {

	public static Instances readDataFromARFF(String path) {
		BufferedReader reader = null;
		Instances structure = null;
		try {
			reader = new BufferedReader(new FileReader(path));
		} catch (FileNotFoundException e) {
			System.err.println("File: " + path + " not found");
			return null;
		}
		try {
			structure = new Instances(reader);
		} catch (IOException e) {
			System.err.println("File: " + path + " not readable");
			return null;
		}
		return structure;
	}

	private static Tokenizer getTokenizer() throws Exception {
		Tokenizer t = new NGramTokenizer();
		String[] options = weka.core.Utils.splitOptions(
				"weka.core.tokenizers.NGramTokenizer -max 3 -min 1 -delimiters \" \\r\\n\\t.,;:\\'\\\"()?!\"");
		t.setOptions(options);
		return t;
	}

	private static StringToWordVector getSTWV() throws Exception {
		StringToWordVector stwv = new StringToWordVector();
		stwv.setLowerCaseTokens(true);
		stwv.setIDFTransform(true);
		stwv.setTFTransform(true);
		stwv.setTokenizer(getTokenizer());
		stwv.setWordsToKeep(1000000);
		return stwv;
	}

	private static Attribute getAttribute(String name,int index) {
		ArrayList<String> rationaleAttribute = new ArrayList<String>();
		rationaleAttribute.add("0");
		rationaleAttribute.add("1");
		return new Attribute(name, rationaleAttribute,index);
	}

	private static Instances buildDataset(List<Comment> commentsList) {
		ArrayList<Attribute> wekaAttributes = new ArrayList<Attribute>();

		// Declare Class value with {0,1} as possible values
		wekaAttributes.add(getAttribute("isAlternative",0));
		wekaAttributes.add(getAttribute("isPro",1));
		wekaAttributes.add(getAttribute("isCon",2));
		wekaAttributes.add(getAttribute("isDecision",3));
		wekaAttributes.add(getAttribute("isIssue",4));

		// Declare text attribute to hold the message (free form text)
		Attribute attributeText = new Attribute("sentence", (List<String>) null,5);

		// Declare the feature vector
		wekaAttributes.add(attributeText);
		Instances data = new Instances("sentences: -C 5 ", wekaAttributes, 1000000);

		for (Comment comment : commentsList) {
			for (Sentence sentence : comment.getSentences()) {
				if (sentence.isRelevant()) {
					Instance newInstance = new DenseInstance(6);
					//The value is meka internal stored in a array similar structure, so if you read the value here, you likely get an index
					newInstance.setValue(attributeText, sentence.getBody());
					data.add(newInstance);
				}
			}
		}
		return data;
	}

	public static void classifySentencesFineGrained(List<Comment> commentsList) throws Exception {
		Instances structure = buildDataset(commentsList);

		structure.setClassIndex(5);
		// MLUtils.prepareData(structure);

		//Read model from supplied path
		String path = ComponentGetter.getUrlOfClassifierFolder() + "br.model";
		InputStream is = new URL(path).openStream();
		LC binaryRelevance = (LC) weka.core.SerializationHelper.read(is);

		//TODO: use data from active objects
		// Classify string instances
		List<double[]> results = new ArrayList<double[]>();
		for (int n = 0; n < structure.size(); n++) {
			Instance test = structure.get(n);
			double[] res = binaryRelevance.distributionForInstance(test);
			results.add(res);
		}

		// Write classification results back to sentence objects
		int i = 0;
		for (Comment comment : commentsList) {
			for (Sentence sentence : comment.getSentences()) {
				if (sentence.isRelevant()) {
					sentence.setClassification(Rationale.transferRationaleList(results.get(i)));
					i++;
				}
			}
		}
	}

}
