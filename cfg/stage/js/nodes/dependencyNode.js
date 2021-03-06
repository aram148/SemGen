/**
 * Represents a dependency node in the d3 graph
 */

DependencyNode.prototype = new Node();
DependencyNode.prototype.constructor = DependencyNode;

function DependencyNode (graph, data, parentNode) {
	// Ensure the node type is formatted properly

	Node.prototype.constructor.call(this, graph, data, parentNode, 5, 12, graph.nodecharge);
	this.submodelid = data.submodelId;
	this.submodelinput = data.issubmodelinput;
	
	this.addClassName("dependencyNode");
	//this.addBehavior(Columns);
	var _node = this;
	graph.depBehaviors.forEach(function(b) {
		_node.addBehavior(b);
	});
	this.addBehavior(HiddenLabelNodeGenerator);

	this.isOrphaned = function() {
		return data.isorphaned;
	}
	
	this.getInputs = function() {
		var inputs = [];
		this.srcobj.inputs.forEach(function (link) {
			var inputnode = _node.graph.findNode(link.input.id);
			//Return only node types that are visible
			if (inputnode.graph.nodesVisible[inputnode.nodeType.id]) {
				inputs.push(inputnode);
			}
			
		});
		return inputs;
	}
}

DependencyNode.prototype.createVisualElement = function (element, graph) {
	Node.prototype.createVisualElement.call(this,element, graph);
	if (this.submodelinput) {
		this.defaultopacity = 0.6;
		this.rootElement.selectAll("circle").attr("opacity", this.defaultopacity);
	}

}

DependencyNode.prototype.getFirstLinkableAncestor = function() {
	var outputNode = this;
	while (!outputNode.isVisible() && outputNode.canlink) {
		outputNode = outputNode.parent;
	}
	if (!outputNode.canlink) outputNode = null;
	return outputNode;
}

DependencyNode.prototype.getLinks = function (linklist) {
	
	// Build an array of links from our list of inputs
	var links = [];
//	if (this.nodeType.id == NodeType.ENTITY.id) {
//		return links;
//	}
	var fade = false;
	var interSubmodelLink = false;
	if (!this.graph.nodesVisible[this.nodeType.id] ) {
		//return links;
		fade = true;
	}
	var outputNode = this.getFirstLinkableAncestor();
	if (!outputNode || outputNode.showchildren) return links; 
	
	this.srcobj.inputs.forEach(function (link) {
		var inputNode = outputNode.graph.findNode(link.input.id);
		
		if (!inputNode.graph.nodesVisible[inputNode.nodeType.id]) {
			if (inputNode.parent == this.parent) {
				return links;
			}
			// fade = true;
		}

		if (outputNode.submodelinput) {
            interSubmodelLink = true;
        }

		inputNode = inputNode.getFirstLinkableAncestor();
		if (inputNode==null) {
			return links;
		}
		else if ((inputNode.parent == outputNode) || inputNode.showchildren) {
			return links;
		}
		if (!inputNode || inputNode==outputNode) return;
		//Check for duplicate links
			for (l in linklist) {
				var exisstinglink = linklist[l];
				if (exisstinglink.linksNodes(inputNode, outputNode)) {
					return;
				}
				else if (exisstinglink.linksNodes(outputNode, inputNode)) {
					exisstinglink.bidirectional = true;
					return;
				}
			}
		
		var length = outputNode.graph.linklength;
		links.push(new Link(outputNode.graph, link, outputNode, inputNode, length, fade, interSubmodelLink));
	});

	return links;
}

DependencyNode.prototype.hasIntermodalLink = function() {
	for (x in this.srcobj.inputs) {
		if (this.srcobj.inputs[x].linklevel > 1) {
			return true;
		}
	}
	return false;
}

DependencyNode.prototype.removeLink = function(inputnode) {
	var srcinputs =this.srcobj.inputs;
	var loc = null;
	for (i in srcinputs) {
		if (srcinputs[i].output == inputnode.id) {
			loc = i;
			break;
		}
	}
	if (i != null) {
		srcinputs.splice(i, 1);
	}
	return false;
}

DependencyNode.prototype.isVisible = function () {
	if (this.srcobj.isorphaned && !this.graph.showorphans && this.getRootParent().nodeType != NodeType.EXTRACTION) return false;  
	return Node.prototype.isVisible.call(this);
}

DependencyNode.prototype.getContextMenu = function() {
	var menu = [];
		if (this.getRootParent().nodeType != NodeType.EXTRACTION) {
			menu = [{text: 'Select Node Inputs', action : 'selectinputs'}, {text : "Extract Selected", action : "extract"}, {text : "Extract Unselected", action : "extractexclude"}];
		}
		else {
			menu = [{text: 'Select Node Inputs', action : 'selectinputs'}]; //, {text : "Remove Selected", action : "removeselected"}];
		}
	
	return menu;
	
}

Node.prototype.updateInfo = function() {
	$("#nodemenuParticipantsRow").hide();
	
	$("#nodemenuAnnotation").text(this.srcobj.physannotation);
	$("#nodemenuUnit").text(this.srcobj.unit);
	$("#nodemenuEquation").text(this.srcobj.equation);
}