package de.uhd.ifi.se.decision.documentation.jira.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.core.entity.GenericEntityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ErrorCollection;
import com.google.common.collect.ImmutableMap;

import de.uhd.ifi.se.decision.documentation.jira.decisionknowledge.DecisionKnowledgeElement;
import de.uhd.ifi.se.decision.documentation.jira.decisionknowledge.Link;
import de.uhd.ifi.se.decision.documentation.jira.util.KeyValuePairList;
import de.uhd.ifi.se.decision.documentation.jira.util.Pair;
import de.uhd.ifi.se.decision.documentation.jira.view.treants.Node;
import de.uhd.ifi.se.decision.documentation.jira.view.treeviewer.Core;
import de.uhd.ifi.se.decision.documentation.jira.view.treeviewer.Data;
import de.uhd.ifi.se.decision.documentation.jira.view.treeviewer.NodeInfo;

/**
 * @author Ewald Rode
 * @description Implements the IPersistenceStrategy interface. Uses JIRA issues
 *              to store decision knowledge
 */
public class IssueStrategy implements IPersistenceStrategy {
	private static final Logger LOGGER = LoggerFactory.getLogger(IssueStrategy.class);

	@Override
	// TODO Separate view and model (this method should not return Data object for
	// Treant)
	public Data createDecisionComponent(DecisionKnowledgeElement decisionElement, ApplicationUser user) {

		IssueInputParameters issueInputParameters = ComponentAccessor.getIssueService().newIssueInputParameters();

		issueInputParameters.setSummary(decisionElement.getName());
		issueInputParameters.setDescription(decisionElement.getDescription());
		issueInputParameters.setAssigneeId(user.getName());
		issueInputParameters.setReporterId(user.getName());

		Project project = ComponentAccessor.getProjectManager().getProjectByCurrentKey(decisionElement.getProjectKey());

		issueInputParameters.setProjectId(project.getId());
		String issueTypeId = getIssueTypeId(decisionElement.getType());
		issueInputParameters.setIssueTypeId(issueTypeId);

		IssueService issueService = ComponentAccessor.getIssueService();

		IssueService.CreateValidationResult result = issueService.validateCreate(user, issueInputParameters);
		if (result.getErrorCollection().hasAnyErrors()) {
			for (Map.Entry<String, String> entry : result.getErrorCollection().getErrors().entrySet()) {
				LOGGER.error(entry.getKey() + ": " + entry.getValue());
			}
			return null;
		} else {
			IssueResult issueResult = issueService.create(user, result);

			Data data = new Data();
			Issue issue = issueResult.getIssue();
			data.setText(issue.getKey() + " / " + issue.getSummary());
			data.setId(String.valueOf(issue.getId()));

			NodeInfo nodeInfo = new NodeInfo();
			nodeInfo.setId(Long.toString(issue.getId()));
			nodeInfo.setKey(issue.getKey());
			nodeInfo.setIssueType(issue.getIssueType().getName());
			nodeInfo.setDescription(issue.getDescription());
			nodeInfo.setSummary(issue.getSummary());
			data.setNodeInfo(nodeInfo);

			return data;
		}
	}

	@Override
	public Data editDecisionComponent(DecisionKnowledgeElement decisionElement, ApplicationUser user) {

		IssueService issueService = ComponentAccessor.getIssueService();

		IssueResult issueResult = issueService.getIssue(user, decisionElement.getId());
		MutableIssue issueToBeUpdated = issueResult.getIssue();
		IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
		issueInputParameters.setSummary(decisionElement.getName());
		issueInputParameters.setDescription(decisionElement.getDescription());
		IssueService.UpdateValidationResult result = issueService.validateUpdate(user, issueToBeUpdated.getId(),
				issueInputParameters);
		if (result.getErrorCollection().hasAnyErrors()) {
			for (Map.Entry<String, String> entry : result.getErrorCollection().getErrors().entrySet()) {
				LOGGER.error(entry.getKey() + ": " + entry.getValue());
			}
			return null;
		} else {
			issueResult = issueService.update(user, result);

			Data data = new Data();
			Issue issue = issueResult.getIssue();
			data.setText(issue.getKey() + " / " + issue.getSummary());
			data.setId(String.valueOf(issue.getId()));

			NodeInfo nodeInfo = new NodeInfo();
			nodeInfo.setId(Long.toString(issue.getId()));
			nodeInfo.setKey(issue.getKey());
			nodeInfo.setIssueType(issue.getIssueType().getName());
			nodeInfo.setDescription(issue.getDescription());
			nodeInfo.setSummary(issue.getSummary());
			data.setNodeInfo(nodeInfo);

			return data;
		}
	}

