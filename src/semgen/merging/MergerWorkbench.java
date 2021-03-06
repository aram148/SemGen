package semgen.merging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Observable;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import semgen.merging.Merger.ResolutionChoice;
import semgen.merging.ModelOverlapMap.MapType;
import semgen.utilities.SemGenError;
import semgen.utilities.Workbench;
import semgen.utilities.file.LoadModelJob;
import semgen.utilities.file.SemGenSaveFileChooser;
import semgen.utilities.uicomponent.SemGenProgressBar;
import semsim.fileaccessors.ModelAccessor;
import semsim.model.collection.SemSimModel;
import semsim.model.computational.datastructures.DataStructure;
import semsim.reading.ModelClassifier.ModelType;
import semsim.utilities.SemSimUtil;

public class MergerWorkbench extends Workbench {
	private int modelselection = -1;
	private ModelOverlapMap overlapmap = null;
	private ArrayList<SemSimModel> loadedmodels = new ArrayList<SemSimModel>();
	private ModelAccessor target = null;
	public SemSimModel mergedmodel;
	private ArrayList<ModelAccessor> modelaccessorlist = new ArrayList<ModelAccessor>();
	private ArrayList<ArrayList<DataStructure>> alldslist = new ArrayList<ArrayList<DataStructure>>();
	private ArrayList<ArrayList<DataStructure>> exposeddslist = new ArrayList<ArrayList<DataStructure>>();
	
	public enum MergeEvent {
		threemodelerror, modellistupdated, modelerrors,	mapfocuschanged, mappingevent, MAPPINGREMOVED, mergecompleted;
		
		String message = null;
		
		private MergeEvent() {}
		
		public boolean hasMessage() {
			return message != null;
		}
		
		public String getMessage() {
			String msg = message;
			message = null;
			return msg;
		}
		
		public void setMessage(String msg) {
			message = msg;
		}
	}

	@Override
	public void initialize() {}
	
	private SemSimModel loadModel(ModelAccessor modelaccessor, boolean autoannotate) {
		LoadModelJob loader = new LoadModelJob(modelaccessor, autoannotate);
		loader.run();
		SemSimModel modeltoload = loader.getLoadedModel();
		return modeltoload;
	}
	public int getNumberofStagedModels() {
		return loadedmodels.size();
	}
	
	public boolean addModels(ArrayList<ModelAccessor> modelaccessors, boolean autoannotate) {
		if (loadedmodels.size() == 2) {
			setChanged();
			notifyObservers(MergeEvent.threemodelerror);
			return false;
		}
		
		SemSimModel model;
		for (ModelAccessor modelaccessor : modelaccessors) {
			model = loadModel(modelaccessor, autoannotate);
			if (SemGenError.showSemSimErrors()) continue;
			loadedmodels.add(model);
			modelaccessorlist.add(modelaccessor);
			addDSNameList(model.getAssociatedDataStructures());
		}

		notifyModelListUpdated();
		return true;
	}
	
	public boolean addModels(ArrayList<ModelAccessor> files, ArrayList<SemSimModel> models, boolean autoannotate) {
		if (loadedmodels.size() == 2) {
			setChanged();
			notifyObservers(MergeEvent.threemodelerror);
			return false;
		}
		
		SemSimModel model;
		ModelAccessor file;
		for (int i = 0; i < files.size(); i++) {
			file = files.get(i);
			model = models.get(i);
			if (model == null) {
				model = loadModel(file, autoannotate);
				if (SemGenError.showSemSimErrors()) continue;
			}
			
			loadedmodels.add(model);
			modelaccessorlist.add(file);
			addDSNameList(model.getAssociatedDataStructures());
		}
		notifyModelListUpdated();
		
		return true;
	}
	private void addDSNameList(Collection<DataStructure> dslist) {
		alldslist.add(SemSimUtil.alphebetizeSemSimObjects(dslist));
		ArrayList<DataStructure> tempdslist = new ArrayList<DataStructure>();
		
		// Iterate through the DataStructures just added and weed out CellML-style inputs
		for(DataStructure ds : alldslist.get(alldslist.size()-1)){
			if(! ds.isFunctionalSubmodelInput()){
				tempdslist.add(ds);
			}
		}
		exposeddslist.add(tempdslist);
	}
	
