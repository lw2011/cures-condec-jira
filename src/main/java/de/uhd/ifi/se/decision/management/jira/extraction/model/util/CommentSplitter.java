package de.uhd.ifi.se.decision.management.jira.extraction.model.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import com.atlassian.gzipfilter.org.apache.commons.lang.ArrayUtils;

import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.persistence.ConfigPersistence;

public class CommentSplitter {

	private List<Integer> startSubstringCount;

	private List<Integer> endSubstringCount;

	public static final String[] excludedTagList = new String[] { "{code}", "{quote}", "{noformat}","{panel}" };

	public static final String[] manualRationaleTagList = new String[] { "[issue]", "[decision]", "[alternative]",
			"[pro]", "[con]" };

	public static final String[] manualRationalIconList = new String[] { "(!)", "(/)", "(?)", "(y)", "(n)" };
	
	public static final String[] allExcluded = (String[]) ArrayUtils.addAll(excludedTagList, manualRationaleTagList);

	public CommentSplitter() {
		this.setStartSubstringCount(new ArrayList<Integer>());
		this.setEndSubstringCount(new ArrayList<Integer>());
	}

	public List<String> sliceCommentRecursionCommander(String body, String projectKey) {
		List<String> firstSplit = searchBetweenTagsRecursive(body, "{quote}", "{quote}", new ArrayList<String>());

		
		firstSplit = searchForFurtherTags(firstSplit, "{noformat}", "{noformat}");
		firstSplit = searchForFurtherTags(firstSplit, "{panel:", "{panel}");
		firstSplit = searchForFurtherTags(firstSplit, "{code:", "{code}");
		for (int i = 0; i < manualRationaleTagList.length; i++) {
			String tag = manualRationaleTagList[i];
			firstSplit = searchForFurtherTags(firstSplit, tag, tag.replace("[", "[/"));
		}
		if(ConfigPersistence.isIconParsingEnabled(projectKey)) {
			for (int i = 0; i < manualRationalIconList.length; i++) {
				firstSplit = searchForFurtherTags(firstSplit, manualRationalIconList[i], ".");
			}
		}

		return firstSplit;
	}

	private List<String> searchForFurtherTags(List<String> firstSplit, String openTag, String closeTag) {
		HashMap<Integer, ArrayList<String>> newSlices = new HashMap<Integer, ArrayList<String>>();
		for (String slice : firstSplit) {
			ArrayList<String> slicesOfSentence = searchBetweenTagsRecursive(slice.toLowerCase(), openTag.toLowerCase(), closeTag.toLowerCase(),
					new ArrayList<String>());
			if (slicesOfSentence.size() > 1) {
				newSlices.put(firstSplit.indexOf(slice), slicesOfSentence);
			}
		}
		for (int i = newSlices.keySet().toArray().length - 1; i >= 0; i--) {
			int remove = (int) newSlices.keySet().toArray()[i];
			firstSplit.remove(remove);
			firstSplit.addAll(remove, newSlices.get(remove));
		}

		return firstSplit;

	}

	private ArrayList<String> searchBetweenTagsRecursive(String toSearch, String openTag, String closeTag,
			ArrayList<String> slices) {
		//Icon is used to identify a sentence or a closing tag is forgotten
		if (toSearch.contains(openTag) && !toSearch.contains(closeTag)) {
			return slices;
		}//Open and close tags are existent
		if (toSearch.startsWith(openTag)) {
			String part = StringUtils.substringBetween(toSearch, openTag, closeTag);
			part = openTag + part + closeTag;
			slices.add(part);
			toSearch = toSearch.substring(toSearch.indexOf(openTag) + part.length());
			slices = searchBetweenTagsRecursive(toSearch, openTag, closeTag, slices);
		} else {// currently plain text
			if (toSearch.contains(openTag)) {// comment block has special text later
				slices.add(toSearch.substring(0, toSearch.indexOf(openTag)));
				slices = searchBetweenTagsRecursive(toSearch.substring(toSearch.indexOf(openTag)), openTag, closeTag,
						slices);
			} else {// comment block has no more special text
				slices.add(toSearch);
			}
		}
		return slices;
	}

	public List<Integer> getStartSubstringCount() {
		return startSubstringCount;
	}

	public void setStartSubstringCount(List<Integer> startSubstringCount) {
		this.startSubstringCount = startSubstringCount;
	}

	public List<Integer> getEndSubstringCount() {
		return endSubstringCount;
	}

	public void setEndSubstringCount(List<Integer> endSubstringCount) {
		this.endSubstringCount = endSubstringCount;
	}

	public void addSentenceIndex(int startIndex, int endIndex) {
		this.startSubstringCount.add(startIndex);
		this.endSubstringCount.add(endIndex);
	}

	public static String getKnowledgeTypeFromManuallIssueTag(String body, String projectKey) {
		boolean checkIcons = ConfigPersistence.isIconParsingEnabled(projectKey);
		if (body.toLowerCase().contains("[issue]") || (checkIcons && body.contains("(!)"))) {
			return KnowledgeType.ISSUE.toString();
		}
		if (body.toLowerCase().contains("[alternative]") || (checkIcons && body.contains("(?)"))) {
			return KnowledgeType.ALTERNATIVE.toString();
		}
		if (body.toLowerCase().contains("[decision]") || (checkIcons && body.contains("(/)"))) {
			return KnowledgeType.DECISION.toString();
		}
		if (body.toLowerCase().contains("[pro]") || (checkIcons && body.contains("y)"))) {
			return "pro";
		}
		if (body.toLowerCase().contains("[con]") || (checkIcons && body.contains("(n)"))) {
			return "con";
		}
		return KnowledgeType.OTHER.toString();
	}

}