	@Override
	public boolean deleteDecisionComponent(DecisionKnowledgeElement decisionElement, ApplicationUser user) {
		IssueService issueService = ComponentAccessor.getIssueService();
		IssueService.IssueResult issue = issueService.getIssue(user, decisionElement.getId());
		if (issue.isValid()) {
			IssueService.DeleteValidationResult result = issueService.validateDelete(user, issue.getIssue().getId());
			if (result.getErrorCollection().hasAnyErrors()) {
				for (Map.Entry<String, String> entry : result.getErrorCollection().getErrors().entrySet()) {
					LOGGER.error(entry.getKey() + ": " + entry.getValue());
				}
				return false;
			} else {
				ErrorCollection errorCollection = issueService.delete(user, result);
				if (errorCollection.hasAnyErrors()) {
					return false;
				} else {
					return true;
				}
			}
		} else {
			return false;
		}
	}

	@Override
	public Long createLink(Link link, ApplicationUser user) {
		IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
		IssueLinkTypeManager issueLinkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager.class);
		Collection<IssueLinkType> issueLinkTypeCollection = issueLinkTypeManager
				.getIssueLinkTypesByName(link.getLinkType());
		Iterator<IssueLinkType> issueLinkTypeIterator = issueLinkTypeCollection.iterator();
		long typeId = 0;
		while (issueLinkTypeIterator.hasNext()) {
			IssueLinkType issueLinkType = issueLinkTypeIterator.next();
			typeId = issueLinkType.getId();
		}
		long sequence = 0;
		List<IssueLink> inwardIssueLinkList = issueLinkManager.getInwardLinks(link.getIngoingId());
		List<IssueLink> outwardIssueLinkList = issueLinkManager.getOutwardLinks(link.getIngoingId());
		for (IssueLink issueLink : inwardIssueLinkList) {
			if (sequence <= issueLink.getSequence()) {
				sequence = issueLink.getSequence() + 1;
			}
		}
		for (IssueLink issueLink : outwardIssueLinkList) {
			if (sequence <= issueLink.getSequence()) {
				sequence = issueLink.getSequence() + 1;
			}
		}
		try {
			issueLinkManager.createIssueLink(link.getOutgoingId(), link.getIngoingId(), typeId, sequence, user);
		} catch (CreateException e) {
			LOGGER.error("CreateException");
			return (long) 0;
		} catch (NullPointerException e) {
			LOGGER.error("NullPointerException");
			return (long) 0;
		} finally {
			outwardIssueLinkList = issueLinkManager.getOutwardLinks(link.getIngoingId());
			issueLinkManager.resetSequences(outwardIssueLinkList);
			inwardIssueLinkList = issueLinkManager.getInwardLinks(link.getIngoingId());
			issueLinkManager.resetSequences(inwardIssueLinkList);
		}
		IssueLink issueLink = issueLinkManager.getIssueLink(link.getOutgoingId(), link.getIngoingId(), typeId);
		if (issueLink == null) {
			LOGGER.error("issueLink == null");
			return (long) 0;
		}
		return issueLink.getId();
	}

	@Override
	public List<DecisionKnowledgeElement> getUnlinkedDecisionComponents(long id, String projectKey) {
		List<DecisionKnowledgeElement> unlinkedDecisionComponents = new ArrayList<DecisionKnowledgeElement>();
		ProjectManager projectManager = ComponentAccessor.getProjectManager();
		IssueManager issueManager = ComponentAccessor.getIssueManager();
		Project project = projectManager.getProjectObjByKey(projectKey);
		if (project != null) {
			Collection<Long> issueIds;
			try {
				issueIds = issueManager.getIssueIdsForProject(project.getId());
			} catch (GenericEntityException e) {
				issueIds = new ArrayList<Long>();
			}
			List<Long> issueIdList = new ArrayList<Long>();
			for (Long issueId : issueIds) {
				issueIdList.add(issueId);
			}
			// Make sure that only decision knowledge is included in list
			List<Issue> issueList = new ArrayList<Issue>();
			for (int index = 0; index < issueIdList.size(); ++index) {
				Issue issue = issueManager.getIssueObject(issueIdList.get(index));
				String issueType = issue.getIssueType().getName();
				// TODO Enable to configure decision knowledge types
				if (issueType.equals("Decision") || issueType.equals("Question") || issueType.equals("Issue")
						|| issueType.equals("Goal") || issueType.equals("Solution") || issueType.equals("Alternative")
						|| issueType.equals("Claim") || issueType.equals("Context") || issueType.equals("Assumption")
						|| issueType.equals("Constraint") || issueType.equals("Implication")
						|| issueType.equals("Assessment") || issueType.equals("Argument")) {
					issueList.add(issue);
				}
			}
			Issue parentIssue = issueManager.getIssueObject(id);
			if (parentIssue != null) {
				/*
				 * Fuelle das Result-Array mit Issues welche noch nicht mit dem parentIssue
				 * verlinkt sind
				 */
				for (int index = 0; index < issueList.size(); ++index) {
					Issue issue = issueList.get(index);
					/*
					 * Das Issue, von dem die Anfrage ausgeht, soll nicht in der Liste der
					 * moeglichen Issues auftauchen
					 */
					if (!issue.getId().equals(id)) {
						/* bereits verlinkte Issues sollen nicht nochmal in der Liste auftauchen */
						List<IssueLink> allOutwardIssueLink = ComponentAccessor.getIssueLinkManager()
								.getOutwardLinks(parentIssue.getId());
						List<Issue> outwardIssuesList = new ArrayList<Issue>();
						for (int counter = 0; counter < allOutwardIssueLink.size(); ++counter) {
							IssueLink issueLink = allOutwardIssueLink.get(counter);
							outwardIssuesList.add(issueLink.getDestinationObject());
						}
						boolean linked = false;
						for (int counter = 0; counter < outwardIssuesList.size(); ++counter) {
							Issue linkIssue = outwardIssuesList.get(counter);
							if (linkIssue != null) {
								if (linkIssue.equals(issue)) {
									linked = true;
								}
							}
						}
						if (!linked) {
							unlinkedDecisionComponents.add(new DecisionKnowledgeElement(issueList.get(index)));
						}
					}
				}
			}
		}
		return unlinkedDecisionComponents;
	}

	private String getIssueTypeId(String type) {
		ConstantsManager constantsManager = ComponentAccessor.getConstantsManager();
		Collection<IssueType> listOfIssueTypes = constantsManager.getAllIssueTypeObjects();
		for (IssueType iType : listOfIssueTypes) {
			if (iType.getName().equalsIgnoreCase(type)) {
				return iType.getId();
			}
		}
		return "";
	}

	@Override
	public List<DecisionKnowledgeElement> getDecisionsInProject(Project project) {
		IssueManager issueManager = ComponentAccessor.getIssueManager();
		Collection<Long> issueIds;
		if (project == null) {
			return null;
		}
		try {
			issueIds = issueManager.getIssueIdsForProject(project.getId());
		} catch (GenericEntityException e) {
			issueIds = new ArrayList<Long>();
		}
		List<DecisionKnowledgeElement> decisions = new ArrayList<DecisionKnowledgeElement>();
		for (Long issueId : issueIds) {
			Issue issue = issueManager.getIssueObject(issueId);
			if (issue != null && issue.getIssueType().getName().equals("Decision")) {
				decisions.add(new DecisionKnowledgeElement(issue));
			}
		}
		return decisions;
	}

	/*
	 * TreeViewerRest
	 */
	@Override
	public Core createCore(Project project) {
		Core core = new Core();
		core.setMultiple(false);
		core.setCheckCallback(true);
		core.setThemes(ImmutableMap.of("icons", false));
		HashSet<Data> dataSet = new HashSet<Data>();
		// List<Issue> issueList = this.getDecisionsInProject(project);
		List<DecisionKnowledgeElement> decisions = this.getDecisionsInProject(project);
		if (decisions == null) {
			return null;
		}
		for (int index = 0; index < decisions.size(); ++index) {
			KeyValuePairList.keyValuePairList = new ArrayList<Pair<String, String>>();
			Pair<String, String> kvp = new Pair<String, String>("root", decisions.get(index).getKey());
			KeyValuePairList.keyValuePairList.add(kvp);
			dataSet.add(createData(decisions.get(index)));

		}
		core.setData(dataSet);
		return core;
	}

	public List<Issue> getOutwardKnowledgeElements(DecisionKnowledgeElement decisionKnowledgeElement) {
		List<IssueLink> allOutwardIssueLink = ComponentAccessor.getIssueLinkManager()
				.getOutwardLinks(decisionKnowledgeElement.getId());
		List<Issue> outwardIssues = new ArrayList<Issue>();
		for (int i = 0; i < allOutwardIssueLink.size(); ++i) {
			IssueLink issueLink = allOutwardIssueLink.get(i);
			outwardIssues.add(issueLink.getDestinationObject());
		}
		return outwardIssues;
	}

	public List<Issue> getInwardKnowledgeElements(DecisionKnowledgeElement decisionKnowledgeElement) {
		List<IssueLink> allInwardIssueLink = ComponentAccessor.getIssueLinkManager()
				.getInwardLinks(decisionKnowledgeElement.getId());
		List<Issue> inwardIssues = new ArrayList<Issue>();
		for (int i = 0; i < allInwardIssueLink.size(); ++i) {
			IssueLink issueLink = allInwardIssueLink.get(i);
			inwardIssues.add(issueLink.getSourceObject());
		}
		return inwardIssues;
	}

	@Override
	public List<DecisionKnowledgeElement> getChildren(DecisionKnowledgeElement decisionKnowledgeElement) {
		List<Issue> outwardIssues = this.getOutwardKnowledgeElements(decisionKnowledgeElement);
		List<Issue> inwardIssues = this.getInwardKnowledgeElements(decisionKnowledgeElement);

		List<DecisionKnowledgeElement> children = new ArrayList<DecisionKnowledgeElement>();
		for (int i = 0; i < inwardIssues.size(); ++i) {
			if (inwardIssues.get(i).getIssueType().getName().equals("Argument")) {
				if (decisionKnowledgeElement != null & inwardIssues.get(i) != null) {
					Pair<String, String> newKVP = new Pair<String, String>(decisionKnowledgeElement.getKey(),
							inwardIssues.get(i).getKey());
					Pair<String, String> newKVPReverse = new Pair<String, String>(inwardIssues.get(i).getKey(),
							decisionKnowledgeElement.getKey());
					boolean boolvar = false;
					for (int counter = 0; counter < KeyValuePairList.keyValuePairList.size(); ++counter) {
						Pair<String, String> globalInst = KeyValuePairList.keyValuePairList.get(counter);
						if (newKVP.equals(globalInst)) {
							boolvar = true;
						}
					}
					if (!boolvar) {
						KeyValuePairList.keyValuePairList.add(newKVP);
						KeyValuePairList.keyValuePairList.add(newKVPReverse);
						children.add(new DecisionKnowledgeElement(inwardIssues.get(i)));
					}
				}
			}
		}
		for (int i = 0; i < outwardIssues.size(); ++i) {
			if (decisionKnowledgeElement != null & outwardIssues.get(i) != null) {
				Pair<String, String> newKVP = new Pair<String, String>(decisionKnowledgeElement.getKey(),
						outwardIssues.get(i).getKey());
				Pair<String, String> newKVPReverse = new Pair<String, String>(outwardIssues.get(i).getKey(),
						decisionKnowledgeElement.getKey());
				boolean boolvar = false;
				for (int counter = 0; counter < KeyValuePairList.keyValuePairList.size(); ++counter) {
					Pair<String, String> globalInst = KeyValuePairList.keyValuePairList.get(counter);
					if (newKVP.equals(globalInst)) {
						boolvar = true;
					}
				}
				if (!boolvar) {
					KeyValuePairList.keyValuePairList.add(newKVP);
					KeyValuePairList.keyValuePairList.add(newKVPReverse);
					children.add(new DecisionKnowledgeElement(outwardIssues.get(i)));
				}
			}
		}
		return children;
	}

	public Data createData(DecisionKnowledgeElement decisionKnowledgeElement) {
		if (decisionKnowledgeElement == null) {
			LOGGER.error("NullPointerException: createData Issue was NULL");
			return new Data();
		}
		Data data = new Data();

		data.setText(decisionKnowledgeElement.getKey() + " / " + decisionKnowledgeElement.getName());
		data.setId(String.valueOf(decisionKnowledgeElement.getId()));

		NodeInfo nodeInfo = new NodeInfo();
		nodeInfo.setId(Long.toString(decisionKnowledgeElement.getId()));
		nodeInfo.setKey(decisionKnowledgeElement.getKey());
		nodeInfo.setIssueType(decisionKnowledgeElement.getType());
		nodeInfo.setDescription(decisionKnowledgeElement.getDescription());
		nodeInfo.setSummary(decisionKnowledgeElement.getName());
		data.setNodeInfo(nodeInfo);

		List<DecisionKnowledgeElement> children = this.getChildren(decisionKnowledgeElement);

		List<Data> childrenToData = new ArrayList<Data>();
		for (DecisionKnowledgeElement child : children) {
			childrenToData.add(createData(child));
		}
		data.setChildren(childrenToData);

		return data;
	}

	private Node createNode(Issue issue, int depth, int currentDepth) {
		Node node = new Node();
		Map<String, String> nodeContent = ImmutableMap.of("name", issue.getSummary(), "title",
				issue.getIssueType().getName(), "desc", issue.getKey());
		node.setNodeContent(nodeContent);

		String htmlClass;
		String issueType = issue.getIssueType().getName().toLowerCase();
		if (issueType.equals("constraint") || issueType.equals("assumption") || issueType.equals("implication")
				|| issueType.equals("context")) {
			htmlClass = "context";
		} else if (issueType.equals("problem") || issueType.equals("issue") || issueType.equals("goal")) {
			htmlClass = "problem";
		} else if (issueType.equals("solution") || issueType.equals("claim") || issueType.equals("alternative")) {
			htmlClass = "solution";
		} else {
			htmlClass = "rationale";
		}
		node.setHtmlClass(htmlClass);

		long htmlId = issue.getId();
		node.setHtmlId(htmlId);

		if (currentDepth + 1 < depth) {
			List<Node> children = new ArrayList<Node>();
			List<Issue> toBeAddedToChildren = new ArrayList<Issue>();
			List<IssueLink> allOutwardIssueLink = ComponentAccessor.getIssueLinkManager()
					.getOutwardLinks(issue.getId());
			if (allOutwardIssueLink != null) {
				// this.children = new ArrayList<Node>();
				for (int i = 0; i < allOutwardIssueLink.size(); ++i) {
					IssueLink issueLink = allOutwardIssueLink.get(i);
					Issue issueLinkDestination = issueLink.getDestinationObject();
					/*
					 * Erstelle Parent-Child Beziehung und pruefe ob diese bereits in der
					 * KeyValuePair-Liste vorhanden ist. Wenn nein, fuege diesem Knoten Kinder hinzu
					 */
					if (issue != null & issueLinkDestination != null) {
						Pair<String, String> newKVP = new Pair<String, String>(issue.getKey(),
								issueLinkDestination.getKey());
						Pair<String, String> newKVPReverse = new Pair<String, String>(issueLinkDestination.getKey(),
								issue.getKey());
						boolean boolvar = false;
						for (int counter = 0; counter < KeyValuePairList.keyValuePairList.size(); ++counter) {
							Pair<String, String> globalInst = KeyValuePairList.keyValuePairList.get(counter);
							if (newKVP.equals(globalInst) || newKVPReverse.equals(globalInst)) {
								boolvar = true;
							}
						}
						if (!boolvar) {
							KeyValuePairList.keyValuePairList.add(newKVP);
							KeyValuePairList.keyValuePairList.add(newKVPReverse);
							toBeAddedToChildren.add(issueLinkDestination);
						}
					}
				}
			}

			List<IssueLink> allInwardIssueLink = ComponentAccessor.getIssueLinkManager().getInwardLinks(issue.getId());
			if (allInwardIssueLink != null) {
				for (int i = 0; i < allInwardIssueLink.size(); ++i) {
					IssueLink issueLink = allInwardIssueLink.get(i);
					Issue issueLinkDestination = issueLink.getSourceObject();
					/*
					 * Erstelle Parent-Child Beziehung und pruefe ob diese bereits in der
					 * KeyValuePair-Liste vorhanden ist. Wenn nein, fuege diesem Knoten Kinder hinzu
					 */
					if (issue != null & issueLinkDestination != null) {
						Pair<String, String> newKVP = new Pair<String, String>(issue.getKey(),
								issueLinkDestination.getKey());
						Pair<String, String> newKVPReverse = new Pair<String, String>(issueLinkDestination.getKey(),
								issue.getKey());
						boolean boolvar = false;
						for (int counter = 0; counter < KeyValuePairList.keyValuePairList.size(); ++counter) {
							Pair<String, String> globalInst = KeyValuePairList.keyValuePairList.get(counter);
							if (newKVP.equals(globalInst) || newKVPReverse.equals(globalInst)) {
								boolvar = true;
							}
						}
						if (!boolvar) {
							KeyValuePairList.keyValuePairList.add(newKVP);
							KeyValuePairList.keyValuePairList.add(newKVPReverse);
							toBeAddedToChildren.add(issueLinkDestination);
						}
					}
				}
			}
			for (int index = 0; index < toBeAddedToChildren.size(); ++index) {
				children.add(createNode(toBeAddedToChildren.get(index), depth, currentDepth + 1));
			}
			node.setChildren(children);
		}
		return node;
	}

	/* TreantsRest */
	@Override
	public Node createNodeStructure(String key, int depth) {
		IssueManager issueManager = ComponentAccessor.getIssueManager();
		Issue issue = issueManager.getIssueByCurrentKey(key);
		Node node = new Node();
		Map<String, String> nodeContent = ImmutableMap.of("name", issue.getSummary(), "title",
				issue.getIssueType().getName(), "desc", issue.getKey());
		node.setNodeContent(nodeContent);

		String htmlClass;
		String issueType = issue.getIssueType().getName().toLowerCase();
		if (issueType.equals("constraint") || issueType.equals("assumption") || issueType.equals("implication")
				|| issueType.equals("context")) {
			htmlClass = "context";
		} else if (issueType.equals("problem") || issueType.equals("issue") || issueType.equals("goal")) {
			htmlClass = "problem";
		} else if (issueType.equals("solution") || issueType.equals("claim") || issueType.equals("alternative")) {
			htmlClass = "solution";
		} else {
			htmlClass = "rationale";
		}
		node.setHtmlClass(htmlClass);

		long htmlId = issue.getId();
		node.setHtmlId(htmlId);

		List<Node> children = new ArrayList<Node>();
		List<IssueLink> allOutwardIssueLink = ComponentAccessor.getIssueLinkManager().getOutwardLinks(issue.getId());
		KeyValuePairList.keyValuePairList = new ArrayList<Pair<String, String>>();
		if (allOutwardIssueLink != null) {
			if (allOutwardIssueLink.size() > 0) {
				for (int i = 0; i < allOutwardIssueLink.size(); i++) {
					IssueLink issueLink = allOutwardIssueLink.get(i);
					Issue issueLinkDestination = issueLink.getDestinationObject();
					if (issue != null & issueLinkDestination != null) {
						KeyValuePairList.keyValuePairList
								.add(new Pair<String, String>(issue.getKey(), issueLinkDestination.getKey()));
						KeyValuePairList.keyValuePairList
								.add(new Pair<String, String>(issueLinkDestination.getKey(), issue.getKey()));
						children.add(createNode(issueLinkDestination, depth, 0));
					}
				}
			}
		}
		List<IssueLink> allInwardIssueLink = ComponentAccessor.getIssueLinkManager().getInwardLinks(issue.getId());
		if (allInwardIssueLink != null) {
			if (allInwardIssueLink.size() > 0) {
				for (int i = 0; i < allInwardIssueLink.size(); i++) {
					IssueLink issueLink = allInwardIssueLink.get(i);
					Issue issueLinkDestination = issueLink.getSourceObject();
					if (issue != null & issueLinkDestination != null) {
						Pair<String, String> kvp = new Pair<String, String>(issue.getKey(),
								issueLinkDestination.getKey());
						Pair<String, String> kvp2 = new Pair<String, String>(issueLinkDestination.getKey(),
								issue.getKey());
						KeyValuePairList.keyValuePairList.add(kvp);
						KeyValuePairList.keyValuePairList.add(kvp2);
						children.add(createNode(issueLinkDestination, depth, 0));
					}
				}
			}
		}
		node.setChildren(children);
		return node;
	}
}