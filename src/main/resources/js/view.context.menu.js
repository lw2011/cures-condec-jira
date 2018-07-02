var createKnowledgeElementText = "Add Element";
var linkKnowledgeElementText = "Link Existing Element";
var deleteLinkToParentText = "Delete Link to Parent";
var editKnowledgeElementText = "Edit Element";
var deleteKnowledgeElementText = "Delete Element";

var contextMenuCreateAction = {
	// label for Tree Viewer, name for Treant context menu
	"label" : createKnowledgeElementText,
	"name" : createKnowledgeElementText,
	"action" : function(position) {
		var id = getSelectedTreeViewerNodeId(position);
		setUpContextMenuContentForCreateAction(id);
	},
	"callback" : function(key, options) {
		var id = getSelectedTreantNodeId(options);
		setUpContextMenuContentForCreateAction(id);
	}
};

function getSelectedTreeViewerNode(position) {
	var selector = position.reference.prevObject.selector;
	return $("#evts").jstree(true).get_node(selector);
}

function getSelectedTreeViewerNodeId(node) {
	return getSelectedTreeViewerNode(node).id;
}

function getSelectedTreantNodeId(options) {
	var context = options.$trigger.context;
	return context.id;
}

function setUpContextMenuContentForCreateAction(id) {
	setUpModal();
	setHeaderText(createKnowledgeElementText);
	setUpContextMenuContent("", "", "Alternative", createKnowledgeElementText);

	var submitButton = document.getElementById("form-input-submit");
	submitButton.onclick = function() {
		var summary = document.getElementById("form-input-summary").value;
		var description = document.getElementById("form-input-description").value;
		var type = $("select[name='form-select-type']").val();
		createDecisionKnowledgeElementAsChild(summary, description, type, id);
		closeModal();
	};
}

function setUpModal() {
	var modal = document.getElementById("context-menu-modal");
	modal.style.display = "block";

	// adds click-handler for elements in modal to close modal window
	var elementsWithCloseFunction = document.getElementsByClassName("modal-close");
	for (var counter = 0; counter < elementsWithCloseFunction.length; counter++) {
		elementsWithCloseFunction[counter].onclick = function() {
			closeModal();
		};
	}

	// closes modal window if user clicks anywhere outside of the modal
	window.onclick = function(event) {
		if (event.target === modal) {
			closeModal();
		}
	};
}

function setHeaderText(headerText) {
	var header = document.getElementById("context-menu-header");
	header.textContent = headerText;
}

function setUpContextMenuContent(summary, description, knowledgeType, buttonText) {
	document
			.getElementById("modal-content")
			.insertAdjacentHTML(
					"afterBegin",
					"<p><label for='form-input-summary' style='display:block;width:45%;float:left;'>Summary:</label>"
							+ "<input id='form-input-summary' type='text' placeholder='Summary' value='"
							+ summary
							+ "' style='width:50%;'/></p>"
							+ "<p><label for='form-input-description' style='display:block;width:45%;float:left;'>Description:</label>"
							+ "<input id='form-input-description' type='text' placeholder='Description' value='"
							+ description
							+ "' style='width:50%;'/></p>"
							+ "<p><label for='form-select-type' style='display:block;width:45%;float:left;'>Knowledge type:</label>"
							+ "<select name='form-select-type' style='width:50%;'/></p>"
							+ "<p><input id='form-input-submit' type='submit' value='" + buttonText
							+ "' style='float:right;'/></p>");

	for (var index = 0; index < knowledgeTypes.length; index++) {
		var isSelected = "";
		if (isKnowledgeTypeLocatedAtIndex(knowledgeType, index)) {
			isSelected = "selected ";
		}
		$("select[name='form-select-type']")[0].insertAdjacentHTML("beforeend", "<option " + isSelected + "value='"
				+ knowledgeTypes[index] + "'>" + knowledgeTypes[index] + "</option>");
	}
}

function isKnowledgeTypeLocatedAtIndex(knowledgeType, index) {
	return knowledgeType.toLowerCase() === knowledgeTypes[index].toLocaleLowerCase();
}

var contextMenuLinkAction = {
	// label for Tree Viewer, name for Treant context menu
	"label" : linkKnowledgeElementText,
	"name" : linkKnowledgeElementText,
	"action" : function(position) {
		var id = getSelectedTreeViewerNodeId(position);
		setUpContextMenuContentForLinkAction(id);
	},
	"callback" : function(key, options) {
		var id = getSelectedTreantNodeId(options);
		setUpContextMenuContentForLinkAction(id);
	}
};

function setUpContextMenuContentForLinkAction(id) {
	setUpModal();
	setHeaderText(linkKnowledgeElementText);

	getUnlinkedDecisionComponents(
			id,
			function(unlinkedDecisionComponents) {
				var insertString = "<p><label for='form-select-component' style='display:block;width:45%;float:left;'>Unlinked Element:</label>"
						+ "<select name='form-select-component' style='width:50%;' />";
				for (var index = 0; index < unlinkedDecisionComponents.length; index++) {
					insertString += "<option value='" + unlinkedDecisionComponents[index].id + "'>"
							+ unlinkedDecisionComponents[index].type + ' / '
							+ unlinkedDecisionComponents[index].summary + "</option>";
				}
				insertString += "</p> <p><input name='form-input-submit' id='form-input-submit' type='submit' value='"
						+ linkKnowledgeElementText + "' style='float:right;'/></p>";

				var content = document.getElementById("modal-content");
				content.insertAdjacentHTML("afterBegin", insertString);

				var submitButton = document.getElementById("form-input-submit");
				submitButton.onclick = function() {
					var childId = $("select[name='form-select-component']").val();
					createLinkToExistingElement(id, childId);
					closeModal();
				};
			});
}

