package semgen.merging.workbench;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import semsim.annotation.ReferenceOntologyAnnotation;
import semsim.model.SemSimModel;
import semsim.model.computational.datastructures.DataStructure;
import semsim.model.computational.datastructures.MappableVariable;
import semsim.model.physical.PhysicalEntity;
import semsim.model.physical.PhysicalModelComponent;
import semsim.model.physical.PhysicalProcess;
import semsim.model.physical.object.CompositePhysicalEntity;
import semsim.model.physical.object.PhysicalProperty;

public class SemanticComparator {
	private SemSimModel model1, model2;
	private DataStructure slndomain;

	public SemanticComparator(SemSimModel m1, SemSimModel m2) {
		model1 = m1; model2 = m2;
		
		if(model1.getSolutionDomains().size() > 0)
			slndomain = model1.getSolutionDomains().toArray(new DataStructure[]{})[0];
	}
	
	public Set<String> identifyIdenticalCodewords() {
		Set<String> matchedcdwds = new HashSet<String>();
		
		for (DataStructure ds : model1.getDataStructures()) {
			if (model2.containsDataStructure(ds.getName()))	matchedcdwds.add(ds.getName());
		}
		if (slndomain != null) {
			String slndomainname = slndomain.getName(); 
			matchedcdwds.remove(slndomainname);
			matchedcdwds.remove(slndomainname + ".min");
			matchedcdwds.remove(slndomainname + ".max");
			matchedcdwds.remove(slndomainname + ".delta");
		}
		return matchedcdwds;
	}
	
	public ArrayList<Pair<DataStructure, DataStructure>> identifyExactSemanticOverlap() {
		ArrayList<Pair<DataStructure, DataStructure>> dsmatchlist = new ArrayList<Pair<DataStructure, DataStructure>>();
		
		DataStructure soldom2 = null;
		if(model2.getSolutionDomains().size() > 0)
			soldom2 = model2.getSolutionDomains().toArray(new DataStructure[]{})[0];
		
		dsmatchlist.add(Pair.of(slndomain, soldom2));
		
		Set<DataStructure> model1ds = getComparableDataStructures(model1);
		Set<DataStructure> model2ds = getComparableDataStructures(model2);
		
		// For each comparable data structure in model 1...
		for(DataStructure ds1 : model1ds){
			
			// Exclude solution domains
			if (ds1 != slndomain) {
				
				// For each comparable data structure in model 2
				for(DataStructure ds2 : model2ds){
					Boolean match = false;
					
					// Test singular annotations
					if(ds1.hasRefersToAnnotation() && ds2.hasRefersToAnnotation()) {
						match = testSingularAnnotations(ds1.getFirstRefersToReferenceOntologyAnnotation(),
								ds2.getFirstRefersToReferenceOntologyAnnotation());
					}
					
					// If the physical properties are not null...
					if(!match && ds1.getPhysicalProperty()!=null && ds2.getPhysicalProperty()!=null){
						
						// And they are properties of a specified physical model component
						if(ds1.getPhysicalProperty().getPhysicalPropertyOf()!=null && ds2.getPhysicalProperty().getPhysicalPropertyOf()!=null){
							PhysicalProperty prop1 = ds1.getPhysicalProperty();
							PhysicalProperty prop2 = ds2.getPhysicalProperty();
							
							// Test equivalency of physical properties
							match = testEquivalencyOfPhysicalComponents(prop1, prop2);
							
							// If the property annotations are the same, test the equivalency of what they are properties of
							if(match){
								System.out.println("Physical properties for " + ds1.getName() + " and " + ds2.getName() + " equivalent");
								match = testEquivalencyOfPhysicalComponents(prop1.getPhysicalPropertyOf(), prop2.getPhysicalPropertyOf());
							}
						}
					}
					if(match){
						dsmatchlist.add(Pair.of(ds1, ds2));
					}
				} // end of iteration through model2 data structures
			}
		} // end of iteration through model1 data structures
		return dsmatchlist;
	}
	