	//Get the objects describing a pair of semantically matched data strutures
	public Pair<DataStructureDescriptor,DataStructureDescriptor> getDSDescriptors(int index) {
		return overlapmap.getDSPairDescriptors(index);
	}
	
	public void removeSelectedModel() {
		if (modelselection == -1) return;
		loadedmodels.remove(modelselection);
		modelaccessorlist.remove(modelselection);
		overlapmap = null;
		alldslist.clear();
		exposeddslist.clear();
		if (!loadedmodels.isEmpty()) {
			addDSNameList(loadedmodels.get(0).getAssociatedDataStructures());
		}
		
		notifyModelListUpdated();
	}

	public boolean hasMultipleModels() {
		return (loadedmodels.size() > 1);
	}
	
	//Compare two models and find all semantically identical codewords and datastructures using the same name
	public void mapModels() {
		SemanticComparator comparator = new SemanticComparator(loadedmodels.get(0), loadedmodels.get(1));
		overlapmap = new ModelOverlapMap(0, 1, comparator);
		setChanged();
		notifyObservers(MergeEvent.mapfocuschanged);
	}
	
	//Create a map of identical submodel names with an empty string for the value. The empty string will
	//be replaced by the users replacement name
	public HashMap<String, String> createIdenticalSubmodelNameMap() {
		HashMap<String, String> namemap = new HashMap<String, String>();
		for (String name : overlapmap.getIdenticalSubmodelNames()) {
			namemap.put(new String(name), "");
		}
		return namemap;
	}
	
	public HashMap<String, String> createIdenticalNameMap() {
		HashMap<String, String> namemap = new HashMap<String, String>();
		for (String name : overlapmap.getIdenticalNames()) {
			namemap.put(new String(name), "");
		}
		return namemap;
	}
	
	public HashMap<String, String> createIdenticalNameMap(ArrayList<ResolutionChoice> choicelist, Set<String> submodelnamemap) {
		HashMap<String, String> identicalmap = new HashMap<String,String>();
		Set<String> identolnames = new HashSet<String>();
		for (int i=getSolutionDomainCount(); i<choicelist.size(); i++) {	
			if (!choicelist.get(i).equals(ResolutionChoice.ignore)) {
				identolnames.add(overlapmap.getDataStructurePairNames(i).getLeft());
			}
		}
		for (String name : overlapmap.getIdenticalNames()) {
			if(name.contains(".")) {
				if (submodelnamemap.contains(name.substring(0, name.lastIndexOf("."))))
					continue;
			}
				
			// If an identical codeword mapping will be resolved by a semantic resolution step or a renaming of identically-named submodels, 
		    // don't include in idneticalmap	
			if (!identolnames.contains(name)) {
				identicalmap.put(new String(name), "");
			}
		}
		return identicalmap;
	}
	
	//Get the names of the two data structures at the given index
	public Pair<String, String> getMapPairNames(int index) {
		return overlapmap.getDataStructurePairNames(index);
	}
	
	public MapType getMapPairType(int index) {
		return overlapmap.getMappingType(index);
	}
	
	//Get the names of the two models being merged
	public Pair<String, String> getOverlapMapModelNames() {
		Pair<Integer, Integer> indicies = overlapmap.getModelIndicies();
		return Pair.of(loadedmodels.get(indicies.getLeft()).getName(), 
				loadedmodels.get(indicies.getRight()).getName());
	}
	
	public ArrayList<String> getModelNames() {
		ArrayList<String> names = new ArrayList<String>();
		for (SemSimModel model : loadedmodels) {
			names.add(model.getName());
		}
		return names;
	}

	public void removeManualCodewordMapping(int overlapindex) {
		overlapmap.removeOverlap(overlapindex);
		setChanged();
		notifyObservers(MergeEvent.MAPPINGREMOVED);
	}
	
