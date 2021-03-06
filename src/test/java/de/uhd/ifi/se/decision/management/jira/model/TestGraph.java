package de.uhd.ifi.se.decision.management.jira.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.atlassian.jira.exception.CreateException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;

import de.uhd.ifi.se.decision.management.jira.TestComponentGetter;
import de.uhd.ifi.se.decision.management.jira.TestSetUp;
import de.uhd.ifi.se.decision.management.jira.mocks.MockDefaultUserManager;
import de.uhd.ifi.se.decision.management.jira.mocks.MockTransactionTemplate;
import de.uhd.ifi.se.decision.management.jira.persistence.IssueStrategy;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.jdbc.NonTransactional;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;

@Data(TestSetUp.AoSentenceTestDatabaseUpdater.class) 
@RunWith(ActiveObjectsJUnitRunner.class)
public class TestGraph extends TestSetUp {

	private EntityManager entityManager;
	private Graph graph;
	private DecisionKnowledgeElement element;

	@Before
	public void setUp() throws CreateException {
		initialization();
		TestComponentGetter.init(new TestActiveObjects(entityManager), new MockTransactionTemplate(),
				new MockDefaultUserManager());
		element = new DecisionKnowledgeElementImpl(ComponentAccessor.getIssueManager().getIssueObject((long) 14));
		element.setProject(new DecisionKnowledgeProjectImpl("Test"));
		graph = new GraphImpl(element.getProject().getProjectKey(), element.getKey());
	}

	@Test
	public void testProjectKeyConstructor() {
		Graph graphRoot = new GraphImpl(element.getProject().getProjectKey());
		assertNotNull(graphRoot);
	}

	@Test
	public void testRootElementLinkDistConstructor() {
		Graph graphRoot = new GraphImpl(element.getProject().getProjectKey(), element.getKey());
		assertNotNull(graphRoot);
	}

	@Test
	public void testRootElementConstructor(){
		Graph graphRoot = new GraphImpl(element);
		assertNotNull(graphRoot);
	}

	@Test
	public void testGetLinkedElementsNull() {
		assertEquals(0, graph.getLinkedElements(null).size());
	}

	@Test
	@NonTransactional
	public void testGetLinkedElementsEmpty() {
		DecisionKnowledgeElement emptyElement = new DecisionKnowledgeElementImpl();
		assertEquals(0, graph.getLinkedElements(emptyElement).size());
	}

	@Test
	@NonTransactional
	public void testGetLinkedElementsFilled() {
		IssueStrategy issueStrategy = new IssueStrategy("TEST");
		ApplicationUser user = ComponentAccessor.getUserManager().getUserByName("NoFails");
		Project project = ComponentAccessor.getProjectManager().getProjectByCurrentKey("TEST");

		long i = 2;
		DecisionKnowledgeElementImpl decision = new DecisionKnowledgeElementImpl(5000, "TESTSummary", "TestDescription",
				KnowledgeType.DECISION, project.getKey(), "TEST-" + 5000);
		decision.setId(5000);

		issueStrategy.insertDecisionKnowledgeElement(decision, user);
		for (KnowledgeType type : KnowledgeType.values()) {
			LinkImpl link = new LinkImpl();
			link.setType("support");
			if (type != KnowledgeType.DECISION) {
				if (type.equals(KnowledgeType.ARGUMENT)) {
					DecisionKnowledgeElementImpl decisionKnowledgeElement = new DecisionKnowledgeElementImpl(i,
							"TESTSummary", "TestDescription", type, project.getKey(), "TEST-" + i);
					issueStrategy.insertDecisionKnowledgeElement(decisionKnowledgeElement, user);
					link.setSourceElement(decision.getId());
					link.setDestinationElement(decisionKnowledgeElement.getId());
					issueStrategy.insertLink(link, user);
				} else {
					DecisionKnowledgeElementImpl decisionKnowledgeElement = new DecisionKnowledgeElementImpl(i,
							"TESTSummary", "TestDescription", type, project.getKey(), "TEST-" + i);
					issueStrategy.insertDecisionKnowledgeElement(decisionKnowledgeElement, user);
					link.setType("attack");
					link.setDestinationElement(decision.getId());
					link.setSourceElement(decisionKnowledgeElement.getId());
					issueStrategy.insertLink(link, user);
				}
			}
			i++;
		}
		System.out.println(graph.getLinkedElements(decision).size());
	}

	@Test
	public void testGetLinkedElementsAndLinksNull(){
		assertEquals(0, graph.getLinkedElementsAndLinks(null).size());
	}

	@Test
	@NonTransactional
	public void testGetLinkedElementsAndLinksEmpty() {
		DecisionKnowledgeElement emptyElement = new DecisionKnowledgeElementImpl();
		assertEquals(0, graph.getLinkedElements(emptyElement).size());
	}

	@Test
	public void testSetRootElement() {
		DecisionKnowledgeElement element = new DecisionKnowledgeElementImpl();
		element.setId(123);
		element.setSummary("Test New Element");
		graph.setRootElement(element);
		assertEquals(element.getSummary(), graph.getRootElement().getSummary());
	}

	@Test
	public void testSetGetProject(){
		DecisionKnowledgeProject project = new DecisionKnowledgeProjectImpl("TEST-Set");
		graph.setProject(project);
		assertEquals("TEST-Set", graph.getProject().getProjectKey());
	}
}
