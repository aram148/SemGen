package semgen.extraction.workbench;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import semgen.utilities.Workbench;
import semsim.model.collection.SemSimModel;
import semsim.model.collection.Submodel;
import semsim.model.computational.datastructures.DataStructure;
import semsim.model.physical.PhysicalEntity;
import semsim.model.physical.PhysicalProcess;
import semsim.model.physical.object.CompositePhysicalEntity;


public class ExtractorWorkbench extends Workbench {
	File sourcefile;
	SemSimModel semsimmodel;
	private HashMap<PhysicalProcess,Set<DataStructure>> processdatastructuremap;
	private HashMap<PhysicalEntity,Set<DataStructure>> entitydatastructuremap;
	private HashMap<Submodel,Set<DataStructure>> submodeldatastructuremap;
	
	private Map<DataStructure,Set<? extends DataStructure>> extractionmap;

	public ExtractorWorkbench(File file, SemSimModel model) {
		sourcefile = file;
		semsimmodel = model;
		
		createProcessDataStructureMap();
		createEntityDataStructureMap();
		createSubmodelDataStructureMap();
	}
	
	@Override
	public void initialize() {

	}

	@Override
	public void setModelSaved(boolean val) {
		
	}

	@Override
	public String getCurrentModelName() {
		return semsimmodel.getName();
	}

	@Override
	public String getModelSourceFile() {
		return semsimmodel.getLegacyCodeLocation();
	}

	@Override
	public File saveModel() {
		return null;
	}

	@Override
	public File saveModelAs() {
		return null;
	}

	public File getSourceFile() {
		return sourcefile;
	}
	
	public SemSimModel getSourceModel() {
		return semsimmodel;
	}

	public HashMap<PhysicalProcess,Set<DataStructure>> getProcessDataStructureMap(){
		return processdatastructuremap;
	}
	
	public void setProcessDataStructureMap(HashMap<PhysicalProcess,Set<DataStructure>> map){
		processdatastructuremap = map;
	}
	
	public HashMap<PhysicalEntity,Set<DataStructure>> getEntityDataStructureMap(){
		return entitydatastructuremap;
	}
	
	public void setEntityDataStructureMap(HashMap<PhysicalEntity,Set<DataStructure>> map){
		entitydatastructuremap = map;
	}
	
	public HashMap<Submodel,Set<DataStructure>> getSubmodelDataStructureMap(){
		return submodeldatastructuremap;
	}
	
	public void setSubmodelDataStructureMap(HashMap<Submodel,Set<DataStructure>> map){
		submodeldatastructuremap = map;
	}
	
	private void createProcessDataStructureMap(){
			
		processdatastructuremap = new HashMap<PhysicalProcess,Set<DataStructure>>();
		Set<DataStructure> propandproc = semsimmodel.getDataStructureswithPhysicalProcesses();
		
		// List physical properties of processes
		for(DataStructure ds : propandproc){
			PhysicalProcess proc = (PhysicalProcess) ds.getAssociatedPhysicalModelComponent();
			
			if(processdatastructuremap.containsKey(proc)){
				processdatastructuremap.get(proc).add(ds);
			}
			else{
				Set<DataStructure> cdwds = new HashSet<DataStructure>();
				cdwds.add(ds);
				processdatastructuremap.put(proc, cdwds);
			}
		}
	}
	
	private void createEntityDataStructureMap(){
		
		entitydatastructuremap = new HashMap<PhysicalEntity,Set<DataStructure>>();
		Set<DataStructure> dses = semsimmodel.getDataStructureswithCompositesEntities();
		
		for(DataStructure ds : dses){
			CompositePhysicalEntity ent = (CompositePhysicalEntity) ds.getAssociatedPhysicalModelComponent();
			
			if(entitydatastructuremap.containsKey(ent)){
				entitydatastructuremap.get(ent).add(ds);
			}
			else{
				Set<DataStructure> cdwds = new HashSet<DataStructure>();
				cdwds.add(ds);
				entitydatastructuremap.put(ent, cdwds);
			}
		}
	}
	
	// Generate the mappings between submodels and the data structures they are associated with
	private void createSubmodelDataStructureMap(){
		
		submodeldatastructuremap = new HashMap<Submodel,Set<DataStructure>>();
		
		for(Submodel submodel : semsimmodel.getSubmodels()){
			submodeldatastructuremap.put(submodel, submodel.getAssociatedDataStructures());
		}
	}
	
	// Retrieve the set of data structures are needed to compute a given data structure
	public Set<DataStructure> getDataStructureDependencyChain(DataStructure startds){
		
		// The hashmap contains the data structure and whether the looping alogrithm here should collect 
		// their inputs (true = collect)
		Map<DataStructure, Boolean> dsandcollectmap = new HashMap<DataStructure, Boolean>();
		dsandcollectmap.put(startds, true);
		DataStructure key = null;
		Boolean cont = true;
		
		while (cont) {
			cont = false; // We don't continue the loop unless we find a data structure with computational inputs
					  	  // that we need to collect (if the value for the DS in the map is 'true')
			for (DataStructure onekey : dsandcollectmap.keySet()) {
				key = onekey;
				if ((Boolean) dsandcollectmap.get(onekey) == true) {
					cont = true;
					for (DataStructure oneaddedinput : onekey.getComputationInputs()) {
						if (!dsandcollectmap.containsKey(oneaddedinput)) {
							dsandcollectmap.put(oneaddedinput, !oneaddedinput.getComputationInputs().isEmpty());
						}
					}
					break;
				}
			}
			dsandcollectmap.remove(key);
			dsandcollectmap.put(key, false);
		}
		
		Set<DataStructure> dsset = new HashSet<DataStructure>(dsandcollectmap.keySet());
		return dsset;
	}

	public Map<DataStructure,Set<? extends DataStructure>> getExtractionMap() {
		return extractionmap;
	}

	public void setExtractionMap(Map<DataStructure,Set<? extends DataStructure>> extractionmap) {
		this.extractionmap = extractionmap;
	}
	
	// Add a data structure's computational inputs to the extraction map
	public void addInputsToExtractionMap(DataStructure onedatastr){
		for (DataStructure nextds : onedatastr.getComputationInputs()) {
			extractionmap.put(nextds, nextds.getComputationInputs());
			for(DataStructure secondaryds : nextds.getComputationInputs()){
				if (!extractionmap.containsKey(secondaryds)) {
					extractionmap.put(secondaryds, new HashSet<DataStructure>());
				}
			}
		}
	}
	
	// Add the data structures associated with a process's participants
	public void addParticipantsToExtractionMap(PhysicalProcess pmc) {
		// Add data structures associated with the participants in the process
		for(PhysicalEntity ent : pmc.getParticipants()){
			if(getEntityDataStructureMap().containsKey(ent)){
				for(DataStructure entds : getEntityDataStructureMap().get(ent)){
					// Maybe change so that if a cdwd that we're including is dependent on another that's
					// a participant, make sure to include its inputs (all inputs?)
					getExtractionMap().put(entds, entds.getComputationInputs());
					// Add the entity's inputs, make them terminal
					for(DataStructure oneentin : entds.getComputationInputs()){
						if(!getExtractionMap().containsKey(oneentin)){
							getExtractionMap().put(oneentin, new HashSet<DataStructure>());
						}
					}
				}
			}
		}
	}
}
