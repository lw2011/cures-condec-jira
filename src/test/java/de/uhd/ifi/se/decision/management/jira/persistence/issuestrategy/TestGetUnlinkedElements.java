package de.uhd.ifi.se.decision.management.jira.persistence.issuestrategy;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestGetUnlinkedElements extends TestIssueStrategySetUp {

	@Test
	public void testIdCannotBeFound() {
		assertEquals(numberOfElements, issueStrategy.getUnlinkedElements(0).size());
	}

	@Test
	public void testIdCanBeFound() {
		assertEquals(numberOfElements - 1, issueStrategy.getUnlinkedElements(15).size());
	}
}
