function initializeDecisionKnowledgePage() {
    console.log("view.decision.knowledge.page initializeDecisionKnowledgePage");
	for (var index = 0; index < knowledgeTypes.length; index++) {
		var isSelected = "";
		if (knowledgeTypes[index] === "Decision") {
			isSelected = "selected ";
		}
		$("select[name='select-root-element-type']")[0].insertAdjacentHTML("beforeend", "<option " + isSelected + " value='"
				+ knowledgeTypes[index] + "'>" + knowledgeTypes[index] + "</option>");
	}

	var createElementButton = document.getElementById("create-element-button");
	var elementInputField = document.getElementById("element-input-field");
	createElementButton.addEventListener("click", function() {
		var summary = elementInputField.value;
		var type = $("select[name='select-root-element-type']").val();
		elementInputField.value = "";
		createDecisionKnowledgeElement(summary, "", type, function(id) {
			updateDecisionKnowledgeView(id);
		});
	});

	var depthOfTreeInput = document.getElementById("depth-of-tree-input");
	depthOfTreeInput.addEventListener("input", function() {
		var depthOfTreeWarningLabel = document.getElementById("depth-of-tree-warning");
		if (this.value > 0) {
			depthOfTreeWarningLabel.style.visibility = "hidden";
			updateDecisionKnowledgeView();
		} else {
			depthOfTreeWarningLabel.style.visibility = "visible";
		}
	});

	updateDecisionKnowledgeView();
}

function updateDecisionKnowledgeView(nodeId) {
    console.log("view.decision.knowledge.page updateDecisionKnowledgeView");
	buildTreeViewer();
	if (nodeId === undefined) {
		var rootElement = getCurrentRootElement();
		if (rootElement) {
			selectNodeInTreeViewer(rootElement.id);
		}
	} else {
		selectNodeInTreeViewer(nodeId);
	}
	jQueryConDec("#jstree").on("select_node.jstree", function(error, tree) {
		var node = tree.node.data;
		buildTreant(node.key, true);
	});
}