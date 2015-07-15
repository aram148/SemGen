package semgen.merging.workbench;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.jdom.JDOMException;
import org.semanticweb.owlapi.model.OWLException;

import semsim.model.collection.FunctionalSubmodel;
import semsim.model.collection.SemSimModel;
import semsim.model.collection.Submodel;
import semsim.model.computational.datastructures.DataStructure;
import semsim.model.computational.datastructures.MappableVariable;
import semsim.model.computational.units.UnitFactor;
import semsim.model.computational.units.UnitOfMeasurement;
import semsim.model.physical.object.CustomPhysicalProcess;
import semsim.model.physical.object.ReferencePhysicalProcess;
import semsim.utilities.SemSimUtil;
import JSim.util.Xcept;

public class Merger {
	private SemSimModel ssm1clone, ssm2clone;
	private ModelOverlapMap overlapmap;
	protected String error;
	private ArrayList<ResolutionChoice> choicelist;
	ArrayList<Pair<Double,String>> conversionfactors;
	
	public static enum ResolutionChoice {
		noselection, first, second, ignore; 
	}
	
	public Merger(SemSimModel model1, SemSimModel model2, ModelOverlapMap modelmap, 
			ArrayList<ResolutionChoice> choices, ArrayList<Pair<Double,String>> conversions) {
		ssm1clone = model1;
		ssm2clone = model2;
		overlapmap = modelmap;
		choicelist = choices;
		conversionfactors = conversions;
	}
	
