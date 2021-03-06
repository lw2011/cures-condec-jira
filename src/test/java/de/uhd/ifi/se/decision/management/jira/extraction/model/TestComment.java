package de.uhd.ifi.se.decision.management.jira.extraction.model;

import static org.junit.Assert.*;

import java.util.Locale;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.issuetype.MockIssueType;
import com.atlassian.jira.mock.issue.MockIssue;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;

import de.uhd.ifi.se.decision.management.jira.TestComponentGetter;
import de.uhd.ifi.se.decision.management.jira.TestSetUp;
import de.uhd.ifi.se.decision.management.jira.extraction.model.impl.CommentImpl;
import de.uhd.ifi.se.decision.management.jira.extraction.persistence.ActiveObjectsManager;
import de.uhd.ifi.se.decision.management.jira.extraction.persistence.DecisionKnowledgeInCommentEntity;
import de.uhd.ifi.se.decision.management.jira.extraction.persistence.LinkBetweenDifferentEntitiesEntity;
import de.uhd.ifi.se.decision.management.jira.mocks.MockDefaultUserManager;
import de.uhd.ifi.se.decision.management.jira.mocks.MockTransactionTemplate;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.jdbc.DatabaseUpdater;
import net.java.ao.test.jdbc.NonTransactional;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(TestComment.AoSentenceTestDatabaseUpdater.class)
public class TestComment extends TestSetUp {

	private EntityManager entityManager;
	private MockIssue issue;

	private com.atlassian.jira.issue.comments.Comment comment1;

	@Before
	public void setUp() {
		initialization();
		TestComponentGetter.init(new TestActiveObjects(entityManager), new MockTransactionTemplate(),
				new MockDefaultUserManager());
		createLocalIssue();
	}

	private void createLocalIssue() {
		Project project = ComponentAccessor.getProjectManager().getProjectByCurrentKey("TEST");
		issue = new MockIssue(30, "TEST-" + 30);
		((MockIssue) issue).setProjectId(project.getId());
		issue.setProjectObject(project);
		IssueType issueType = new MockIssueType(1, KnowledgeType.DECISION.toString().toLowerCase(Locale.ENGLISH));
		issue.setIssueType(issueType);
		issue.setSummary("Test");
	}

	private void addCommentsToIssue(String comment) {

		ComponentAccessor.getCommentManager().deleteCommentsForIssue(issue);
		// Get the current logged in user
		ApplicationUser currentUser = ComponentAccessor.getUserManager().getUser("NoFails");
		// Get access to the Jira comment and component manager
		CommentManager commentManager = ComponentAccessor.getCommentManager();
		// Get the last comment entered in on the issue to a String
		comment1 = commentManager.create(issue, currentUser, comment, true);

	}

	private CommentImpl getComment(String text) {
		createLocalIssue();

		addCommentsToIssue(text);
		return new CommentImpl(comment1);
	}

	@Test
	public void testConstructor() {
		assertNotNull(new CommentImpl());
	}

	@Test
	public void testSentencesAreNotNull() {
		assertNotNull(new CommentImpl().getSentences());
	}

	@Test
	@NonTransactional
	public void testCommentIsCreated() {
		assertNotNull(getComment("this is a test Sentence. With two sentences"));
	}

	@Test
	@NonTransactional
	public void testCommentWithOneQuote() {
		CommentImpl comment = getComment("{quote} this is a quote {quote} and this is a test Sentence.");
		assertNotNull(comment);
		assertEquals(2, comment.getSentences().size());
	}

	@Test
	@NonTransactional
	public void testCommentWithOneQuoteAtTheBack() {
		CommentImpl comment = getComment("and this is a test Sentence. {quote} this is a quote {quote} ");
		assertNotNull(comment);
		assertEquals(2, comment.getSentences().size());
	}

	@Test
	@NonTransactional
	public void testCommentWithTwoQuotes() {
		CommentImpl comment = getComment(
				"{quote} this is a quote {quote} and this is a test Sentence. {quote} this is a second quote {quote} ");
		assertNotNull(comment);
		assertEquals(3, comment.getSentences().size());
	}