var contextMenuEditAction = {
	// label for Tree Viewer, name for Treant context menu
	"label" : editKnowledgeElementText,
	"name" : editKnowledgeElementText,
	"action" : function(position) {
		var id = getSelectedTreeViewerNodeId(position);
		setUpContextMenuContentForEditAction(id);
	},
	"callback" : function(key, options) {
		var id = getSelectedTreantNodeId(options);
		setUpContextMenuContentForEditAction(id);
	}
};

function setUpContextMenuContentForEditAction(id) {
	isIssueStrategy(id, function(isIssueStrategy) {
		if (isIssueStrategy === true) {
			setUpModal();
			var modal = document.getElementById("modal-content");
			var url = AJS.contextPath() + "/secure/EditIssue!default.jspa?id=" + id;
			var iframe = "<iframe src='" + url + "' style='border:none' height='100%' width='100%'></iframe>";
			modal.insertAdjacentHTML("afterBegin", iframe);
		} else {
			setUpModal();
			setHeaderText(editKnowledgeElementText);
			getDecisionKnowledgeElement(id, function(decisionKnowledgeElement) {
				var summary = decisionKnowledgeElement.summary;
				var description = decisionKnowledgeElement.description;
				var type = decisionKnowledgeElement.type;
				setUpContextMenuContent(summary, description, type, editKnowledgeElementText);

				var submitButton = document.getElementById("form-input-submit");
				submitButton.onclick = function() {
					var summary = document.getElementById("form-input-summary").value;
					var description = document.getElementById("form-input-description").value;
					var type = $("select[name='form-select-type']").val();
					editDecisionKnowledgeElementAsChild(summary, description, type, id);
					closeModal();
				};
			});
		}
	});
}

var contextMenuDeleteAction = {
	// label for Tree Viewer, name for Treant context menu
	"label" : deleteKnowledgeElementText,
	"name" : deleteKnowledgeElementText,
	"action" : function(position) {
		var id = getSelectedTreeViewerNodeId(position);
		setUpContextMenuContentForDeleteAction(id);
	},
	"callback" : function(key, options) {
		var id = getSelectedTreantNodeId(options);
		setUpContextMenuContentForDeleteAction(id);
	}
};

function setUpContextMenuContentForDeleteAction(id) {
	setUpModal();
	setHeaderText(deleteKnowledgeElementText);

	var content = document.getElementById("modal-content");
	content.insertAdjacentHTML("afterBegin",
			"<p><input id='abort-submit' type='submit' value='Abort Deletion' style='float:right;'/>"
					+ "<input id='form-input-submit' type='submit' value=" + deleteKnowledgeElementText
					+ " style='float:right;'/></p>");

	var abortButton = document.getElementById("abort-submit");
	abortButton.onclick = function() {
		closeModal();
	};

	var submitButton = document.getElementById("form-input-submit");
	submitButton.onclick = function() {
		deleteDecisionKnowledgeElement(id, function() {
			updateView(id);
		});
		closeModal();
	};
}

var contextMenuDeleteLinkAction = {
	// label for Tree Viewer, name for Treant context menu
	"label" : deleteLinkToParentText,
	"name" : deleteLinkToParentText,
	"action" : function(position) {
		var node = getSelectedTreeViewerNode(position);
		var id = node.id;
		var parentId = node.parent;
		setUpContextMenuContentForDeleteLinkAction(id, parentId);
	},
	"callback" : function(key, options) {
		var id = getSelectedTreantNodeId(options);
		var parentId = findParentId(id);
		setUpContextMenuContentForDeleteLinkAction(id, parentId);
	}
};

function setUpContextMenuContentForDeleteLinkAction(id, parentId) {
	setUpModal();
	setHeaderText(deleteLinkToParentText);

	var content = document.getElementById("modal-content");
	content.insertAdjacentHTML("afterBegin",
			"<p><input id='abort-submit' type='submit' value='Abort Deletion' style='float:right;'/>"
					+ "<input id='form-input-submit' type='submit' value=" + deleteLinkToParentText
					+ " style='float:right;'/></p>");

	var abortButton = document.getElementById("abort-submit");
	abortButton.onclick = function() {
		closeModal();
	};

	var submitButton = document.getElementById("form-input-submit");
	submitButton.onclick = function() {
		deleteLinkToExistingElement(parentId, id);
		closeModal();
	};
}

var contextMenuActions = {
	"create" : contextMenuCreateAction,
	"edit" : contextMenuEditAction,
	"link" : contextMenuLinkAction,
	"deleteLink" : contextMenuDeleteLinkAction,
	"delete" : contextMenuDeleteAction
};

function closeModal() {
	var modal = document.getElementById("context-menu-modal");
	modal.style.display = "none";
	var modalHeader = document.getElementById("modal-header");
	if (modalHeader.hasChildNodes()) {
		var childNodes = modalHeader.childNodes;
		for (var index = 0; index < childNodes.length; ++index) {
			var child = childNodes[index];
			if (child.nodeType === 3) {
				child.parentNode.removeChild(child);
			}
		}
	}
	var modalContent = document.getElementById("modal-content");
	if (modalContent) {
		clearInner(modalContent);
	}
}