	public SemSimModel merge() throws IOException, CloneNotSupportedException, OWLException, InterruptedException, JDOMException, Xcept {
		// First collect all the data structures that aren't going to be used in the resulting merged model
		
		SemSimModel modelfordiscardedds = null;
		DataStructure discardedds = null;
		DataStructure keptds = null;
		
		// If there is one solution domain, get it, otherwise set soldom1 to null
		DataStructure soldom1 = (ssm1clone.getSolutionDomains().size()==1) ? ssm1clone.getSolutionDomains().toArray(new DataStructure[]{})[0] : null;
		
		Set<DataStructure> discardeddsset = new HashSet<DataStructure>();
		Set<DataStructure> keptdsset = new HashSet<DataStructure>();
		Set<DataStructure> dsdonotprune = new HashSet<DataStructure>();
		boolean prune = true;
		
		int i = 0;
		
		// Step through resolution points and replace/rewire codewords as needed
		for (Pair<DataStructure, DataStructure> dsp : overlapmap.getDataStructurePairs()) {
			if (choicelist.get(i).equals(ResolutionChoice.first)) {
				discardedds = dsp.getRight();
				keptds = dsp.getLeft();
				modelfordiscardedds = ssm2clone;
			}
			else if(choicelist.get(i).equals(ResolutionChoice.second)){
				discardedds = dsp.getLeft();
				keptds = dsp.getRight();
				modelfordiscardedds = ssm1clone;
			}
			
			// If "ignore equivalency" is NOT selected
			if(keptds!=null && discardedds !=null){	
				
				discardeddsset.add(discardedds);
				keptdsset.add(keptds);
				dsdonotprune.add(keptds);
				
				if(keptds.isMappable() && discardedds.isMappable()){
					rewireMappedVariableDependencies((MappableVariable)keptds, (MappableVariable)discardedds, modelfordiscardedds, i);
					dsdonotprune.add(discardedds); // MappableVariables that are turned into "receivers" should not be pruned
				}
				else if (! replaceCodeWords(keptds, discardedds, modelfordiscardedds, soldom1, i)) 
					return null;
			}
			i++;
		}
		
		// Determine which data structures should be pruned	
		if(prune){
			Set<DataStructure> runningsettoprune = new HashSet<DataStructure>();
			runningsettoprune.addAll(discardeddsset);
			
			runningsettoprune = getDataStructuresToPrune(discardeddsset, runningsettoprune, dsdonotprune);
			runningsettoprune.removeAll(dsdonotprune);
			
			for(DataStructure dstoprune : runningsettoprune){
				SemSimModel parentmodel = ssm1clone.getAssociatedDataStructures().contains(dstoprune) ? ssm1clone : ssm2clone;
				parentmodel.removeDataStructure(dstoprune.getName());  // Pruning
				
				// Remove equation in MathML block, if inputds is a MappableVariable
				if(dstoprune.isMappable()){
					FunctionalSubmodel fs = parentmodel.getParentFunctionalSubmodelForMappableVariable((MappableVariable)dstoprune);
					fs.removeVariableEquationFromMathML((MappableVariable)dstoprune);
				}
			}
		}
			
		// Remove the computational dependency information for the discarded/receiverized codewords
		for(DataStructure onediscardedds : discardeddsset){
			onediscardedds.getComputationInputs().clear();
		}
	
		// What if both models have a custom phys component with the same name?
		SemSimModel mergedmodel = ssm1clone;
		
		//Add processes to the merged model
		for (CustomPhysicalProcess pp : ssm2clone.getCustomPhysicalProcesses()) {
			mergedmodel.addCustomPhysicalProcess(pp);
		}
		
		for (ReferencePhysicalProcess pp : ssm2clone.getReferencePhysicalProcesses()) {
			mergedmodel.addReferencePhysicalProcess(pp);
		}
		
		Map<UnitOfMeasurement,UnitOfMeasurement> equnitsmap = overlapmap.getEquivalentUnitPairs();
		
		// Create mirror map where model 2's units are the key set and model 1's are the values
		Map<UnitOfMeasurement,UnitOfMeasurement> mirrorunitsmap = new HashMap<UnitOfMeasurement,UnitOfMeasurement>();

		for(UnitOfMeasurement uom1 : equnitsmap.keySet()){
			mirrorunitsmap.put(equnitsmap.get(uom1), uom1);
		}
		
		// Replace any in-line unit declarations for equivalent units
		// First collect the submodels we need to check
		Set<FunctionalSubmodel> submodelswithconstants = new HashSet<FunctionalSubmodel>();

		for(FunctionalSubmodel fs : ssm2clone.getFunctionalSubmodels()){
			if(fs.getComputation().getMathML()!=null){
				if(fs.getComputation().getMathML().contains("<cn")) 
					submodelswithconstants.add(fs);
			}
		}
		
		for(FunctionalSubmodel fswithcon : submodelswithconstants){
			String oldmathml = fswithcon.getComputation().getMathML();
			String newmathml = oldmathml;
			for(UnitOfMeasurement uom : mirrorunitsmap.keySet()){
				newmathml = newmathml.replace("\"" + uom.getName() + "\"", "\"" + mirrorunitsmap.get(uom).getName() + "\"");
			}
			fswithcon.getComputation().setMathML(newmathml);
		}
		
		// Copy in all data structures
		for(DataStructure dsfrom2 : ssm2clone.getAssociatedDataStructures()){
			mergedmodel.addDataStructure(dsfrom2);
			
			// Deal with unit equivalencies
			UnitOfMeasurement dsfrom2unit = dsfrom2.getUnit();
			if(mirrorunitsmap.containsKey(dsfrom2unit)){
				dsfrom2.setUnit(mirrorunitsmap.get(dsfrom2unit));
			}
		}		
		
		// Copy in the units, deal with equivalencies along the way
		for(UnitOfMeasurement model2unit : ssm2clone.getUnits()){
					
			// If an equivalent unit was not found for the model 2 unit, add it and
			// deal with equivalent units in its unit factor set
			if( ! mirrorunitsmap.containsKey(model2unit)){
				mergedmodel.addUnit(model2unit);
				
				for(UnitFactor uf : model2unit.getUnitFactors()){
					
					// If the unit factor uses a unit with an equivalent unit in model1, replace it
					UnitOfMeasurement baseunit = uf.getBaseUnit();
					
					if(mirrorunitsmap.containsKey(baseunit)){
						uf.setBaseUnit(mirrorunitsmap.get(baseunit));
					}
				}

			}
		}
		
		// Copy in the submodels
				for(Submodel subfrom2 : ssm2clone.getSubmodels()){
					mergedmodel.addSubmodel(subfrom2);
				}
		
		// Prune empty submodels
		if(prune) pruneSubmodels(mergedmodel);
		
		// Remove legacy code info
		mergedmodel.setSourceFileLocation("");
		
		// WHAT TO DO ABOUT ONTOLOGY-LEVEL ANNOTATIONS?
		mergedmodel.setNamespace(mergedmodel.generateNamespaceFromDateAndTime());
		
		return mergedmodel;
	}
	
