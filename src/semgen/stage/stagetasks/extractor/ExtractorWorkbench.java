package semgen.stage.stagetasks.extractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Observable;
import java.util.Set;

import semgen.utilities.Workbench;
import semgen.utilities.file.SaveSemSimModel;
import semgen.utilities.file.SemGenSaveFileChooser;
import semsim.model.collection.SemSimModel;
import semsim.model.computational.datastructures.DataStructure;
import semsim.reading.ModelAccessor;


public class ExtractorWorkbench extends Workbench {
	private SemSimModel sourcemodel;
	private ArrayList<SemSimModel> extractions= new ArrayList<SemSimModel>();
	private BatchSave saver;
	private ArrayList<ModelAccessor> modelaccessorlist = new ArrayList<ModelAccessor>();

	public ExtractorWorkbench(ModelAccessor accessor, SemSimModel model) {
		modelaccessorlist.add(accessor);
		sourcemodel = model;
	}
	
	@Override
	public void initialize() {}
	
	public Extractor makeNewExtraction(String name) {
		SemSimModel extraction = new SemSimModel();
		extraction.setName(name);
		extractions.add(extraction);
		modelaccessorlist.add(null);
		return new ExtractNew(sourcemodel, extraction);
		
	}

	public Extractor makeNewExtractionExclude(String name) {
		SemSimModel extraction = new SemSimModel();
		extraction.setName(name);
		extractions.add(extraction);
		modelaccessorlist.add(null);
		return new ExtractRemove(sourcemodel, extraction);
		
	}
	
	public void saveExtractions(ArrayList<Integer> indicies) {
		saver = new BatchSave(indicies);
		boolean hasnext = false;
		while (!hasnext) {
			saveModel();
			hasnext = saver.next();
		}
		saver = null;
	}
	
	@Override
	public void setModelSaved(boolean val) {}

	@Override
	public String getCurrentModelName() {
		return sourcemodel.getName();
	}

	@Override
	public ModelAccessor getModelSourceLocation() {
		return sourcemodel.getLegacyCodeLocation();
	}
	
	@Override
	public ModelAccessor saveModel() {
		ModelAccessor ma = saver.getModelAccessor();
		if (ma == null) ma = saveModelAs();
		else {
			SaveSemSimModel.writeToFile(saver.getModelOnDeck(), ma, ma.getFileThatContainsModel(), saver.getModelOnDeck().getSourceModelType());
		}
		return ma;
	}

	@Override
	public ModelAccessor saveModelAs() {
		SemSimModel model = saver.getModelOnDeck();
		SemGenSaveFileChooser filec = new SemGenSaveFileChooser(new String[]{"owl", "proj", "cellml", "sbml"}, "owl");
		ModelAccessor ma = filec.SaveAsAction(model);
		
		if (ma != null) {
			model.setName(ma.getModelName());
			
			SaveSemSimModel.writeToFile(model, ma, ma.getFileThatContainsModel(), filec.getFileFilter());

			saver.setModelAccessor(ma);
		}

		return ma;
	}

	public SemSimModel getSourceModel() {
		return sourcemodel;
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

	@Override
	public void update(Observable arg0, Object arg1) {
		
	}
	
	public SemSimModel getExtractedModelbyIndex(Integer index) {
		return this.extractions.get(index);
	}
	
	public ModelAccessor getAccessorbyIndex(Integer index) {
		return this.modelaccessorlist.get(index);
	}
	
	
	private class BatchSave {
		ArrayList<SemSimModel> tobesaved = new ArrayList<SemSimModel>();
		int ondeck = 0;
		
		public BatchSave(ArrayList<Integer> indicies) {
			for (Integer index : indicies) {
				tobesaved.add(extractions.get(index));
			}
		}
		
		public SemSimModel getModelOnDeck() {
			return tobesaved.get(ondeck);
		}
		
		public ModelAccessor getModelAccessor() {
			return modelaccessorlist.get(ondeck+1);
		}
		
		public void setModelAccessor(ModelAccessor newaccessor) {
			modelaccessorlist.set(ondeck, newaccessor);
		}
		
		public boolean next() {
			ondeck++;
			return ondeck == tobesaved.size();
		}
	}
}
