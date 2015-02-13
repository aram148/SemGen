/**
 * Represents a model node in the d3 graph
 */

var openPopover;

ModelNode.prototype = new Node();
ModelNode.prototype.constructor = Node;
function ModelNode (name) {
	Node.prototype.constructor.call(this, name, name, 16, 0);
	this.fixed = true;
	this.isInitialized = false;
	
	this.addClassName("modelNode");
}

ModelNode.prototype.createVisualElement = function (element) {
	Node.prototype.createVisualElement.call(this, element);
	
	// If we've initialized don't create
	if(this.isInitialized)
		return;
	
	// Define the popover html
	$("circle", element).popover({
		container: "body",
		title: "Model Tasks",
		html: true,
		content: 
			"<ul class='modelPopover'>" +
				"<li><a href='#' onclick='comingSoonClickHandler(this);'>Merge</a></li>" +
				"<li><a href='#' onclick='comingSoonClickHandler(this);'>Annotate</a></li>" +
				"<li><a href='#' onclick='comingSoonClickHandler(this);'>Extract</a></li>" +
				"<li class='submenuContainer'>" +
					"Visualize" +
					"<ul>" +
						"<li><a href='#' onclick='comingSoonClickHandler(this);'>Submodels</a></li>" +
						"<li><a href='#' onclick='taskClicked(this);'>Dependencies</a></li>" +
					"</ul>" +
				"</li>" +
			"</ul>",
		placement: "bottom",
		trigger: "manual" });

	// Show the popover when the node is clicked
	$("circle", element).click({modelNode: this}, function (e) {
		var popover = $(this);
		var isOpen = false;
		
		// If there's an open popover, hide it
		if(openPopover != null) {
			isOpen = openPopover.attr("aria-describedBy") == popover.attr("aria-describedBy");
			hideOpenPopover();
		}
		
		// If the open popover is not the node that was clicked
		// and store it
		if(!isOpen) {
			$(this).popover("show");
			openPopover = $(this).popover();
			openPopover.modelNode = e.data.modelNode;
		}
		
		e.stopPropagation();
	});
	
	$(window).click(function () { hideOpenPopover(); });
	
	this.isInitialized = true;
}

function hideOpenPopover() {
	if(!openPopover)
		return;
	
	openPopover.popover("hide");
	openPopover = null;
}
	
function comingSoonClickHandler(element) {
	alert("'" + element.innerHTML + "' coming soon!");
}

function taskClicked (element) {
	var task = element.innerHTML.toLowerCase();
	sender.taskClicked(openPopover.modelNode.id, task);
}