	// Find all the data structures that should be compared. This weeds out 
	// MappableVariables that have an "in" interface. For CellML-type models, the Merger should not 
	// propose mappings between variables with an "in" interface.
	public Set<DataStructure> getComparableDataStructures(SemSimModel model){
		Set<DataStructure> dsset = new HashSet<DataStructure>();
		for(DataStructure ds : model.getDataStructures()){
			if(ds instanceof MappableVariable){
				MappableVariable mappedds = (MappableVariable)ds;
				
				// If the mapped variable has an "in" interface value, don't add to the set of comparable DataStructures
				if (mappedds.getPublicInterfaceValue().equals("in") || mappedds.getPrivateInterfaceValue().equals("in"))
					continue;
			}
			dsset.add(ds);
		}
		return dsset;
	}
	
	
	private Boolean testSingularAnnotations(ReferenceOntologyAnnotation ann1, ReferenceOntologyAnnotation ann2){
		return (ann1.getReferenceURI().toString().equals(ann2.getReferenceURI().toString()));
	}
	
	
	private boolean testEquivalencyOfPhysicalComponents(PhysicalModelComponent pmc1, PhysicalModelComponent pmc2){
		if(pmc1.getClass() != pmc2.getClass()) return false;
		
		// This handles physical properties, referenced singular physical entities, and referenced physical processes
		if(pmc1.hasRefersToAnnotation() && pmc2.hasRefersToAnnotation()){
			if(pmc1.getFirstRefersToReferenceOntologyAnnotation().getReferenceURI().toString().equals(pmc2.getFirstRefersToReferenceOntologyAnnotation().getReferenceURI().toString())){
				System.out.println("Singular terms equivalent for " + pmc1.getName() + " and " + pmc2.getName());
				return true;
			}
			return false;
		}
		
		// This handles composite physical entities
		else if(pmc1 instanceof CompositePhysicalEntity){
			CompositePhysicalEntity cpe1 = (CompositePhysicalEntity)pmc1;
			CompositePhysicalEntity cpe2 = (CompositePhysicalEntity)pmc2;
			return testEquivalencyOfCompositePhysicalEntities(cpe1, cpe2);
		}
		
		// This handles physical processes
		else if(pmc1 instanceof PhysicalProcess){
			System.out.println("****");

			System.out.println("Looking at process " + pmc1.getName());
			System.out.println("Looking at process " + pmc2.getName());
			
			PhysicalProcess process1 = (PhysicalProcess)pmc1;
			PhysicalProcess process2 = (PhysicalProcess)pmc2;
				
			// Test whether the two processes have the same sources, sinks and mediators
			if (! testEquivalencyOfProcessParticipants(process1.getSourcePhysicalEntities(), process2.getSourcePhysicalEntities())){
				System.out.println("sources not equivalent");

				return false;

			}
			if (! testEquivalencyOfProcessParticipants(process1.getSinkPhysicalEntities(), process2.getSinkPhysicalEntities())){
				System.out.println("sinks not equivalent");
				return false;
			}
			if (! testEquivalencyOfProcessParticipants(process1.getMediatorPhysicalEntities(), process2.getMediatorPhysicalEntities())){
				System.out.println("mediators not equivalent");
				return false;
			}
		}
		return true; // if we have made it here, the physical components are equivalent
	}
	
	private boolean testEquivalencyOfCompositePhysicalEntities(CompositePhysicalEntity cpe1, CompositePhysicalEntity cpe2){
		if(cpe1.getArrayListOfEntities().size()!=cpe2.getArrayListOfEntities().size())
			return false;
		
		for(int i=0; i<cpe1.getArrayListOfEntities().size(); i++){
			if(cpe1.getArrayListOfEntities().get(i).hasRefersToAnnotation() && cpe2.getArrayListOfEntities().get(i).hasRefersToAnnotation()){
				if(!cpe1.getArrayListOfEntities().get(i).getFirstRefersToReferenceOntologyAnnotation().getReferenceURI().toString().equals( 
					cpe2.getArrayListOfEntities().get(i).getFirstRefersToReferenceOntologyAnnotation().getReferenceURI().toString())){
					return false;
				}
			}
			else return false;
		}
		return true;
	}
	
	
	private boolean testEquivalencyOfProcessParticipants(Set<PhysicalEntity> ents1, Set<PhysicalEntity> ents2){
		boolean matchfound = false;
		// If the two sets of participants are the same size
		if (ents1.size() == ents2.size()){
			for(PhysicalEntity ent1 : ents1){
				for(PhysicalEntity ent2 : ents2){
					if(testEquivalencyOfPhysicalComponents(ent1, ent2)){
						matchfound = true;
						break;
					}
				}
			}
			// if we've made it here, the participants are equivalent
		}
		return matchfound;
	}
}
