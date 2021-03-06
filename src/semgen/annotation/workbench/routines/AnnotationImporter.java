package semgen.annotation.workbench.routines;

import java.io.File;
import java.io.IOException;

import org.jdom.JDOMException;

import semgen.SemGen;
import semgen.utilities.SemGenJob;
import semgen.utilities.file.LoadModelJob;
import semsim.annotation.AnnotationCopier;
import semsim.annotation.SemSimTermLibrary;
import semsim.fileaccessors.FileAccessorFactory;
import semsim.model.collection.SemSimModel;
import semsim.model.collection.Submodel;
import semsim.model.computational.datastructures.DataStructure;

public class AnnotationImporter extends SemGenJob {
	SemSimModel importedmodel;
	SemSimModel importingmodel;
	SemSimTermLibrary library;
	
	private Boolean[] options;
	private File sourcefile;
	
	public AnnotationImporter(SemSimTermLibrary lib, SemSimModel currentmodel, File file, Boolean[] opts) {
		sourcefile = file;
		library = lib;
		importingmodel = currentmodel;
		options = opts;
	}
	
	@Override
	public void run() {
		try {
			loadSourceModel();
		} catch (JDOMException | IOException e) {
			e.printStackTrace();
		}
		copyModelAnnotations();
	}
	
	private void loadSourceModel() throws JDOMException, IOException {
		LoadModelJob loader = new LoadModelJob(FileAccessorFactory.getModelAccessor(sourcefile), this);
		loader.run();
		if (!loader.isValid()) {
			abort();
			return;
		}
		importedmodel = loader.getLoadedModel();
	}
	
	private boolean copyModelAnnotations() {
		library.addTermsinModel(importedmodel);
		boolean changed = false;
		if (options[0]) {
			//importingmodel.importCurationalMetadatafromModel(importedmodel, true);
			changed = true;
		}
		if (options[1]) {
			if (importCodewords()) changed = true;
		}
		
		if (options[2]) {
			if (copySubmodels()) changed = true;
		}
		return changed;
	}
	
	// Copy over all the submodel data
	// Make sure to include change flag functionality
	private boolean copySubmodels() {
		Boolean changemadetosubmodels = false;
		for(Submodel sub : importingmodel.getSubmodels()){
			if(importedmodel.getSubmodel(sub.getName()) !=null){
				changemadetosubmodels = true;
				
				Submodel srcsub = importedmodel.getSubmodel(sub.getName());
				
				// Copy free-text description
				sub.setDescription(srcsub.getDescription());
			}
		}
		return changemadetosubmodels;
	}
	
	private boolean importCodewords() {
		
		boolean changes = false;
			
		for(DataStructure ds : importingmodel.getAssociatedDataStructures()){
				
			if(importedmodel.containsDataStructure(ds.getName())){
				try{
					changes = true;
				
					DataStructure srcds = importedmodel.getAssociatedDataStructure(ds.getName());				
					ds.copyDescription(srcds);
					ds.copySingularAnnotations(srcds, SemGen.semsimlib);
					AnnotationCopier.copyCompositeAnnotation(library, ds, srcds);
				}
				catch(Exception ex){
					ex.printStackTrace();
				}
				
			} // otherwise no matching data structure found in source model
		} // end of data structure loop
		return changes;
	}


}
