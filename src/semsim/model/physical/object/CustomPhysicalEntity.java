package semsim.model.physical.object;

import java.net.URI;

import semsim.SemSimConstants;
import semsim.model.SemSimTypes;
import semsim.model.physical.PhysicalEntity;


public class CustomPhysicalEntity extends PhysicalEntity{
	
	public CustomPhysicalEntity(String name, String description){
		setName(name);
		setDescription(description);
	}
	
	/** 
	 * Copy constructur
	 * @param cupe
	 */
	public CustomPhysicalEntity(CustomPhysicalEntity cupe) {
		super(cupe);
	}
	
	@Override
	protected boolean isEquivalent(Object obj) {
		return ((CustomPhysicalEntity)obj).getName().equals(getName());
	}
	
	@Override
	public URI getSemSimClassURI() {
		return SemSimConstants.CUSTOM_PHYSICAL_ENTITY_CLASS_URI;
	}
	@Override
	public SemSimTypes getSemSimType() {
		return SemSimTypes.CUSTOM_PHYSICAL_ENTITY;
	}
}