	@Test
	@NonTransactional
	public void testGetTaggedBody() {
		CommentImpl comment = getComment(
				"{quote} this is a quote {quote} and this is a test Sentence. {quote} this is a second quote {quote} ");
		assertNotNull(comment.getTaggedBody(0));
		assertTrue(comment.getTaggedBody(0).contains("span"));
	}

	@Test
	@NonTransactional
	public void TestSentenceSplitWithDifferentQuotes() {
		CommentImpl comment = getComment("{quote} this is a quote {quote} and this is a test Sentence.");
		assertEquals(2, comment.getSentences().size());

		comment = getComment(
				"{quote} this is a quote {quote} and this is a test Sentence. {quote} this is a second quote {quote} ");
		assertEquals(3, comment.getSentences().size());

	
	}
	
	@Test
	@NonTransactional
	public void TestSentenceSplitWithDifferentQuotes2() {
		CommentImpl comment =  getComment(
				"{quote} this is a quote {quote} and this is a test Sentence. {quote} this is a second quote {quote} and a Sentence at the back");
		assertEquals(4, comment.getSentences().size());

		comment = getComment(
				"{quote} this is a quote {quote} {quote} this is a second quote right after the first one {quote} and a Sentence at the back");
		assertEquals(3, comment.getSentences().size());

		comment = getComment(
				"{quote} this is a quote {quote} {quote} this is a second quote right after the first one {quote} {quote} These are many quotes {quote}");
		assertEquals(3, comment.getSentences().size());
		
	}

	@Test
	@NonTransactional
	public void TestSentenceSplitWithNoformats() {
		CommentImpl comment = getComment("{noformat} this is a noformat {noformat} and this is a test Sentence.");
		assertEquals(2, comment.getSentences().size());

		comment = getComment(
				"{noformat} this is a noformat {noformat} and this is a test Sentence. {noformat} this is a second noformat {noformat} ");
		assertEquals(3, comment.getSentences().size());

	}
	@Test
	@NonTransactional
	public void TestSentenceSplitWithNoformats2() {
		CommentImpl comment = getComment(
				"{noformat} this is a noformat {noformat} and this is a test Sentence. {noformat} this is a second noformat {noformat} and a Sentence at the back");
		assertEquals(4, comment.getSentences().size());

		comment = getComment(
				"{noformat} this is a noformat {noformat} {noformat} this is a second noformat right after the first one {noformat} and a Sentence at the back");
		assertEquals(3, comment.getSentences().size());

		comment = getComment(
				"{noformat} this is a noformat {noformat} {noformat} this is a second noformat right after the first one {noformat} {noformat} These are many noformats {noformat}");
		assertEquals(3, comment.getSentences().size());
	}

	@Test
	@NonTransactional
	public void TestSentenceSplitWithNoformatsAndQuotes() {
		CommentImpl comment = getComment(
				"{noformat} this is a noformat {noformat} {quote} and this is a test Sentence.{quote}");
		assertEquals(2, comment.getSentences().size());

		comment = getComment(
				"{noformat} this is a noformat {noformat} and this is a test Sentence. {quote} this is a also a quote {quote} ");
		assertEquals(3, comment.getSentences().size());

		comment = getComment(
				"{noformat} this is a noformat {noformat} and this is a test Sentence. {quote} this is a also a quote {quote}{quote} this is a also a quote {quote} ");
		assertEquals(4, comment.getSentences().size());

		comment = getComment(
				"{noformat} this is a noformat {noformat} and this is a test Sentence. {quote} this is a also a quote {quote}{quote} this is a also a quote {quote} {noformat} this is a noformat {noformat} and this is a test Sentence.");
		assertEquals(6, comment.getSentences().size());

		comment = getComment(
				"{noformat} this is a first noformat {noformat} and this is a second test Sentence. {quote} this is a also a third quote {quote}{quote} this is a also a fourth quote {quote} {noformat} this is a fifth noformat {noformat} and this is a sixth test Sentence.");
		assertEquals(6, comment.getSentences().size());

		comment = getComment(
				"{noformat} this is a noformat {noformat} and this is a test Sentence. {noformat} this is a second noformat {noformat} and a Sentence at the back");
		assertEquals(4, comment.getSentences().size());

		comment = getComment(
				"{noformat} this is a noformat {noformat} {noformat} this is a second noformat right after the first one {noformat} and a Sentence at the back");
		assertEquals(3, comment.getSentences().size());

		comment = getComment(
				"{noformat} this is a noformat {noformat} {noformat} this is a second noformat right after the first one {noformat} {noformat} These are many noformats {noformat}");
		assertEquals(3, comment.getSentences().size());
	}

