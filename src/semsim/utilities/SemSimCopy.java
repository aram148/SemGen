package semsim.utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import semsim.annotation.Annotation;
import semsim.model.collection.FunctionalSubmodel;
import semsim.model.collection.SemSimModel;
import semsim.model.collection.Submodel;
import semsim.model.computational.Computation;
import semsim.model.computational.datastructures.DataStructure;
import semsim.model.computational.datastructures.Decimal;
import semsim.model.computational.datastructures.MMLchoice;
import semsim.model.computational.datastructures.MappableVariable;
import semsim.model.computational.datastructures.SemSimInteger;
import semsim.model.computational.units.UnitFactor;
import semsim.model.computational.units.UnitOfMeasurement;
import semsim.model.physical.PhysicalEntity;
import semsim.model.physical.PhysicalProcess;
import semsim.model.physical.object.CompositePhysicalEntity;
import semsim.model.physical.object.CustomPhysicalEntity;
import semsim.model.physical.object.CustomPhysicalProcess;
import semsim.model.physical.object.ReferencePhysicalEntity;
import semsim.model.physical.object.ReferencePhysicalProcess;
/**
 * Methods for performing a deep copy the structures in a SemSim model. This does NOT
 * create an exact copy, use the 'clone' method in SemSimModel instead.
 * @author Christopher
 *
 */
public class SemSimCopy {
	HashMap<PhysicalEntity, PhysicalEntity> entities = new HashMap<PhysicalEntity, PhysicalEntity>();
	HashMap<PhysicalProcess, PhysicalProcess> procs = new HashMap<PhysicalProcess, PhysicalProcess>();
	HashMap<UnitOfMeasurement, UnitOfMeasurement> unitmap = new HashMap<UnitOfMeasurement, UnitOfMeasurement>();
	HashMap<DataStructure, DataStructure> dsmap = new HashMap<DataStructure, DataStructure>();
	HashMap<Computation, Computation> compmap = new HashMap<Computation, Computation>();
	HashMap<Submodel, Submodel> smmap = new HashMap<Submodel, Submodel>();
	
	SemSimModel modeltocopy;
	SemSimModel destmodel;
	
	public SemSimCopy(SemSimModel source, SemSimModel target) {
		modeltocopy = source;
		destmodel = target;
		copySemSimModelStructures();
	}
	
	/**
	 * Method for copying the physical and computational components from one model to another. 
	 * @param modeltocopy
	 * @param destmodel
	 */
	private void copySemSimModelStructures() {
		copyPhysicalEntities(modeltocopy);
		destmodel.setPhysicalEntities(new HashSet<PhysicalEntity>(entities.values()));
		
		copyPhysicalProcesses(modeltocopy);
		destmodel.setPhysicalProcesses(new HashSet<PhysicalProcess>(procs.values()));
		
		copyUnits(modeltocopy.getUnits());
		destmodel.setUnits(new HashSet<UnitOfMeasurement>(unitmap.values()));
		
		copyDataStructures(modeltocopy);
		destmodel.setAssociatedDataStructures(new HashSet<DataStructure>(dsmap.values()));
		
		copySubModels();
		destmodel.setSubmodels(new HashSet<Submodel>(smmap.values()));
		remapSubmodels();
		
		copyComputations();
	}

	public static Set<Annotation> copyAnnotations(Set<Annotation> annstocopy) {
		Set<Annotation> annset = new HashSet<Annotation>();
		for (Annotation ann : annstocopy) {
			annset.add(new Annotation(ann));
		}
		
		return annset;
	}
	
	private HashMap<PhysicalEntity, PhysicalEntity> copyPhysicalEntities(SemSimModel modeltocopy) {
		for (ReferencePhysicalEntity rpe : modeltocopy.getReferencePhysicalEntities()) {
			entities.put(rpe,rpe);
		}
		for (CustomPhysicalEntity cupe : modeltocopy.getCustomPhysicalEntities()) {
			entities.put(cupe, new CustomPhysicalEntity(cupe));
		}
		for (CompositePhysicalEntity cpe : modeltocopy.getCompositePhysicalEntities()) {
			CompositePhysicalEntity newcpe = new CompositePhysicalEntity(cpe);
			ArrayList<PhysicalEntity> pes = new ArrayList<PhysicalEntity>();
			for (PhysicalEntity pe : cpe.getArrayListOfEntities()) {
				if (!entities.containsKey(pe)) {
					if (pe.hasRefersToAnnotation()) {
						entities.put(pe, pe);
					}
					else {
						entities.put(pe, new CustomPhysicalEntity((CustomPhysicalEntity) pe));
					}
				}
				pes.add(entities.get(pe));
			}
			newcpe.setArrayListOfEntities(pes);
			entities.put(cpe, newcpe);
		}

		return entities;
	}
	
	private HashMap<PhysicalProcess, PhysicalProcess> copyPhysicalProcesses(SemSimModel modeltocopy) {		
		for (ReferencePhysicalProcess rpp : modeltocopy.getReferencePhysicalProcesses()) {
			procs.put(rpp,rpp);
		}
		for (CustomPhysicalProcess cpp : modeltocopy.getCustomPhysicalProcesses()) {
			CustomPhysicalProcess newcpp = new CustomPhysicalProcess(cpp);
			for (PhysicalEntity part : cpp.getParticipants()) {
				newcpp.replaceParticipant(part, entities.get(part));
			}
			procs.put(cpp, newcpp);
		}
		
		return procs;
	}
	