	// Changes to variables when merging two models with CellML-style mapped variables
	private void rewireMappedVariableDependencies(MappableVariable sourceds, MappableVariable receiverds, 
			SemSimModel modelforrecieverds, int index){
				
		//For the codeword that will now receive values from the source codeword,
		// turn it into a component input and create a mapping from the source codeword to the receiver.
		receiverds.setPublicInterfaceValue("in");
		sourceds.addVariableMappingTo(receiverds);
		
		//Take all mappedTo values for receiver codeword and apply them to source codeword.
		for(MappableVariable mappedtods : receiverds.getMappedTo()){
			sourceds.addVariableMappingTo(mappedtods);
		}
		
		// Remove all mappedTo DataStructures for receiver codeword.
		receiverds.getMappedTo().clear();
		
		//Also remove any initial_value that the receiver DS has.
		receiverds.setCellMLinitialValue(null);
		receiverds.setStartValue(null);
		
		// Find mathML block for the receiver codeword and remove it from the FunctionalSubmodel's computational code
		FunctionalSubmodel fs = modelforrecieverds.getParentFunctionalSubmodelForMappableVariable(receiverds);
		fs.removeVariableEquationFromMathML(receiverds);
	}
	
	
	// Collect those data structures that are "orphaned" by the merge and flag them for pruning
	private Set<DataStructure> getDataStructuresToPrune(Set<DataStructure> settocheck, Set<DataStructure> runningset, Set<DataStructure> flagimmune){
		
		Set<DataStructure> allinputs = new HashSet<DataStructure>();
		Set<DataStructure> nextsettoprocess = new HashSet<DataStructure>();
		
		// Get all inputs to codewords that are edited (replaced, turned into receivers, or orphaned)
		for(DataStructure dstocheck : settocheck){
			allinputs.addAll(dstocheck.getComputationInputs());
		}
		
		for(DataStructure inputds : allinputs){
			
			// If the input is only used to compute codewords that are pruned, prune it
			if(runningset.containsAll(inputds.getUsedToCompute())){
				
//				 Don't prune data structures that must be included in the model
				if(! flagimmune.contains(inputds)){
					runningset.add(inputds);
					nextsettoprocess.add(inputds);
				}
			}
		}
		
		// Process recursively
		if(nextsettoprocess.size()>0)
			getDataStructuresToPrune(nextsettoprocess, runningset, flagimmune);
		
		return runningset;
	}
	
	
	private boolean replaceCodeWords(DataStructure keptds, DataStructure discardedds, 
			SemSimModel modelfordiscardedds, DataStructure soldom1, int index) {
		
		// If we need to add in a unit conversion factor
		String replacementtext = keptds.getName();
		
		Pair<Double, String> conversionfactor = conversionfactors.get(index);
		
		// if the two terms have different names, or a conversion factor is required
		if(!discardedds.getName().equals(keptds.getName()) || conversionfactor.getLeft()!=1){
			replacementtext = "(" + keptds.getName() + conversionfactor.getRight() + String.valueOf(conversionfactor.getLeft()) + ")";
			SemSimUtil.replaceCodewordInAllEquations(discardedds, keptds, modelfordiscardedds, 
					discardedds.getName(), replacementtext, conversionfactor.getLeft());
		}
		// What to do about sol doms that have different units?
		
		if(discardedds.isSolutionDomain()){
		  
			// Re-set the solution domain designations for all DataStructures in model 2
			for(DataStructure nsdds : ssm2clone.getAssociatedDataStructures()){
				if(nsdds.hasSolutionDomain())
					nsdds.setSolutionDomain(soldom1);
			}
		}
		
		// If we are removing a state variable, remove its derivative, if present
		if(discardedds.hasSolutionDomain()){
			
			if(modelfordiscardedds.containsDataStructure(discardedds.getName() + ":" + discardedds.getSolutionDomain().getName())){
				modelfordiscardedds.removeDataStructure(discardedds.getName() + ":" + discardedds.getSolutionDomain().getName());
			}
		}
		
		// If the semantic resolution took care of a syntactic resolution
		if(!choicelist.get(index).equals(ResolutionChoice.ignore)){
			overlapmap.getIdenticalNames().remove(discardedds.getName());
		}
		return true;
	}
	
	// Remove empty submodels
	private void pruneSubmodels(SemSimModel model){
		Set<Submodel> tempset = new HashSet<Submodel>();
		tempset.addAll(model.getSubmodels());
		
		for(Submodel sub : tempset){
			
			if(sub.getAssociatedDataStructures().isEmpty()){
				model.removeSubmodel(sub);
			}
		}
	}
}