	@Test
	@NonTransactional
	public void TestSentenceOrder() {
		CommentImpl comment = getComment(
				"{noformat} this is a first noformat {noformat} and this is a second test Sentence. {quote} this is a also a third quote {quote}{quote} this is a also a fourth quote {quote} {noformat} this is a fifth noformat {noformat} and this is a sixth test Sentence.");
		assertEquals(6, comment.getSentences().size());
		assertTrue(comment.getSentences().get(0).getBody().contains("first"));
		assertTrue(comment.getSentences().get(1).getBody().contains("second"));
		assertTrue(comment.getSentences().get(2).getBody().contains("third"));
		assertTrue(comment.getSentences().get(3).getBody().contains("fourth"));
		assertTrue(comment.getSentences().get(4).getBody().contains("fifth"));
		assertTrue(comment.getSentences().get(5).getBody().contains("sixth"));
	}

	@Test
	@NonTransactional
	public void TestSentenceSplitWithUnknownTag() {
		CommentImpl comment = getComment(
				"{noformat} this is a noformat {noformat} {wuzl} and this is a test Sentence {wuzl}");
		assertEquals(2, comment.getSentences().size());
		assertTrue(comment.getSentences().get(0).getBody().contains("{noformat} this is a noformat {noformat}"));
	}

	@Test
	@NonTransactional
	public void TestSentenceSplitWithCodeTag() {
		CommentImpl comment = getComment("{code:Java} int i = 0 {code} and this is a test Sentence.");
		assertEquals(2, comment.getSentences().size());

		comment = getComment(
				"{code:java} this is a code {code} and this is a test Sentence. {quote} this is a also a quote {quote} ");
		assertEquals(3, comment.getSentences().size());

		comment = getComment(
				"{code:java} this is a code {code} and this is a test Sentence. {quote} this is a also a quote {quote}{quote} this is a also a quote {quote} ");
		assertEquals(4, comment.getSentences().size());

		comment = getComment(
				"{code:java} this is a code {code} and this is a test Sentence. {quote} this is a also a quote {quote}{quote} this is a also a quote {quote} {code:java} this is a code {code} and this is a test Sentence.");
		assertEquals(6, comment.getSentences().size());

		comment = getComment(
				"{code:java} this is a first code {code} and this is a second test Sentence. {quote} this is a also a third quote {quote}{quote} this is a also a fourth quote {quote} {code:java} this is a fifth code {code} and this is a sixth test Sentence.");
		assertEquals(6, comment.getSentences().size());

		comment = getComment(
				"{code:java} this is a code {code} and this is a test Sentence. {code:java} this is a second code {code} and a Sentence at the back");
		assertEquals(4, comment.getSentences().size());

		comment = getComment(
				"{code:java} this is a code {code} {code:java} this is a second code right after the first one {code} and a Sentence at the back");
		assertEquals(3, comment.getSentences().size());

		comment = getComment(
				"{code:java} this is a code {code} {code:java} this is a second code right after the first one {code} {code:java} These are many codes {code}");
		assertEquals(3, comment.getSentences().size());
	}
	
	@Test
	@NonTransactional
	public void TestPropertiesOfCodeElementedText() {
		CommentImpl comment = getComment("{code:Java} int i = 0 {code} and this is a test Sentence.");
		assertEquals(2, comment.getSentences().size());
		assertEquals(false, comment.getSentences().get(0).isRelevant());
		assertEquals(false, comment.getSentences().get(0).isPlainText());
		assertEquals(false, comment.getSentences().get(0).isTagged());
		assertEquals(false, comment.getSentences().get(0).isTaggedManually());
	}
	