	//Add manual codeword mapping to the list of equivalent terms
	public Pair<String,String> addManualCodewordMapping(String cdwd1, String cdwd2) {
		int index1 = getExposedCodewordIndexbyName(0, cdwd1);
		int index2 =getExposedCodewordIndexbyName(1, cdwd2);

		return addManualCodewordMapping(index1,index2);
	}
	
	
	//Add manual codeword mapping to the list of equivalent terms
	public Pair<String,String> addManualCodewordMapping(int cdwd1, int cdwd2) {
		Pair<Integer, Integer> minds = overlapmap.getModelIndicies();
		DataStructure ds1 = exposeddslist.get(minds.getLeft()).get(cdwd1);
		DataStructure ds2 = exposeddslist.get(minds.getRight()).get(cdwd2);
				
		if (codewordMappingExists(ds1, ds2)) return Pair.of(ds1.getName(),ds2.getName());
		addCodewordMapping(ds1, ds2, MapType.MANUAL_MAPPING);
		setChanged();
		notifyObservers(MergeEvent.mappingevent);
		return null;
	}
	
	private int getExposedCodewordIndexbyName(int modind, String cdwd) {
		for (DataStructure ds : exposeddslist.get(modind)) {
			if (ds.getName().equals(cdwd)) {
				return exposeddslist.get(modind).indexOf(ds);
			}
		}
		//If a datastructure with that name cannot be found, try searching with just the DS name
		String[] names = cdwd.split("\\.");
		String dsname = names[names.length-1];

		for (DataStructure ds : exposeddslist.get(modind)) {
			if (ds.getName().equals(dsname)) {
				return exposeddslist.get(modind).indexOf(ds);
			}
		}
		return -1;
	}

	public ArrayList<Pair<DataStructure,DataStructure>> getOverlapPairs() {
		return overlapmap.getDataStructurePairs();
	}
	
	public int getMappingCount() {
		return overlapmap.getMappingCount();
	}
	
	public int getSolutionDomainCount() {
		return overlapmap.getSolutionDomainCount();
	}
	
	public boolean hasSemanticOverlap() {
		return (overlapmap.getMappingCount()>0);
	}
	
	private void addCodewordMapping(DataStructure ds1, DataStructure ds2, MapType maptype) {
		overlapmap.addDataStructureMapping(ds1, ds2, maptype);
	}
	
	private boolean codewordMappingExists(DataStructure ds1, DataStructure ds2) {
		return overlapmap.dataStructuresAlreadyMapped(ds1, ds2);
	}
	
	public boolean codewordMappingExists(String cdwd1uri, String cdwd2uri) {
		return overlapmap.codewordsAlreadyMapped(cdwd1uri, cdwd2uri);
	}
	
	public void setSelection(int index) {
		modelselection = index;
	}
	
	//Get the models a modeloverlapmap describes
	private Pair<SemSimModel, SemSimModel> getModelOverlapMapModels(ModelOverlapMap map) {
		Pair<Integer, Integer> indexpair = map.getModelIndicies();
		return Pair.of(loadedmodels.get(indexpair.getLeft()),loadedmodels.get(indexpair.getRight()));
	}
	
	public ArrayList<Boolean> getUnitOverlaps() {
		return overlapmap.compareDataStructureUnits();
	}
	
	/**
	 * Create a merger task class and define the end task behavior.
	 * @param dsnamemap
	 * @param smnamemap
	 * @param choices
	 * @param conversions
	 * @param bar
	 * @return
	 */
	
