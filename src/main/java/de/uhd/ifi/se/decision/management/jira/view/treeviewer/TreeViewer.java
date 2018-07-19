package de.uhd.ifi.se.decision.management.jira.view.treeviewer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;

import com.google.common.collect.ImmutableMap;

import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.Graph;
import de.uhd.ifi.se.decision.management.jira.model.GraphImpl;
import de.uhd.ifi.se.decision.management.jira.persistence.AbstractPersistenceStrategy;
import de.uhd.ifi.se.decision.management.jira.persistence.StrategyProvider;

/**
 * Creates tree viewer content
 */
public class TreeViewer {

	@XmlElement
	private boolean multiple;

	@XmlElement(name = "check_callback")
	private boolean checkCallback;

	@XmlElement
	private Map<String, Boolean> themes;

	@XmlElement
	private Set<Data> data;

	private Graph graph;
	private List<String> ids;
	private long index;

	public TreeViewer() {
	}

	public TreeViewer(String projectKey) {
		this.multiple = false;
		this.checkCallback = true;
		this.themes = ImmutableMap.of("icons", true);

		this.ids = new ArrayList<String>();
		this.index = 1;

		StrategyProvider strategyProvider = new StrategyProvider();
		AbstractPersistenceStrategy strategy = strategyProvider.getStrategy(projectKey);
		List<DecisionKnowledgeElement> decisions = strategy.getDecisions(projectKey);

		Set<Data> dataSet = new HashSet<Data>();
		for (DecisionKnowledgeElement decision : decisions) {
			dataSet.add(this.getDataStructure(decision));
		}
		this.data = dataSet;
	}

	public Data getDataStructure(DecisionKnowledgeElement decisionKnowledgeElement) {
		if (decisionKnowledgeElement == null) {
			return new Data();
		}
		this.graph = new GraphImpl(decisionKnowledgeElement);
		Data data = new Data(decisionKnowledgeElement);
		data = this.makeIdUnique(data);
		List<Data> children = this.getChildren(decisionKnowledgeElement);
		data.setChildren(children);
		return data;
	}

	private List<Data> getChildren(DecisionKnowledgeElement decisionKnowledgeElement) {
		List<Data> children = new ArrayList<>();
		List<DecisionKnowledgeElement> linkedElements = graph.getLinkedElements(decisionKnowledgeElement);
		for (DecisionKnowledgeElement element : linkedElements) {
			Data dataChild = new Data(element);
			dataChild = this.makeIdUnique(dataChild);
			List<Data> childrenOfElement = this.getChildren(element);
			dataChild.setChildren(childrenOfElement);
			children.add(dataChild);
		}
		return children;
	}

	private Data makeIdUnique(Data data) {
		if (!ids.contains(data.getId())) {
			ids.add(data.getId());
		} else {
			data.setId(index + data.getId());
			index++;
		}
		return data;
	}

	public boolean isMultiple() {
		return multiple;
	}

	public void setMultiple(boolean multiple) {
		this.multiple = multiple;
	}

	public boolean isCheckCallback() {
		return checkCallback;
	}

	public void setCheckCallback(boolean checkCallback) {
		this.checkCallback = checkCallback;
	}

	public Map<String, Boolean> getThemes() {
		return themes;
	}

	public void setThemes(Map<String, Boolean> themes) {
		this.themes = themes;
	}

	public Set<Data> getData() {
		return data;
	}

	public void setData(Set<Data> data) {
		this.data = data;
	}

	public List<String> getIds() {
		return ids;
	}

	public void setIds(List<String> ids) {
		this.ids = ids;
	}
}
