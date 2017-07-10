package com.atlassian.DecisionDocumentation.rest.treants;

import javax.xml.bind.annotation.*;

import com.atlassian.jira.issue.Issue;

/**
 * 
 * @author Ewald Rode
 * @description
 */
@XmlRootElement(name = "treant")
@XmlAccessorType(XmlAccessType.FIELD)
public class TreantRestModel {
	/*Einstellungen fuer die Darstellung des Baumes*/
    @XmlElement
    private Chart chart;
    
    /*Baum-Datenstruktur*/
    @XmlElement
    private NodeStructure nodeStructure;
    
    

    public TreantRestModel() {
    	this.chart = null;
    	this.nodeStructure = null;
    }

    public TreantRestModel(Issue issue, int depth) {
        this.chart = new Chart();
        this.nodeStructure = new NodeStructure(issue, depth);
    }
}