	public String executeMerge(HashMap<String,String> dsnamemap, HashMap<String,String> smnamemap, ArrayList<ResolutionChoice> choices, 
			ArrayList<Pair<Double,String>> conversions, SemGenProgressBar bar) {
		Pair<SemSimModel, SemSimModel> models = getModelOverlapMapModels(overlapmap);

		if(models.getLeft().getSolutionDomains().size()>1 || models.getRight().getSolutionDomains().size()>1){
			return "One of the models to be merged has multiple solution domains.";
		}
		
		MergerTask task = new MergerTask(models, overlapmap, dsnamemap, smnamemap, choices, conversions, bar) {
			//At the end of the task get the merged model and save it out as a temporary owl file, reload it and delete the temp file.
			public void endTask() {
				mergedmodel = getMergedModel();
				//Prevent an invalid CellML model from getting written.
				if( ! mergedmodel.getEvents().isEmpty() && target.getModelType() == ModelType.CELLML_MODEL){
					SemGenError.showError("Cannot save as CellML because model contains discrete events", 
							"Cannot write to CellML");
					return;
				}
				
				target.writetoFile(mergedmodel);
				
				LoadModelJob loader = new LoadModelJob(target, false);
				loader.run();
				mergedmodel = loader.getLoadedModel();
								
				setChanged();
				notifyObservers(MergeEvent.mergecompleted);
			}
		};
		task.execute();
		
		return null;
	}

	@Override
	public void setModelSaved(boolean val) {
		modelsaved = val;
	}

	@Override
	public String getCurrentModelName() {
		if (modelselection == -1) return null;
		return loadedmodels.get(modelselection).getName();
	}

	@Override
	public ModelAccessor getModelSourceLocation() {
		if (modelselection == -1) return null;
		return loadedmodels.get(modelselection).getLegacyCodeLocation();
	}

	@Override
	public ModelAccessor saveModel(Integer index) {
		if (modelaccessorlist.size() >= 3) {
			
			if(modelaccessorlist.get(2) != null){
				modelaccessorlist.get(2).writetoFile(mergedmodel);
				setModelSaved(true);
				return  modelaccessorlist.get(2);
			}
		}
		return saveModelAs(index);		
	}

	@Override
	public ModelAccessor saveModelAs(Integer index) {
		SemGenSaveFileChooser filec = new SemGenSaveFileChooser(new String[]{"owl", "proj", "cellml", "sbml", "omex"}, "owl");
		ModelAccessor ma = filec.SaveAsAction(mergedmodel);
		if (ma != null) {
			target = ma;
			if (!writeMerge()) return null;
		}
		this.modelsaved = (ma!=null);
		return ma;
	}
	
	public boolean selectMergeFileLocation() {
		SemGenSaveFileChooser filec = new SemGenSaveFileChooser(new String[]{"owl", "proj", "cellml", "sbml", "omex"}, "owl");
		ModelAccessor ma = filec.SaveAsAction();
		if (ma==null) return false;
		target = ma;
		
		return true;
	}
	
	public boolean writeMerge() {
		
			if( ! mergedmodel.getEvents().isEmpty() && target.getModelType() == ModelType.CELLML_MODEL){
				SemGenError.showError("Cannot save as CellML because model contains discrete events", 
						"Cannot write to CellML");
				return false;
			}
			mergedmodel.setName(target.getModelName());
			
			target.writetoFile(mergedmodel);
			if (modelaccessorlist.size() != 3) {
				modelaccessorlist.add(null);
			}
			modelaccessorlist.set(2, target);
			this.modelsaved = true;
			return true;
	}
	
	public ModelAccessor getMergedFileAddress() {
		return target;
	}
	
	@Override
	public void exportModel(Integer index){}
	
	private void notifyModelListUpdated() {
		modelselection = -1;
		setChanged();
		notifyObservers(MergeEvent.modellistupdated);
	}
	
	public String getMergedModelName() {
		return mergedmodel.getName();
	}
	
	public String getModelName(int index) {
		return loadedmodels.get(index).getName();
	}
	
	public SemSimModel getMergedModel() {
		return mergedmodel;
	}
	

	//For populating the manual mapping panel, get all Data Structure names and add descriptions if available.
	public ArrayList<String> getExposedDSNamesandDescriptions(int index){
		ArrayList<String> namelist = new ArrayList<String>();
		for (DataStructure ds : exposeddslist.get(index)) {
			String desc = "(" + ds.getName() + ")";
			if(ds.getDescription()!=null) desc = ds.getDescription() + " " + desc;
			namelist.add(desc);
		}
		return namelist;
	}
	
	@Override
	public void update(Observable o, Object arg) {
	}
	
}