	@Test
	@NonTransactional
	public void TestPropertiesOfNoFormatElementedText() {
		CommentImpl comment = getComment("{noformat} int i = 0 {noformat} and this is a test Sentence.");
		assertEquals(2, comment.getSentences().size());
		assertEquals(false, comment.getSentences().get(0).isRelevant());
		assertEquals(false, comment.getSentences().get(0).isPlainText());
		assertEquals(false, comment.getSentences().get(0).isTagged());
		assertEquals(false, comment.getSentences().get(0).isTaggedManually());
	}
	
	@Test
	@NonTransactional
	public void TestPropertiesOfQuotedElementedText() {
		CommentImpl comment = getComment("{quote} int i = 0 {quote} and this is a test Sentence.");
		assertEquals(2, comment.getSentences().size());
		assertEquals(false, comment.getSentences().get(0).isRelevant());
		assertEquals(false, comment.getSentences().get(0).isPlainText());
		assertEquals(false, comment.getSentences().get(0).isTagged());
		assertEquals(false, comment.getSentences().get(0).isTaggedManually());
	}
	
	@Test
	@NonTransactional
	public void TestPropertiesOfTaggedElementedText() {
		CommentImpl comment = getComment("[Alternative] this is a manually created alternative [/Alternative] and this is a test Sentence.");
		assertEquals(2, comment.getSentences().size());
		assertEquals(true, comment.getSentences().get(0).isRelevant());
		assertEquals(false, comment.getSentences().get(0).isPlainText());
		assertEquals(true, comment.getSentences().get(0).isTagged());
		assertEquals(true, comment.getSentences().get(0).isTaggedManually());
	}
	
	@Test
	@NonTransactional
	public void TestPropertiesOfIconElementedText() {
		CommentImpl comment = getComment("(y) this is a icon pro text.  and this is a test Sentence.");
		assertEquals(2, comment.getSentences().size());
		assertEquals(true, comment.getSentences().get(0).isRelevant());
		assertEquals(false, comment.getSentences().get(0).isPlainText());
		assertEquals(true, comment.getSentences().get(0).isTagged());
		assertEquals(true, comment.getSentences().get(0).isTaggedManually());
		assertEquals(KnowledgeType.ARGUMENT, comment.getSentences().get(0).getType());
	}
	
	
	

	@Test
	@NonTransactional
	public void TestTagReplacementToHTMLCode() {
		CommentImpl comment = getComment("{quote} a quote {quote}");
		System.out.println(comment.getTaggedBody(0));
		//{quote} is replaced on js side
		assertTrue(comment.getTaggedBody(0).contains("{quote} a quote {quote}"));
	}
	
	
	@Test
	@NonTransactional
	public void TestManuallyTaggingPro() {
		CommentImpl comment = getComment("[Pro]this is a manual pro tagged sentence [/Pro]");
		//test the result in splits, fails when checked with equals
		assertTrue(comment.getTaggedBody(0).contains("<span id=\"comment0\">"));
		assertTrue(comment.getTaggedBody(0).contains("<span class=\"sentence Pro\"  id  = ui1>"));
		//important that the tag is not inside the text area
		assertTrue(comment.getTaggedBody(0).contains("<span class =tag>[Pro]</span>")); 
		assertTrue(comment.getTaggedBody(0).contains("<span class = sentenceBody>"));
		assertTrue(comment.getTaggedBody(0).contains("this is a manual pro tagged sentence "));
		//important that the tag is not inside the text area
		assertTrue(comment.getTaggedBody(0).contains("</span><span class =tag>[/Pro]</span>"));
		assertTrue(comment.getTaggedBody(0).contains("</span></span>"));		
	}
	