	private HashMap<UnitOfMeasurement, UnitOfMeasurement> copyUnits(Set<UnitOfMeasurement> units) {
		
		HashMap<UnitFactor, UnitFactor> ufactormap = new HashMap<UnitFactor, UnitFactor>();
		for (UnitOfMeasurement old : units) {
			UnitOfMeasurement newunit = new UnitOfMeasurement(old);
			unitmap.put(old, newunit);
			
			UnitFactor newuf;
			for (UnitFactor uf : old.getUnitFactors()) {
				if (ufactormap.containsKey(uf)) {
					newuf = ufactormap.get(uf);
				}
				else {
					newuf = new UnitFactor(uf);
					ufactormap.put(uf, newuf);
				}
				newuf.setBaseUnit(newunit);
				newunit.addUnitFactor(newuf);
			}
			newunit.setAnnotations(copyAnnotations(old.getAnnotations()));
		}

		return unitmap;
	}
	
	private void copyDataStructures(SemSimModel modeltocopy) {
		for (DataStructure ds : modeltocopy.getIntegers()) {
			dsmap.put(ds, new SemSimInteger((SemSimInteger)ds));
		}
		for (DataStructure ds : modeltocopy.getMMLchoiceVars()) {
			dsmap.put(ds, new MMLchoice((MMLchoice)ds));
		}
		HashMap<MappableVariable, MappableVariable> mvset = new HashMap<MappableVariable, MappableVariable>();
		for (DataStructure ds : modeltocopy.getDecimals()) {
			if (ds instanceof MappableVariable) {
				MappableVariable newmv = new MappableVariable((MappableVariable)ds);
				mvset.put((MappableVariable)ds, newmv);
				dsmap.put(ds, newmv);
			}
			else {
				dsmap.put(ds, new Decimal((Decimal)ds));
			}
		}
		for (MappableVariable old : mvset.keySet()) {
			Set<MappableVariable> fromset = new HashSet<MappableVariable>();
			for (MappableVariable mv : old.getMappedFrom()) {
				fromset.add(mvset.get(mv));
			}
			Set<MappableVariable> toset = new HashSet<MappableVariable>();
			for (MappableVariable mv : old.getMappedTo()) {
				toset.add(mvset.get(mv));
			}
			MappableVariable newmv = mvset.get(old);
			newmv.setMappedFrom(fromset);
			newmv.setMappedTo(toset);
		}
		
		remapDataStructures();
	}
	
	private void remapDataStructures() {
		for (DataStructure ds : dsmap.values()) {
			Computation comp = ds.getComputation();
			if (comp!=null) {
				Computation newcomp = new Computation(comp);
				ds.setComputation(newcomp);
				compmap.put(comp, newcomp);
			}
			if (ds.hasAssociatedPhysicalComponent()) {
				if (entities.containsKey(ds.getAssociatedPhysicalModelComponent())) {
					ds.setAssociatedPhysicalModelComponent(entities.get(ds.getAssociatedPhysicalModelComponent()));
				}
				else {
					ds.setAssociatedPhysicalModelComponent(procs.get(ds.getAssociatedPhysicalModelComponent()));
				}
			}
			if (ds.hasSolutionDomain()) {
				ds.setSolutionDomain(dsmap.get(ds.getSolutionDomain()));
			}
			if (ds.hasUnits()) {
				ds.setUnit(unitmap.get(ds.getUnit()));
			}
			HashSet<DataStructure> used = new HashSet<DataStructure>();
			for (DataStructure utc : ds.getUsedToCompute()) {
				used.add(dsmap.get(utc));
			}
			ds.setUsedToCompute(used);
		}
	}
	
	private void copyComputations() {
		for (Computation comp : compmap.values()) {
			HashSet<DataStructure> inputs = new HashSet<DataStructure>();
			for (DataStructure in : comp.getInputs()) {
				inputs.add(dsmap.get(in));
			}
			HashSet<DataStructure> outputs = new HashSet<DataStructure>();
			for (DataStructure out : comp.getInputs()) {
				outputs.add(dsmap.get(out));
			}
			comp.setInputs(inputs);
			comp.setOutputs(outputs);
		}
	}
	
	private void copySubModels() {
		for (Submodel sm : modeltocopy.getSubmodels()) {
			Submodel newsm;
			if (sm.isFunctional()) {
				FunctionalSubmodel fsm = new FunctionalSubmodel((FunctionalSubmodel)sm);
				if (!compmap.containsKey(fsm.getComputation())) {
					Computation newcomp = new Computation(fsm.getComputation());
					compmap.put(fsm.getComputation(), newcomp);
					fsm.setComputation(newcomp);
				}
				else {
					fsm.setComputation(compmap.get(fsm.getComputation()));
				}
				newsm = fsm;
			}
			else {
				newsm = new Submodel(sm);
			}
			smmap.put(sm, newsm);
		}
	}
	
	private void remapSubmodels() {
		for (Submodel newsm : smmap.values()) {
			HashSet<DataStructure> dsset = new HashSet<DataStructure>();
			for (DataStructure ds : newsm.getAssociatedDataStructures()) {
				dsset.add(dsmap.get(ds));
			}
			HashSet<Submodel> smset = new HashSet<Submodel>();
			for (Submodel assocsm : newsm.getSubmodels()) {
				smset.add(smmap.get(assocsm));
			}
			newsm.setAssociatedDataStructures(dsset);
			newsm.setSubmodels(smset);
		}
	}
}
