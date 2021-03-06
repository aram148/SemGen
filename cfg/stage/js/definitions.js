/**
 *
 */

var StageTasks = {
    PROJECT: {type: "PROJECT", thumb: '../../src/semgen/icons/stageicon2020.png'},
    MERGER: {type: "MERGER", thumb: '../../src/semgen/icons/mergeicon2020.png'},
    EXTRACTOR: {type: "EXTRACTOR", thumb: '../../src/semgen/icons/extractoricon2020.png'}
};

var NodeType = {
    MODEL: {id: 0, nodeType: "Model", color: "#000000", canShowHide: false},
    SUBMODEL: {id: 1, nodeType: "Submodel", color: "#CA9485", canShowHide: false},
    STATE: {id: 2, nodeType: "State", color: "#1F77B4", canShowHide: true},
    RATE: {id: 3, nodeType: "Rate", color: "#2CA02C", canShowHide: true},
    FORCE: {id: 4, nodeType: "Force", color: "#ff8aff", canShowHide: true},
    CONSTITUTIVE: {id: 5, nodeType: "Constitutive", color: "#FF7F0E", canShowHide: true},
    ENTITY: {id: 6, nodeType: "Entity", color: "#1F77B4", canShowHide: true},
    PROCESS: {id: 7, nodeType: "Process", color: "#2CA02C", canShowHide: true},
    MEDIATOR: {id: 8, nodeType: "Entity", color: "#1F77B4", canShowHide: true},
    NULLNODE: {id: 9, nodeType: "Null", color: "#FFFFFF", canShowHide: true},
    EXTRACTION: {id: 10, nodeType: "Extraction", color: "#118888", canShowHide: false},
    UNSPECIFIED: {id: 11, nodeType: "Unspecified", color: "#FFFFFF", canShowHide:true}
};

var NodeTypeMap = {
    "Model": NodeType.MODEL,
    "Submodel": NodeType.SUBMODEL,
    "State": NodeType.STATE,
    "Rate": NodeType.RATE,
    "Force": NodeType.FORCE,
    "Constitutive": NodeType.CONSTITUTIVE,
    "Entity": NodeType.ENTITY,
    "Process": NodeType.PROCESS,
    "Mediator": NodeType.MEDIATOR,
    "Null": NodeType.NULLNODE,
    "Extraction": NodeType.EXTRACTION,
    "Unspecified": NodeType.UNSPECIFIED
};

var NodeTypeArray = [
    NodeType.MODEL,
    NodeType.SUBMODEL,
    NodeType.STATE,
    NodeType.RATE,
    NodeType.FORCE,
    NodeType.CONSTITUTIVE,
    NodeType.ENTITY,
    NodeType.PROCESS,
    NodeType.MEDIATOR,
    NodeType.NULLNODE,
    NodeType.EXTRACTION,
    NodeType.UNSPECIFIED
];


var defaultcharge = -180;
var defaultlinklength = 50;
var defaultchargedistance = 300;

var DisplayModes = {
    SHOWSUBMODELS: { id: 0, btnid: "showSubmodels", keys: [NodeType.SUBMODEL, NodeType.STATE, NodeType.RATE, NodeType.FORCE, NodeType.CONSTITUTIVE]},
    SHOWDEPENDENCIES: { id: 1, btnid: "showDependencies", keys: [NodeType.STATE, NodeType.RATE, NodeType.FORCE, NodeType.CONSTITUTIVE]},
    SHOWPHYSIOMAP: { id: 2, btnid: "showPhysiomap", keys: [NodeType.ENTITY, NodeType.PROCESS, NodeType.UNSPECIFIED]},
    SHOWMODEL: { id: 3, btnid: "showModel", keys: [NodeType.MODEL]}
};



var LinkLevels = {
    INTRASUB: {text: "Used to Compute", color: "#555555", linewidth: "1.5px"},
    INTERSUB: {text: "Inter-Submodel", color: "#CA9485", linewidth: "0.25px"},
    AUTOMAP: {text: "Auto-mapping (merging)", color: "#5bc0de", linewidth: "1.5px"},
    CUSTOM: {text: "Manual-mapping (merging)", color: "#f0ad4e", linewidth: "1.5px"},
    PRODCON: {text: "Production/Consumption", color: "#555555", linewidth: "1.5px"},
    MEDIATOR: {text: "Mediator", color: "#555555", linewidth: "1.5px"}
}

var LinkDisplayModes = {
    SUBDEP: { id: 0, keys: [LinkLevels.INTRASUB, LinkLevels.INTERSUB, LinkLevels.AUTOMAP, LinkLevels.CUSTOM]},
    PHYSIOMAP: { id: 1, keys: [LinkLevels.PRODCON, LinkLevels.MEDIATOR]}
};


var LinkLevelsArray = [
    LinkLevels.INTRASUB,
    LinkLevels.INTERSUB,
    LinkLevels.CUSTOM,
    LinkLevels.MEDIATOR
]