	@Test
	@NonTransactional
	public void TestManuallyTaggingDecision() {
		CommentImpl comment = getComment("[Decision]this is a manual pro tagged sentence [/Decision]");
		//test the result in splits, fails when checked with equals
		assertTrue(comment.getTaggedBody(0).contains("<span id=\"comment0\">"));
		assertTrue(comment.getTaggedBody(0).contains("<span class=\"sentence Decision\"  id  = ui1>"));
		//important that the tag is not inside the text area
		assertTrue(comment.getTaggedBody(0).contains("<span class =tag>[Decision]</span>")); 
		assertTrue(comment.getTaggedBody(0).contains("<span class = sentenceBody>"));
		assertTrue(comment.getTaggedBody(0).contains("this is a manual pro tagged sentence "));
		//important that the tag is not inside the text area
		assertTrue(comment.getTaggedBody(0).contains("</span><span class =tag>[/Decision]</span>"));
		assertTrue(comment.getTaggedBody(0).contains("</span></span>"));		
	}
	
	@Test
	@NonTransactional
	public void TestManuallyTaggingIssue() {
		CommentImpl comment = getComment("[Issue]this is a manual pro tagged sentence [/Issue]");
		//test the result in splits, fails when checked with equals
		assertTrue(comment.getTaggedBody(0).contains("<span id=\"comment0\">"));
		assertTrue(comment.getTaggedBody(0).contains("<span class=\"sentence Issue\"  id  = ui1>"));
		//important that the tag is not inside the text area
		assertTrue(comment.getTaggedBody(0).contains("<span class =tag>[Issue]</span>")); 
		assertTrue(comment.getTaggedBody(0).contains("<span class = sentenceBody>"));
		assertTrue(comment.getTaggedBody(0).contains("this is a manual pro tagged sentence "));
		//important that the tag is not inside the text area
		assertTrue(comment.getTaggedBody(0).contains("</span><span class =tag>[/Issue]</span>"));
		assertTrue(comment.getTaggedBody(0).contains("</span></span>"));		
	}
	
	
	@Test
	@NonTransactional
	public void TestManuallyTaggingIssueWithIcon() {
		CommentImpl comment = getComment("(y)this is a manual pro tagged sentence.");
		assertTrue(comment.getTaggedBody(0).contains("<span id=\"comment0\">"));
		assertTrue(comment.getTaggedBody(0).contains("<span class=\"sentence Pro\"  id  = ui1>"));
		assertTrue(comment.getTaggedBody(0).contains("<span class =tag>[Pro]</span>"));
		assertTrue(comment.getTaggedBody(0).contains("<span class = sentenceBody>this is a manual pro tagged sentence.</span>"));
		assertTrue(comment.getTaggedBody(0).contains("<span class =tag>[/Pro]</span></span></span>"));
	}
	
	@Test
	@NonTransactional
	public void TestManuallyTaggingIssueWithIconAndPlainText() {
		CommentImpl comment = getComment("(y)this is a manual pro tagged pro sentence. This is simple plain text");
		assertTrue(comment.getTaggedBody(0).contains("<span id=\"comment0\">"));
		assertTrue(comment.getTaggedBody(0).contains("<span class=\"sentence Pro\"  id  = ui1>"));
		assertTrue(comment.getTaggedBody(0).contains("<span class =tag>[Pro]</span>"));
		assertTrue(comment.getTaggedBody(0).contains("<span class = sentenceBody>this is a manual pro tagged pro sentence.</span>"));
		assertTrue(comment.getTaggedBody(0).contains("<span class =tag>[/Pro]</span></span>"));
		assertTrue(comment.getTaggedBody(0).contains("<span class=\"sentence isNotRelevant\"  id  = ui2>"));
		assertTrue(comment.getTaggedBody(0).contains("<span class =tag></span>"));
		assertTrue(comment.getTaggedBody(0).contains("<span class = sentenceBody> This is simple plain text</span>"));
		assertTrue(comment.getTaggedBody(0).contains("<span class =tag></span>"));
		assertTrue(comment.getTaggedBody(0).contains("</span></span>"));
	}
	
	

	public static final class AoSentenceTestDatabaseUpdater implements DatabaseUpdater {
		@SuppressWarnings("unchecked")
		@Override
		public void update(EntityManager entityManager) throws Exception {
			entityManager.migrate(DecisionKnowledgeInCommentEntity.class);
			entityManager.migrate(LinkBetweenDifferentEntitiesEntity.class);
		}
	}
}
